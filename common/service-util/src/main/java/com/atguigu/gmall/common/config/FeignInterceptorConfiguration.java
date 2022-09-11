package com.atguigu.gmall.common.config;

import com.atguigu.gmall.common.constant.SysRedisConst;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class FeignInterceptorConfiguration {
    /**
     * 把用户id带到feign即将发起的新请求中
     * @return
     */
    @Bean
    public RequestInterceptor userHeaderInterceptor(){

//        return new RequestInterceptor() {
//            @Override
//            public void apply(RequestTemplate requestTemplate) {
//
//            }
//        };

        return (requestTemplate) -> {
            //随时调用，获取老请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(SysRedisConst.USERID_HEADER);

            //用户id头添加到feign的新情求中
            requestTemplate.header(SysRedisConst.USERID_HEADER,userId);

            //临时id头添加到feign的新情求中
            String tempId = request.getHeader(SysRedisConst.USERTEMPID_HEADER);
            requestTemplate.header(SysRedisConst.USERTEMPID_HEADER,tempId);
        };
    }
}
