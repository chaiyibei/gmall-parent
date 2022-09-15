package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.rabbit.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建订单用的所有交换机和对列
 */
@Configuration
public class OrderEventMqConfiguration {

    /**
     * 项目启动自动创建交换机
     * @return
     */
    @Bean
    public Exchange orderEventExchange(){
        /**
         * String name,
         * boolean durable,
         * boolean autoDelete,
         * Map<String, Object> arguments
         */
        TopicExchange exchange = new TopicExchange(
                MqConst.EXCHANGE_ORDER_EVENT,
                true,
                false,
                null);
        return exchange;
    }

    /**
     * 订单延迟队列
     * @return
     */
    @Bean
    public Queue orderDelayQueue(){
        /**
         * String name, boolean durable,
         * boolean durable
         * boolean exclusive,
         * boolean autoDelete,
         * @Nullable Map<String, Object> arguments
         */
        Map<String, Object> arguments = new HashMap<>();
        //设置延迟队列参数
        arguments.put("x-message-ttl", SysRedisConst.ORDER_CLOSE_TTL*1000);
//        arguments.put("x-message-ttl", 10000);
        arguments.put("x-dead-letter-exchange",MqConst.EXCHANGE_ORDER_EVENT);
        arguments.put("x-dead-letter-routing-key",MqConst.RK_ORDER_DEAD);

        return new Queue(
                MqConst.QUEUE_ORDER_DELAY,
                true,
                false,
                false,
                arguments
                );
    }

    /**
     * 延迟队列和交换机绑定
     * @return
     */
    @Bean
    public Binding orderDelayQueueBinding(){
        /**
         * String destination, 目的地
         * Binding.DestinationType destinationType, 目的地类型
         * String exchange, 交换机
         * String routingKey, 路由键
         * @Nullable Map<String, Object> arguments
         */
        return new Binding(
                MqConst.QUEUE_ORDER_DELAY,
                Binding.DestinationType.QUEUE,
                MqConst.EXCHANGE_ORDER_EVENT,
                MqConst.RK_ORDER_CREATED,
                null
        );
    }

    /**
     * 死单对列，保存所有过期订单，进行关单
     * @return
     */
    @Bean
    public Queue orderDeadQueue(){
        /**
         * String name, boolean durable,
         * boolean durable
         * boolean exclusive,
         * boolean autoDelete,
         * @Nullable Map<String, Object> arguments
         */
        return new Queue(
                MqConst.QUEUE_ORDER_DEAD,
                true,
                false,
                false,
                null
        );
    }

    /**
     * 死单队列和交换机绑定
     * @return
     */
    @Bean
    public Binding orderDeadQueueBinding(){
        /**
         * String destination, 目的地
         * Binding.DestinationType destinationType, 目的地类型
         * String exchange, 交换机
         * String routingKey, 路由键
         * @Nullable Map<String, Object> arguments
         */
        return new Binding(
                MqConst.QUEUE_ORDER_DEAD,
                Binding.DestinationType.QUEUE,
                MqConst.EXCHANGE_ORDER_EVENT,
                MqConst.RK_ORDER_DEAD,
                null
        );
    }
}
