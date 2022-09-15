package com.atguigu.gmall.rabbit.constant;

public class MqConst {
    //订单交换机
    public static final String EXCHANGE_ORDER_EVENT = "order-event-exchange";

    //订单延迟队列
    public static final String QUEUE_ORDER_DELAY = "order-delay-queue";

    //订单死信路由键
    public static final String RK_ORDER_DEAD = "order-dead";

    //订单延迟路由键
    public static final String RK_ORDER_CREATED = "order-created";

    //订单死单对列
    public static final String QUEUE_ORDER_DEAD = "order-dead-queue";
}
