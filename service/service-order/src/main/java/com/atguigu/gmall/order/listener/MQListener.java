package com.atguigu.gmall.order.listener;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class MQListener {

    private ConcurrentHashMap<String,AtomicInteger> counter = new ConcurrentHashMap<>();
    AtomicInteger count = new AtomicInteger(0);

//    @RabbitListener(queues = "haha")
    public void listenHaha(Message message, Channel channel) throws IOException {
        //1、给个消息发送的时候，给一个唯一标志
        String content = new String(message.getBody());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        counter.putIfAbsent(content,new AtomicInteger(0));
        try {
            System.out.println("收到的消息：" + content);
            //处理业务 TODO
            int i = 10/0;
            channel.basicAck(deliveryTag,false); //不要批量回复
        }catch (Exception e){
            log.error("消息消费失败：{}",content);
            AtomicInteger integer = counter.get(content);
            System.out.println("加到了："+integer);
            if (integer.incrementAndGet() <= 10){
                //重新存储这个消息，待下个人继续处理
                channel.basicNack(deliveryTag,false,true);
            }else {
                //已经超过最大重试次数
                //TODO 重试失败消息表，人工补偿
                log.error("{} 消息重试10次依然失败，已经记录在表",content);
                channel.basicNack(deliveryTag,false,false);
            }
        }
    }
}
