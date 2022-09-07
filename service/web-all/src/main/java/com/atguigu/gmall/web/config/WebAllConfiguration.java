package com.atguigu.gmall.web.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAllConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(){

        return null;
    }
}
