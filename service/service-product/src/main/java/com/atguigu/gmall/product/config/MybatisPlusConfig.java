package com.atguigu.gmall.product.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    /**
     *
     * @return
     */

    //1、把MyBatisPlus的插件主体（总插件）放到容器
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        //插件主体
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        //加入内部的分页插件   码溢出以后，默认就访问最后一页即可
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;

    }
}
