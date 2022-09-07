package com.atguigu.gmall.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.auth")
@Component
public class AuthUrlProperties {

    List<String> noAuthUrl;
    List<String> loginAuthUrl;
    String loginPage;
    List<String> denyUrl;
}
