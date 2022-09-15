package com.atguigu.gmall.rabbit.annotation;

import com.atguigu.gmall.rabbit.config.AppRabbitConfiguration;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurationSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(AppRabbitConfiguration.class)
public @interface EnableAppRabbit {
}
