package com.atguigu.gmall.order.biz.impl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.feign.cart.CartFeignClient;
import com.atguigu.gmall.feign.product.SkuProductFeignClient;
import com.atguigu.gmall.feign.user.UserFeignClient;
import com.atguigu.gmall.feign.ware.WareFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.to.mq.OrderMsg;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.vo.order.CartInfoVo;
import com.atguigu.gmall.model.vo.order.OrderSubmitVo;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.google.common.collect.Lists;

import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import com.atguigu.gmall.order.biz.OrderBizService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderBizServiceImpl implements OrderBizService {
    @Autowired
    CartFeignClient cartFeignClient;

    @Autowired
    UserFeignClient userFeignClient;

    @Autowired
    SkuProductFeignClient skuProductFeignClient;

    @Autowired
    WareFeignClient wareFeignClient;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    OrderInfoService orderInfoService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public OrderConfirmDataVo getOrderConfirmData() {
        OrderConfirmDataVo confirmDataVo = new OrderConfirmDataVo();
        //1、获取购物车表中选中的商品
        //购物车服务返回的，每个商品并不能代表最新价格
        //购物车中的商品只代表在redis中存储的数据，并不能代表最新价格
        List<CartInfo> data = cartFeignClient.getChecked().getData();
        List<CartInfoVo> infoVos = data.stream().map(cartInfo -> {
            CartInfoVo cartInfoVo = new CartInfoVo();
            cartInfoVo.setSkuId(cartInfo.getSkuId());
            cartInfoVo.setImgUrl(cartInfo.getImgUrl());
            cartInfoVo.setSkuName(cartInfo.getSkuName());
            //价格还需要再查一遍
            Result<BigDecimal> price = skuProductFeignClient.getSku1010Price(cartInfo.getSkuId());
            cartInfoVo.setOrderPrice(price.getData());
            cartInfoVo.setSkuNum(cartInfo.getSkuNum());
            //查询库存
            String stock = wareFeignClient.hasStock(cartInfo.getSkuId(), cartInfo.getSkuNum());
            cartInfoVo.setHasStock(stock);
            return cartInfoVo;
        }).collect(Collectors.toList());

        confirmDataVo.setDetailArrayList(infoVos);

        //2、统计商品的总数量
        Integer totalNum = infoVos.stream().map(CartInfoVo::getSkuNum)
                .reduce((o1, o2) -> o1 + o2 ).get();
        confirmDataVo.setTotalNum(totalNum);

        //3、统计商品的总金额
        BigDecimal totalAmount = infoVos.stream()
                .map(cartInfoVo -> cartInfoVo.getOrderPrice().multiply(new BigDecimal(cartInfoVo.getSkuNum() + "")))
                .reduce((o1, o2) -> o1.add(o2)).get();
        confirmDataVo.setTotalAmount(totalAmount);

        //4、获取用户收货地址列
        Result<List<UserAddress>> addressList = userFeignClient.getUserAddressList();
        confirmDataVo.setUserAddressList(addressList.getData());

        //5、生成一个追踪号
        //5。1、订单的唯一追踪号，对外交易号（和第三方交互）
        //5.2、用来防重复提交。做防重令牌
        String tradeNo = generateTradeNo();
//        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        confirmDataVo.setTradeNo(tradeNo);

        return confirmDataVo;
    }

    @Override
    public String generateTradeNo() {
        long millis = System.currentTimeMillis();
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        String tradeNo = millis + "_" + authInfo.getUserId();

        //令牌redis存一份
        redisTemplate.opsForValue().set(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo,"1",15, TimeUnit.MINUTES);

        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo) {
        //1、先看有没有，如果有就是正确令牌   脚本校验
        String lua = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";

//        String s = redisTemplate.opsForValue().get(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo);
//        if (!StringUtils.isEmpty(s)){
//            //redis有,校验成功并删除
//            redisTemplate.delete(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo);
//            return true;
//        }

        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(lua, Long.class),
                Arrays.asList(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo),
                new String[]{"1"});
        if (execute > 0){
            //令牌正确
            redisTemplate.delete(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public Long submitOrder(OrderSubmitVo submitVo,String tradeNo) {
        //1、验令牌
        Boolean checkTradeNo = checkTradeNo(tradeNo);
        if (!checkTradeNo){
            throw new GmallException(ResultCodeEnum.TOKEN_INVAILD);
        }

        //2、验库存
        List<String> noStockSkus = new ArrayList<>();
        for (CartInfoVo infoVo : submitVo.getOrderDetailList()) {
            String stock = wareFeignClient.hasStock(infoVo.getSkuId(), infoVo.getSkuNum());
            if (!"1".equals(stock)){
                noStockSkus.add(infoVo.getSkuName());
            }
        }
        if (noStockSkus.size() > 0){
            GmallException exception = new GmallException(ResultCodeEnum.ORDER_NO_STOCK);
            String skuNames = noStockSkus.stream()
                    .reduce(((s1, s2) -> s1 + " " + s2))
                    .get();
            throw new GmallException(
                    exception.getMessage()+skuNames,exception.getCode());
        }

        //3、验价格
        List<String> skuNames = new ArrayList<>();
        for (CartInfoVo infoVo : submitVo.getOrderDetailList()) {
            Result<BigDecimal> price = skuProductFeignClient.getSku1010Price(infoVo.getSkuId());
            if (!price.getData().equals(infoVo.getOrderPrice())){
                skuNames.add(infoVo.getSkuName());
            }
        }
        if (skuNames.size() > 0){
            GmallException exception = new GmallException(ResultCodeEnum.ORDER_PRICE_CHANGED);
            String skuName = skuNames.stream()
                    .reduce(((s1, s2) -> s1 + " " + s2))
                    .get();
            throw new GmallException(
                    exception.getMessage()+ "</br>" +skuName,exception.getCode());
        }

        //4、把订单信息保存到数据库
        Long orderId = orderInfoService.saveOrder(submitVo,tradeNo);

        //5、清除购物车中选中的商品
        cartFeignClient.deleteChecked();

        //45min未支付就要关闭订单。
        //给MQ发一个消息，说明这个订单创建成功了

        return orderId;
    }

    @Override
    public void closeOrder(Long orderId, Long userId) {
        //未支付或者已结束才可以关闭订单
        ProcessStatus closed = ProcessStatus.CLOSED;
        List<ProcessStatus> expected = Arrays.asList(ProcessStatus.UNPAID,ProcessStatus.FINISHED);
        orderInfoService.changeOrderStatus(orderId,userId,closed,expected);

    }
}










