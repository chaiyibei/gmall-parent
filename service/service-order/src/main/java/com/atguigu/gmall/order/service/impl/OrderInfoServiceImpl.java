package com.atguigu.gmall.order.service.impl;
import java.math.BigDecimal;

import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.to.mq.OrderMsg;
import com.atguigu.gmall.model.vo.order.CartInfoVo;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.atguigu.gmall.model.activity.CouponInfo;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.vo.order.OrderSubmitVo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author 柴小贝
* @description 针对表【order_info(订单表 订单表)】的数据库操作Service实现
* @createDate 2022-09-12 14:32:08
*/
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
    implements OrderInfoService{
    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailService orderDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Transactional //数据库成功 + 消息成功
    @Override
    public Long saveOrder(OrderSubmitVo submitVo,String tradeNo) {
        //1、准备订单数据
        OrderInfo orderInfo = prepareOrderInfo(submitVo,tradeNo);
        //2.1、保存 OrderInfo
        orderInfoMapper.insert(orderInfo);
        //2.2、保存 OrderDetail
        List<OrderDetail> details = prepareOrderDetail(submitVo,orderInfo);
        orderDetailService.saveBatch(details);

        //发送订单创建完成消息
        OrderMsg orderMsg = new OrderMsg(orderInfo.getId(),orderInfo.getUserId());
        rabbitTemplate.convertAndSend(
                MqConst.EXCHANGE_ORDER_EVENT,
                MqConst.RK_ORDER_CREATED,
                Jsons.toStr(orderMsg)
        );

        //3、返回订单id
        return orderInfo.getId();
    }

    @Transactional
    @Override
    public void changeOrderStatus(Long orderId, Long userId, ProcessStatus closed, List<ProcessStatus> expected) {
        String orderStatus = closed.getOrderStatus().name();
        String processStatus = closed.name();
        List<String> expects = expected.stream()
                .map(status -> status.name())
                .collect(Collectors.toList());

        orderInfoMapper.updateOrderStatus(orderId,userId,processStatus,orderStatus,expects);
    }

    @Override
    public OrderInfo getOrderInfoByOutTradeNoAndUserId(String outTradeNo, Long userId) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOutTradeNo, outTradeNo)
                .eq(OrderInfo::getUserId, userId));
        return orderInfo;
    }

    @Override
    public OrderInfo getOrderInfoByOrderIdAndUserId(Long orderId, Long userId) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId));
        return orderInfo;
    }

    /**
     * 订单明细
     * @param submitVo
     * @param orderInfo
     * @return
     */
    private List<OrderDetail> prepareOrderDetail(OrderSubmitVo submitVo, OrderInfo orderInfo) {
        List<OrderDetail> details = submitVo.getOrderDetailList()
                .stream()
                .map(vo -> {
                    OrderDetail detail = new OrderDetail();
                    detail.setOrderId(orderInfo.getId());
                    detail.setSkuId(vo.getSkuId());
                    detail.setSkuName(vo.getSkuName());
                    detail.setImgUrl(vo.getImgUrl());
                    detail.setOrderPrice(vo.getOrderPrice());
                    detail.setSkuNum(vo.getSkuNum());
                    detail.setHasStock(vo.getHasStock());
                    detail.setCreateTime(new Date());
                    detail.setSplitTotalAmount(vo.getOrderPrice()
                            .multiply(new BigDecimal(vo.getSkuNum() + "")));
                    detail.setSplitActivityAmount(new BigDecimal("0"));
                    detail.setSplitCouponAmount(new BigDecimal("0"));
                    detail.setUserId(orderInfo.getUserId());
//                    detail.setId(0L);
                    return detail;
                }).collect(Collectors.toList());

        return details;
    }

    private OrderInfo prepareOrderInfo(OrderSubmitVo submitVo,String tradeNo) {
        OrderInfo orderInfo = new OrderInfo();
        //收货人
        orderInfo.setConsignee(submitVo.getConsignee());
        orderInfo.setConsigneeTel(submitVo.getConsigneeTel());
        //订单总额
        BigDecimal totalAmount = submitVo.getOrderDetailList()
                .stream()
                .map(info -> info.getOrderPrice().multiply(new BigDecimal(info.getSkuNum() + "")))
                .reduce((o1, o2) -> o1.add(o2))
                .get();
        orderInfo.setTotalAmount(totalAmount);
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //用户id
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        orderInfo.setUserId(authInfo.getUserId());
        //支付方式
        orderInfo.setPaymentWay(submitVo.getPaymentWay());
        //配送地址
        orderInfo.setDeliveryAddress(submitVo.getDeliveryAddress());
        //订单备注
        orderInfo.setOrderComment(submitVo.getOrderComment());
        //对外流水号
        orderInfo.setOutTradeNo(tradeNo);
        //交易体 拿到这个订单中购买的第一个商品的名字，作为订单的体
        orderInfo.setTradeBody(submitVo.getOrderDetailList().get(0).getSkuName());
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间。订单多久没支付以后过期，过期变为已关闭状态
        orderInfo.setExpireTime(new Date(
                System.currentTimeMillis() + 1000 * SysRedisConst.ORDER_CLOSE_TTL));
        //订单的处理状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        //物流单编号
        orderInfo.setTrackingNo("");
        //父订单
        orderInfo.setParentOrderId(0L);

        //订单的图片
        orderInfo.setImgUrl(submitVo.getOrderDetailList().get(0).getImgUrl());
        //订单明细。值这个订单到底买了哪些商品
//        List<OrderDetail> orderDetails = submitVo.getOrderDetailList()
//                .stream()
//                .map(cartInfoVo -> {
//                    OrderDetail detail = new OrderDetail();
//                    //TODO
//                    return detail;
//                }).collect(Collectors.toList());
//        orderInfo.setOrderDetailList(orderDetails);
        //仓库id
//        orderInfo.setWareId("");
        //省
//        orderInfo.setProvinceId(0L);
        //当前订单被优惠活动减掉的金额
        orderInfo.setActivityReduceAmount(new BigDecimal("0"));
        //当前订单被优惠券减掉的金额
        orderInfo.setCouponAmount(new BigDecimal("0"));
        //订单原始总额
        orderInfo.setOriginalTotalAmount(totalAmount);
        //可退款最后日期
        orderInfo.setRefundableTime(new Date(
                System.currentTimeMillis() + 1000 * SysRedisConst.ORDER_REFUND_TTL));
        //运费。第三方物流平台，动态计算运费
        orderInfo.setFeightFee(new BigDecimal("0"));
        //
        orderInfo.setOperateTime(new Date());
        //
//        orderInfo.setOrderDetailVoList(Lists.newArrayList());
        //
//        orderInfo.setCouponInfo(new CouponInfo());
        //
//        orderInfo.setId(0L);

        return orderInfo;
    }
}




