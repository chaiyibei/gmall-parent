package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.gateway.properties.AuthUrlProperties;
import com.atguigu.gmall.model.user.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpHead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Webflux：响应式web编程【消息队列分布式】
 *      内存版的消息队列
 *
 * servlet：阻塞式编程方式
 *
 * 单数据流：数据发布者 Mono
 * 多数据流：Flux
 */
@Slf4j
@Component
public class GlobalAuthFilter implements GlobalFilter {
    // 路径匹配器   ant风格路径
    AntPathMatcher matcher = new AntPathMatcher();

    @Autowired
    AuthUrlProperties urlProperties;

    @Autowired
    StringRedisTemplate redisTemplate;

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
        String uri = exchange.getRequest().getURI().toString();
        log.info("{} 请求开始", path);

        //2、无需登录可放行的资源，（静态资源：直接放行）
        // /js/**, /css/**, /img/**
        for (String url : urlProperties.getNoAuthUrl()) {
            boolean match = matcher.match(url, path);
            if (match){
                return chain.filter(exchange);
            }
        }

        //静态资源虽然带了token，不用校验token，直接访问
        //能走到这儿，说明不是直接放行的资源

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
                String tokenValue = getTokenValue(exchange);

                //3.2、校验 token
                UserInfo userInfo = getTokenUserInfo(tokenValue);

                //3.3、判断用户信息是否正确
                if (userInfo != null){
                    //redis中有此用户
                    ServerWebExchange webExchange = userIdTransport(userInfo, exchange);
                    return chain.filter(webExchange);
                }else {
                    //redis中无此用户【假令牌、token没有，没登录】
                    //重定向到登录页
                    return redirectToCustomPage(urlProperties.getLoginPage()+"?originUrl="+uri
                            ,exchange);
                }

            }
        }

        //能走到这儿，既不是静态资源直接放行，也不是必须登录才能访问的，就是一个普通请求
        //普通请求只要带了token，说明可能登录了。只要登录了，就透传用户id
        String tokenValue = getTokenValue(exchange);
        UserInfo userInfo = getTokenUserInfo(tokenValue);
        if (userInfo != null){
            exchange = userIdTransport(userInfo, exchange);
        }else {
            //带了token，但是是假令牌
            if (!StringUtils.isEmpty(tokenValue)){
                //重定向到登录页
                return redirectToCustomPage(urlProperties.getLoginPage()+"?originUrl="+uri
                        ,exchange);
            }
        }
        return chain.filter(exchange);

        //4、对登录后的请求进行 user_id 透传
//        Mono<Void> filter = chain.filter(exchange).doFinally((signalType) -> {
//            log.info("{} 请求结束", path);
//        });
//        return filter;
    }

    /**
     * 响应一个结果
     * @param result
     * @param exchange
     * @return
     */
    private Mono<Void> responseResult(Result<String> result, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        String jsonStr = Jsons.toStr(result);
        DataBuffer dataBuffer = response.bufferFactory().wrap(jsonStr
                .getBytes());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(Mono.just(dataBuffer));
    }

    /**
     * 用户id透传
     * @param userInfo
     * @param exchange
     */
    private ServerWebExchange userIdTransport(UserInfo userInfo, ServerWebExchange exchange) {
        if (userInfo != null){
            //请求一旦发来，所有的请求数据都是固定的，只能读不能改
            ServerHttpRequest request = exchange.getRequest();

            //根据原来的请求，封装一个请求
            ServerHttpRequest newRequest = exchange.getRequest()
                    .mutate()
                    .header(SysRedisConst.USERID_HEADER, userInfo.getId().toString())
                    .build();

            //放行的时候传改掉的exchange
            ServerWebExchange webExchange = exchange.mutate()
                    .request(newRequest)
                    .response(exchange.getResponse())
                    .build();
            return webExchange;

//            request.getHeaders().add(SysRedisConst.USERID_HEADER,userInfo.getId().toString());
        }
        return exchange;
    }

    /**
     * 重定向到指定位置
     * @param location
     * @param exchange
     * @return
     */
    private Mono<Void> redirectToCustomPage(String location, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        //1、重定向【302状态码 + 响应头中 Location：新位置】
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().add(HttpHeaders.LOCATION,location);

        //2、清除旧的错误的Cookie
        ResponseCookie cookie = ResponseCookie
                .from("token", "deleteOldCookie")
                .domain(".gmall.com")
                .path("/")
                .maxAge(0)
                .build();
        response.getCookies().add("token",cookie);

        //3、响应结束
        return response.setComplete();
    }

    /**
     * 根据token的值去redis中查到用户信息
     * @param tokenValue
     * @return
     */
    private UserInfo getTokenUserInfo(String tokenValue) {
        String json = redisTemplate.opsForValue().get(SysRedisConst.LOGIN_USER + tokenValue);
        if (!StringUtils.isEmpty(json)){
            return Jsons.toObj(json,UserInfo.class);
        }
        return null;
    }

    /**
     * 取到token对应的值
     * @param exchange
     * @return
     */
    private String getTokenValue(ServerWebExchange exchange) {
        //【Cookie[token=xxx]】【Header[token=xxx]】可能都有
        //1、先检查 Cookie 中有没有这个 token
        String tokenValue = "";
        HttpCookie token = exchange.getRequest()
                .getCookies().getFirst("token");
        if (token != null){
            tokenValue = token.getValue();
            return tokenValue;
        }

        //2、说明cookie中没有
        tokenValue = exchange.getRequest()
                .getHeaders().getFirst("token");

        return tokenValue;
    }
}
