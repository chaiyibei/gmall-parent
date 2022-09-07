package com.atguigu.gmall.model.vo.user;

import lombok.Data;

@Data
public class LoginSuccessVo {
    private String token;
    private String nickName;
}
