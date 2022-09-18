package com.atguigu.gmall.rabbit.service;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class RabbitService {
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     *
     * @param maxNum 指定最大重试次数
     * @param uniqKey 指定识别消息的唯一key
     * @param deliveryTag 消息tag
     * @param channel 通道
     * @throws IOException
     */
    public void retryConsumeMsg(Integer maxNum, String uniqKey, Long deliveryTag, Channel channel) throws IOException {
        //Lua脚本
        Long increment = redisTemplate.opsForValue().increment(uniqKey);
        if (increment <= 10){
            channel.basicNack(deliveryTag,false,true);
        }else {
            channel.basicNack(deliveryTag,false,false);
            redisTemplate.delete(uniqKey);
            log.error("消息：{},消费失败{}次",deliveryTag,maxNum);
        }
    }
}
