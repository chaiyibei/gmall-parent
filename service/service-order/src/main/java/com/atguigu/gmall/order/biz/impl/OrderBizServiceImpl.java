package com.atguigu.gmall.order.biz.impl;
import java.util.*;

import com.atguigu.gmall.model.activity.CouponInfo;
import java.math.BigDecimal;
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
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.to.mq.OrderMsg;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.vo.order.*;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

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

    @Autowired
    OrderDetailService orderDetailService;

    @Override
    public OrderConfirmDataVo getOrderConfirmData() {
        OrderConfirmDataVo confirmDataVo = new OrderConfirmDataVo();
        //1???????????????????????????????????????
        //??????????????????????????????????????????????????????????????????
        //?????????????????????????????????redis????????????????????????????????????????????????
        List<CartInfo> data = cartFeignClient.getChecked().getData();
        List<CartInfoVo> infoVos = data.stream().map(cartInfo -> {
            CartInfoVo cartInfoVo = new CartInfoVo();
            cartInfoVo.setSkuId(cartInfo.getSkuId());
            cartInfoVo.setImgUrl(cartInfo.getImgUrl());
            cartInfoVo.setSkuName(cartInfo.getSkuName());
            //???????????????????????????
            Result<BigDecimal> price = skuProductFeignClient.getSku1010Price(cartInfo.getSkuId());
            cartInfoVo.setOrderPrice(price.getData());
            cartInfoVo.setSkuNum(cartInfo.getSkuNum());
            //????????????
            String stock = wareFeignClient.hasStock(cartInfo.getSkuId(), cartInfo.getSkuNum());
            cartInfoVo.setHasStock(stock);
            return cartInfoVo;
        }).collect(Collectors.toList());

        confirmDataVo.setDetailArrayList(infoVos);

        //2???????????????????????????
        Integer totalNum = infoVos.stream().map(CartInfoVo::getSkuNum)
                .reduce((o1, o2) -> o1 + o2 ).get();
        confirmDataVo.setTotalNum(totalNum);

        //3???????????????????????????
        BigDecimal totalAmount = infoVos.stream()
                .map(cartInfoVo -> cartInfoVo.getOrderPrice().multiply(new BigDecimal(cartInfoVo.getSkuNum() + "")))
                .reduce((o1, o2) -> o1.add(o2)).get();
        confirmDataVo.setTotalAmount(totalAmount);

        //4??????????????????????????????
        Result<List<UserAddress>> addressList = userFeignClient.getUserAddressList();
        confirmDataVo.setUserAddressList(addressList.getData());

        //5????????????????????????
        //5???1?????????????????????????????????????????????????????????????????????
        //5.2??????????????????????????????????????????
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

        //??????redis?????????
        redisTemplate.opsForValue().set(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo,"1",15, TimeUnit.MINUTES);

        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo) {
        //1????????????????????????????????????????????????   ????????????
        String lua = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";

//        String s = redisTemplate.opsForValue().get(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo);
//        if (!StringUtils.isEmpty(s)){
//            //redis???,?????????????????????
//            redisTemplate.delete(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo);
//            return true;
//        }

        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(lua, Long.class),
                Arrays.asList(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo),
                new String[]{"1"});
        if (execute > 0){
            //????????????
            redisTemplate.delete(SysRedisConst.ORDER_TEMP_TOKEN + tradeNo);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public Long submitOrder(OrderSubmitVo submitVo,String tradeNo) {
        //1????????????
        Boolean checkTradeNo = checkTradeNo(tradeNo);
        if (!checkTradeNo){
            throw new GmallException(ResultCodeEnum.TOKEN_INVAILD);
        }

        //2????????????
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

        //3????????????
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

        //4????????????????????????????????????
        Long orderId = orderInfoService.saveOrder(submitVo,tradeNo);

        //5????????????????????????????????????
        cartFeignClient.deleteChecked();

        //45min??????????????????????????????
        //???MQ???????????????????????????????????????????????????

        return orderId;
    }

    @Override
    public void closeOrder(Long orderId, Long userId) {
        //?????????????????????????????????????????????
        ProcessStatus closed = ProcessStatus.CLOSED;
        List<ProcessStatus> expected = Arrays.asList(ProcessStatus.UNPAID,ProcessStatus.FINISHED);
        orderInfoService.changeOrderStatus(orderId,userId,closed,expected);

    }

    @Override
    public List<WareChildOrderVo> orderSplit(OrderWareMapVo params) {
        //1????????????id
        Long orderId = params.getOrderId();
        //1.1??????????????????
        OrderInfo parentOrder = orderInfoService.getById(orderId);
        //1.2????????????????????????
        List<OrderDetail> details = orderDetailService.getOrderDetails(orderId,parentOrder.getUserId());
        parentOrder.setOrderDetailList(details);

        //2??????????????????
        List<WareMapItem> items = Jsons.toObj(params.getWareSkuMap(),
                new TypeReference<List<WareMapItem>>() {});

        //3???????????????
        List<OrderInfo> splitOrders = items.stream().map(wareMapItem -> {
            //4??????????????????
            OrderInfo orderInfo = saveChildOrderInfo(wareMapItem, parentOrder);
            return orderInfo;
        }).collect(Collectors.toList());

        //???????????????????????? ?????????
        orderInfoService.changeOrderStatus(parentOrder.getId(),
                parentOrder.getUserId(),
                ProcessStatus.SPLIT,
                Arrays.asList(ProcessStatus.PAID));

        //4???????????????????????????????????????
        return convertSpiltOrdersToWareChildOrderVo(splitOrders);
    }

    private List<WareChildOrderVo> convertSpiltOrdersToWareChildOrderVo(List<OrderInfo> splitOrders) {
        List<WareChildOrderVo> orderVos = splitOrders.stream()
                .map(orderInfo -> {
                    WareChildOrderVo orderVo = new WareChildOrderVo();
                    orderVo.setOrderId(orderInfo.getId());
                    orderVo.setConsignee(orderInfo.getConsignee());
                    orderVo.setConsigneeTel(orderInfo.getConsigneeTel());
                    orderVo.setOrderComment(orderInfo.getOrderComment());
                    orderVo.setOrderBody(orderInfo.getTradeBody());
                    orderVo.setDeliveryAddress(orderVo.getDeliveryAddress());
                    orderVo.setPaymentWay(orderInfo.getPaymentWay());
                    orderVo.setWareId(orderVo.getWareId());
                    //???????????????
                    List<WareChildOrderDetailItemVo> itemVos = orderInfo.getOrderDetailList().stream()
                            .map(orderDetail -> {
                                WareChildOrderDetailItemVo itemVo = new WareChildOrderDetailItemVo();
                                itemVo.setSkuId(orderDetail.getSkuId());
                                itemVo.setSkuNum(orderDetail.getSkuNum());
                                itemVo.setSkuName(orderDetail.getSkuName());

                                return itemVo;
                            }).collect(Collectors.toList());
                    orderVo.setDetails(itemVos);

                    return orderVo;
                }).collect(Collectors.toList());
        return orderVos;
    }

    //?????????????????????
    private OrderInfo saveChildOrderInfo(WareMapItem wareMapItem, OrderInfo parentOrder) {
        //1??????????????????????????????
        List<Long> skuIds = wareMapItem.getSkuIds(); //10 49
        //2???????????????????????????????????????
        Long wareId = wareMapItem.getWareId();  //1

        //3????????????
        OrderInfo childOrder = new OrderInfo();
        childOrder.setConsignee(parentOrder.getConsignee());
        childOrder.setConsigneeTel(parentOrder.getConsigneeTel());

        //4?????????????????????????????????
        List<OrderDetail> childOrderDetails = parentOrder.getOrderDetailList()
                .stream()
                .filter(orderDetail -> skuIds.contains(orderDetail.getSkuId()))
                .collect(Collectors.toList());
        //????????????
        BigDecimal totalAmount = childOrderDetails.stream()
                .map(orderDetail ->
                        orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum() + "")))
                .reduce((o1, o2) -> o1.add(o2)).get();
        childOrder.setTotalAmount(totalAmount);
        childOrder.setOrderStatus(parentOrder.getOrderStatus());
        childOrder.setUserId(parentOrder.getUserId());
        childOrder.setPaymentWay(parentOrder.getPaymentWay());
        childOrder.setDeliveryAddress(parentOrder.getDeliveryAddress());
        childOrder.setOrderComment(parentOrder.getOrderComment());
        childOrder.setOutTradeNo(parentOrder.getOutTradeNo());
        childOrder.setTradeBody(childOrderDetails.get(0).getSkuName());
        childOrder.setCreateTime(new Date());
        childOrder.setExpireTime(parentOrder.getExpireTime());
        childOrder.setProcessStatus(parentOrder.getProcessStatus());

        //???????????????????????????????????????????????????
        childOrder.setTrackingNo("");

        childOrder.setParentOrderId(parentOrder.getId());
        childOrder.setImgUrl(childOrderDetails.get(0).getImgUrl());
        //???????????????????????????????????????????????????
        childOrder.setOrderDetailList(childOrderDetails);
        childOrder.setWareId(""+wareId);
        childOrder.setProvinceId(0L);
        childOrder.setActivityReduceAmount(new BigDecimal("0"));
        childOrder.setCouponAmount(new BigDecimal("0"));
        childOrder.setOriginalTotalAmount(new BigDecimal("0"));
        childOrder.setRefundableTime(parentOrder.getRefundableTime());
        childOrder.setFeightFee(parentOrder.getFeightFee());
        childOrder.setOperateTime(new Date());
//        childOrder.setOrderDetailVoList(Lists.newArrayList());
//        childOrder.setCouponInfo(new CouponInfo());
//        childOrder.setId(0L);

        //???????????????
        orderInfoService.save(childOrder);
        //????????????????????????
        childOrder.getOrderDetailList().stream()
                .forEach(orderDetail -> orderDetail.setOrderId(childOrder.getId()));
        List<OrderDetail> detailList = childOrder.getOrderDetailList();
        //???????????????????????????
        orderDetailService.saveBatch(detailList);

        return childOrder;
    }
}










