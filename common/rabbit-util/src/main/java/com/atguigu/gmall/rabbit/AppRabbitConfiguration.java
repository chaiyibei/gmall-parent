package com.atguigu.gmall.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableRabbit
@Configuration
public class AppRabbitConfiguration {


    @Bean
    RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer,
                                  ConnectionFactory connectionFactory){
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template,connectionFactory);
        //感知消息是否真的被投递到对列


        //设置重试器，发送失败会重试3次


        return template;
    }

}
