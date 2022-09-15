package com.atguigu.gmall.order.listener;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.model.to.mq.OrderMsg;
import com.atguigu.gmall.order.biz.OrderBizService;
import com.atguigu.gmall.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单关闭监听器
 */
@Slf4j
@Component
public class OrderCloseListener {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    OrderBizService orderBizService;

    @RabbitListener(queues = MqConst.QUEUE_ORDER_DEAD)
    public void orderClose(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        //1、拿到订单消息
        OrderMsg orderMsg = Jsons.toObj(message, OrderMsg.class);
        try {
            //2、进行订单关闭  保证幂等性
            log.info("监听超时订单{}，正在关闭",orderMsg);
            orderBizService.closeOrder(orderMsg.getOrderId(),orderMsg.getUserId());

            channel.basicAck(deliveryTag,false);
        }catch (Exception e){
            log.error("消息关闭业务失败。消息：{},失败原因：{}",orderMsg,e);
            //Lua脚本
            Long increment = redisTemplate.opsForValue().increment(SysRedisConst.MQ_RETRY + "order:" + orderMsg.getOrderId());
            if (increment <= 10){
                channel.basicNack(deliveryTag,false,true);
            }else {
                channel.basicNack(deliveryTag,false,false);
                redisTemplate.delete(SysRedisConst.MQ_RETRY + "order:" + orderMsg.getOrderId());
            }
        }
    }
}
















