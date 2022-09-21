package com.atguigu.gmall.seckill.biz.impl;
import java.math.BigDecimal;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.order.OrderFeignClient;
import com.atguigu.gmall.feign.user.UserFeignClient;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.to.mq.SeckillTempOrderMsg;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.vo.seckill.SeckillOrderConfirmVo;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.google.common.collect.Lists;
import com.atguigu.gmall.model.activity.CouponInfo;

import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.seckill.biz.SeckillBizService;
import com.atguigu.gmall.seckill.service.SeckillGoodsCacheOpsService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillBizServiceImpl implements SeckillBizService {
    @Autowired
    SeckillGoodsCacheOpsService cacheOpsService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    UserFeignClient userFeignClient;

    @Autowired
    OrderFeignClient orderFeignClient;

    @Override
    public String generateSeckillCode(Long skuId) {
        //前置校验
        //1、获取当前商品
        SeckillGoods goods = cacheOpsService.getSeckillGoodsDetail(skuId);
        if (goods == null){
            //请求非法，当前商品不是参与秒杀的商品
            throw new GmallException(ResultCodeEnum.SECKILL_ILLEGAL);
        }

        //2、看这个商品是否在秒杀时间内
        Date date = new Date();
        if (!date.after(goods.getStartTime())){
            //还没开始，或者已经结束
            throw new GmallException(ResultCodeEnum.SECKILL_NO_START);
        }
        if (!date.before(goods.getEndTime())){
            throw new GmallException(ResultCodeEnum.SECKILL_END);
        }

        //3、判断是否还有足够库存，每个请求放过去，内存库存减一
        if (goods.getStockCount() <= 0){
            throw new GmallException(ResultCodeEnum.SECKILL_FINISH);
        }

        //内存库存减一
//        goods.setStockCount(goods.getStockCount()-1);

        //4、往下放行，生成一个秒杀码  固定算法：同一个用户 + 同一天 + 同一个商品 = 唯一码
        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
        String code = generateCode(userId, DateUtil.formatDate(new Date()), skuId);

        return code;
    }

    @Override
    public Boolean checkSeckillCode(Long skuId,String code) {
        //系统根据算法再生成一次
        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
        String day = DateUtil.formatDate(new Date());
        String skuIdStr = MD5.encrypt(userId + "_" + day + "_" + skuId);

        if (skuIdStr.equals(code) && redisTemplate.hasKey(SysRedisConst.SECKILL_CODE + code)){
            return true;
        }
        return false;
    }

    @Override
    public ResultCodeEnum seckillOrder(Long skuId, String skuIdStr) {
        SeckillGoods goods = cacheOpsService.getSeckillGoodsDetail(skuId);

        //4、验证秒杀码
        Boolean b = checkSeckillCode(skuId, skuIdStr);
        if (!b){
            return ResultCodeEnum.SECKILL_ILLEGAL;
        }

        //1、校验秒杀码是否合法
        if (goods == null){
            return ResultCodeEnum.SECKILL_ILLEGAL;
        }

        //2、验证时间
        Date date = new Date();
        if (!date.after(goods.getStartTime())){
            //还没开始，或者已经结束
            return ResultCodeEnum.SECKILL_NO_START;
        }
        if (!date.before(goods.getEndTime())){
            return ResultCodeEnum.SECKILL_END;
        }

        //3、验证库存
        if (goods.getStockCount() <= 0){
            return ResultCodeEnum.SECKILL_FINISH;
        }

        //判断这个请求是否已经发送过了
        Long increment = redisTemplate.opsForValue().increment(SysRedisConst.SECKILL_CODE + skuIdStr);
        if (increment > 2){
            return ResultCodeEnum.SUCCESS;
        }

        //5、开始秒杀
        //5.1、先让redis预扣库存
        Long decrement = redisTemplate.opsForValue().decrement(SysRedisConst.CACHE_SECKILL_GOODS_STOCK + skuId);
        if (decrement >= 0){
            goods.setStockCount(goods.getStockCount()-1);
            //5.2、再让数据库真正去下单秒杀，去扣减
            OrderInfo orderInfo = prepareTempSeckillOrder(skuId);
            redisTemplate.opsForValue().set(SysRedisConst.SECKILL_ORDER
                    + skuIdStr, Jsons.toStr(orderInfo),1,TimeUnit.DAYS);
            //真正扣库存，创订单
            String str = Jsons.toStr(
                    new SeckillTempOrderMsg(orderInfo.getUserId(), skuId, skuIdStr));
            rabbitTemplate.convertAndSend(MqConst.EXCHANGE_SECKILL_EVENT,
                    MqConst.RK_SECKILL_ORDERWAIT,str);
            //说明redis扣减成功，发消息
            return ResultCodeEnum.SUCCESS;
        }else {
            //说明没有库存了
            return ResultCodeEnum.SECKILL_FINISH;
        }
    }

    @Override
    public ResultCodeEnum checkSeckillOrderStatus(Long skuId) {
        //订单状态检查
        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
        String day = DateUtil.formatDate(new Date());
        String skuIdStr = MD5.encrypt(userId + "_" + day + "_" + skuId);

        String json = redisTemplate.opsForValue().get(SysRedisConst.SECKILL_ORDER + skuIdStr);
        if (json == null){
            return ResultCodeEnum.SECKILL_RUN;
        }
        if ("x".equals(json)){
            return ResultCodeEnum.SECKILL_FINISH;
        }

        //1、是否已经下过单
        OrderInfo orderInfo = Jsons.toObj(json, OrderInfo.class);
        if (orderInfo.getId() != null && orderInfo.getId() > 0){
            return ResultCodeEnum.SECKILL_ORDER_SUCCESS;
        }

        //2、是否抢单成功
        if (orderInfo.getOperateTime() != null){
            return ResultCodeEnum.SECKILL_SUCCESS;
        }

        //只要是成功状态会继续查询
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    public SeckillOrderConfirmVo getSeckillOrderConfirmVo(Long skuId) {
        SeckillOrderConfirmVo confirmVo = null;

        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
        String day = DateUtil.formatDate(new Date());
        String skuIdStr = MD5.encrypt(userId + "_" + day + "_" + skuId);
        String json = redisTemplate.opsForValue().get(SysRedisConst.SECKILL_ORDER + skuIdStr);

        if (!StringUtils.isEmpty(json) && !"x".equals(json)){
            OrderInfo info = Jsons.toObj(json, OrderInfo.class);
            confirmVo = new SeckillOrderConfirmVo();

            confirmVo.setTempOrder(info);
            confirmVo.setTotalNum(info.getOrderDetailList().size());
            confirmVo.setTotalAmount(info.getTotalAmount());
            //用户的收货地址
            Result<List<UserAddress>> addressList = userFeignClient.getUserAddressList();
            confirmVo.setUserAddressList(addressList.getData());
        }
        return confirmVo;
    }

    @Override
    public Long submitSeckillOrder(OrderInfo orderInfo) {
        OrderInfo dbOrder = prepareAndSaveOrderInfoForDb(orderInfo);
        return dbOrder.getId();
    }

    private OrderInfo prepareAndSaveOrderInfoForDb(OrderInfo orderInfo) {
        OrderInfo redisData = null;
        Long skuId = orderInfo.getOrderDetailList().get(0).getSkuId();
        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
        String code = MD5.encrypt(userId + "_" + DateUtil.formatDate(new Date()) + "_" + skuId);
        //1、从redis拿到临时单数据
        String json = redisTemplate.opsForValue().get(SysRedisConst.SECKILL_ORDER + code);
        if(!StringUtils.isEmpty(json) && !"x".equals(json)){
            //2、获取临时单数据
            redisData = Jsons.toObj(json, OrderInfo.class);
            redisData.setConsignee(orderInfo.getConsignee());
            redisData.setConsigneeTel(orderInfo.getConsigneeTel());
            redisData.setOrderStatus(ProcessStatus.UNPAID.getOrderStatus().name());

            redisData.setDeliveryAddress(orderInfo.getDeliveryAddress());
            redisData.setOrderComment(orderInfo.getOrderComment());

            redisData.setOutTradeNo(System.currentTimeMillis() + "_" +userId);

            redisData.setCreateTime(new Date());
            Date date = new Date(System.currentTimeMillis() + 1000 * 60 * 15L);
            redisData.setExpireTime(date);
            redisData.setProcessStatus(ProcessStatus.UNPAID.name());

            //订单明细表

            redisData.setActivityReduceAmount(new BigDecimal("0"));
            redisData.setCouponAmount(new BigDecimal("0"));
            redisData.setOriginalTotalAmount(new BigDecimal("0"));
            redisData.setRefundableTime(new Date());
            redisData.setFeightFee(new BigDecimal("0"));
            redisData.setOperateTime(new Date());

            //远程保存订单
            Result<Long> result = orderFeignClient.submitSeckillOrder(redisData);
            redisData.setId(result.getData());
            //更新到redis
            redisTemplate.opsForValue().set(
                    SysRedisConst.SECKILL_ORDER+code,
                    Jsons.toStr(redisData));
        }

        return redisData;
    }

    private OrderInfo prepareTempSeckillOrder(Long skuId) {
        SeckillGoods goods = cacheOpsService.getSeckillGoodsDetail(skuId);

        OrderInfo orderInfo = new OrderInfo();
        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
//        orderInfo.setConsignee("");
//        orderInfo.setConsigneeTel("");
        orderInfo.setTotalAmount(goods.getCostPrice()); //
//        orderInfo.setOrderStatus("");
        orderInfo.setUserId(userId); //
//        orderInfo.setPaymentWay("");
//        orderInfo.setDeliveryAddress("");
//        orderInfo.setOrderComment("");
//        orderInfo.setOutTradeNo("");
        orderInfo.setTradeBody(goods.getSkuName()); //
//        orderInfo.setCreateTime(new Date());
//        orderInfo.setExpireTime(new Date());
//        orderInfo.setProcessStatus("");
//        orderInfo.setTrackingNo("");
//        orderInfo.setParentOrderId(0L);
        orderInfo.setImgUrl(goods.getSkuDefaultImg()); //

        //订单详情
        OrderDetail detail = new OrderDetail();
//        detail.setOrderId(0L);
        detail.setSkuId(skuId);
        detail.setSkuName(goods.getSkuName());
        detail.setImgUrl(goods.getSkuDefaultImg());
        detail.setOrderPrice(goods.getPrice());
        detail.setSkuNum(1);
        detail.setHasStock("1");
        detail.setCreateTime(new Date());
        detail.setSplitTotalAmount(goods.getCostPrice());
//        detail.setSplitActivityAmount(new BigDecimal("0"));
        detail.setSplitCouponAmount(goods.getPrice().subtract(goods.getCostPrice()));
        detail.setUserId(userId);
//        detail.setId(0L);

        List<OrderDetail> orderDetailList = Arrays.asList(detail);
        orderInfo.setOrderDetailList(orderDetailList);
//        orderInfo.setWareId("");
//        orderInfo.setProvinceId(0L);
//        orderInfo.setActivityReduceAmount(new BigDecimal("0"));
//        orderInfo.setCouponAmount(new BigDecimal("0"));
//        orderInfo.setOriginalTotalAmount(new BigDecimal("0"));
//        orderInfo.setRefundableTime(new Date());
//        orderInfo.setFeightFee(new BigDecimal("0"));
//        orderInfo.setOperateTime(new Date());
//        orderInfo.setOrderDetailVoList(Lists.newArrayList());
//        orderInfo.setCouponInfo(new CouponInfo());
//        orderInfo.setId(0L);

        return orderInfo;
    }

    private String generateCode(Long userId,String day,Long skuId){
        //1、生成秒杀码
        String code = MD5.encrypt(userId + "_" + day + "_" + skuId);
        //2、redis存一份
        redisTemplate.opsForValue().setIfAbsent(
                SysRedisConst.SECKILL_CODE + code,"1",1, TimeUnit.DAYS);
        return code;
    }
}
