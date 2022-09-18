package com.atguigu.gmall.order.listener;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.to.mq.WareDeductStatusMsg;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.atguigu.gmall.rabbit.service.RabbitService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
public class OrderStockDeductListener {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitService rabbitService;

    @Autowired
    OrderInfoService orderInfoService;

    /**
     * 如果没有自动创建
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(value = MqConst.QUEUE_WARE_ORDER,
                    durable = "true",exclusive = "false",autoDelete = "false"),
                    exchange = @Exchange(name = MqConst.EXCHANGE_WARE_ORDER,
                            durable = "true",autoDelete = "false",type = "direct"),
                    key = MqConst.RK_WARE_ORDER
            )
    }) //库存扣减结果监听
    public void stockDeductListener(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        //按照库存扣减结果
        WareDeductStatusMsg msg = Jsons.toObj(message, WareDeductStatusMsg.class);
        Long orderId = msg.getOrderId();
        try {
            log.info("订单服务【修改订单出库状态】 监听到库存扣减结果：{}",msg);

            //查询订单
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            ProcessStatus status = null;
            switch (msg.getStatus()){
                case "DEDUCTED": status = ProcessStatus.WAITING_DELEVER;break;
                case "OUT_OF_STOCK": status = ProcessStatus.WAITING_SCHEDULE;break;
                default: status = ProcessStatus.PAID;
            }
            orderInfoService.changeOrderStatus(orderId,
                    orderInfo.getUserId(),
                    status,
                    Arrays.asList(ProcessStatus.PAID));
            channel.basicAck(tag,false);
        }catch (Exception e){
            String uniqKey = SysRedisConst.MQ_RETRY + "stock:order:deduct:" + orderId;
            rabbitService.retryConsumeMsg(10,uniqKey,tag,channel);
        }


    }
}
