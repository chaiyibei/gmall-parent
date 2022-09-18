package com.atguigu.gmall.order.listener;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.to.mq.WareDeductSkuInfo;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.model.to.mq.WareDeductMsg;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.order.service.PaymentInfoService;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.atguigu.gmall.rabbit.service.RabbitService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderPayedListener {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitService rabbitService;

    @Autowired
    PaymentInfoService paymentInfoService;

    @Autowired
    OrderInfoService orderInfoService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderDetailService orderDetailService;

    @RabbitListener(queues = MqConst.QUEUE_ORDER_PAYED)
    public void payedListener(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        Map<String,String> map = Jsons.toObj(message, Map.class);
        //拿到支付宝的交易号 唯一
        String tradeNo = map.get("trade_no");
        try {
            //处理业务，修改订单状态
            //1、保存支付消息
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(map);
            //2、修改订单状态
            orderInfoService.changeOrderStatus(
                    paymentInfo.getOrderId(),
                    paymentInfo.getUserId(),
                    ProcessStatus.PAID,
                    Arrays.asList(ProcessStatus.UNPAID,ProcessStatus.CLOSED));

            //3、通知库存系统，扣减库存
            WareDeductMsg wareDeductMsg = prepareWareDeductMsg(paymentInfo);
            rabbitTemplate.convertAndSend(MqConst.EXCHANGE_WARE_EVENT,
                    MqConst.RK_WARE_DEDUCT,
                    Jsons.toStr(wareDeductMsg));

            channel.basicAck(tag,false);
        }catch (Exception e){
            String uniqKey = SysRedisConst.MQ_RETRY + "order:payed:" + tradeNo;
            rabbitService.retryConsumeMsg(10, uniqKey, tag, channel);
        }
    }

    private WareDeductMsg prepareWareDeductMsg(PaymentInfo paymentInfo) {
        WareDeductMsg msg = new WareDeductMsg();
        msg.setOrderId(paymentInfo.getOrderId());
        Long userId = paymentInfo.getUserId();
        //1、查询出当前订单
        OrderInfo orderInfo = orderInfoService
                .getOrderInfoByOrderIdAndUserId(paymentInfo.getOrderId(),userId);
        msg.setConsignee(orderInfo.getConsignee());
        msg.setConsigneeTel(orderInfo.getConsigneeTel());
        msg.setOrderComment(orderInfo.getOrderComment());
        msg.setOrderBody(orderInfo.getTradeBody());
        msg.setDeliveryAddress(orderInfo.getDeliveryAddress());
        msg.setPaymentWay("2");
        //2、查询出订单的明细
        List<WareDeductSkuInfo> infos = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, orderInfo.getId())
                        .eq(OrderDetail::getUserId, userId)
        ).stream().map(orderDetail -> {
            WareDeductSkuInfo skuInfo = new WareDeductSkuInfo();
            skuInfo.setSkuId(orderDetail.getSkuId());
            skuInfo.setSkuNum(orderDetail.getSkuNum());
            skuInfo.setSkuName(orderDetail.getSkuName());

            return skuInfo;
        }).collect(Collectors.toList());
        msg.setDetails(infos);

        return msg;
    }
}
