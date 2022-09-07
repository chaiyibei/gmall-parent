package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.gateway.properties.AuthUrlProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class GlobalAuthFilter implements GlobalFilter {

    AntPathMatcher matcher = new AntPathMatcher();

    @Autowired
    AuthUrlProperties urlProperties;

    /**
     * Mono
     * Flux
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //1、前置拦截,获取请求路径
        String path = exchange.getRequest().getURI().getPath();
        log.info("{} 请求开始", path);

        //2、无需登录可放行的资源，静态资源：直接放行
        // /js/**, /css/**, /img/**
        for (String url : urlProperties.getNoAuthUrl()) {
            boolean match = matcher.match(url, path);
            if (match){
                return chain.filter(exchange);
            }
        }

        //3、只要是 /api/inner/ 的请求全部拒绝
        for (String url : urlProperties.getDenyUrl()) {
            boolean match = matcher.match(url, path);
            if (match){
                //直接响应json数据即可
                Result<String> result = Result.build("", ResultCodeEnum.PERMISSION);
                return responseResult(result,exchange);
            }
        }

        //4、需要登录的请求，进行权限验证
        for (String url : urlProperties.getLoginAuthUrl()) {
            boolean match = matcher.match(url, path);
            if (match){
                //登录等校验
                //3.1、获取 token 信息【Cookie[token=xxx]】【Header[token=xxx]】

                //3.2、校验 token

                //3.3、判断用户信息是否正确

            }
        }

        //能走到这儿，既不是静态资源直接放行，也不是必须登录才能访问的，就是一个普通请求
        //普通请求只要带了token，说明可能登录了。只要登录了，就透传用户id

        return chain.filter(exchange);

        //4、对登录后的请求进行 user_id 透传
//        Mono<Void> filter = chain.filter(exchange).doFinally((signalType) -> {
//            log.info("{} 请求结束", path);
//        });
//        return filter;
    }

    private Mono<Void> responseResult(Result<String> result, ServerWebExchange exchange) {

        return null;
    }
}
