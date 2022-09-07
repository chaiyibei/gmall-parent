package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.model.vo.user.LoginSuccessVo;
import com.atguigu.gmall.user.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@RequestMapping("/api/user")
@RestController
public class UserController {
    @Autowired
    UserInfoService userInfoService;

    // {loginName: "admin", passwd: "111111"}
    /**
     * 用户登录
     * @param userInfo
     * @return
     */
    @PostMapping("/passport/login")
    public Result login(@RequestBody UserInfo userInfo){
        LoginSuccessVo vo = userInfoService.login(userInfo.getLoginName(),userInfo.getPasswd());
        if (vo != null){
            return Result.ok(vo);
        }

        return Result.build("", ResultCodeEnum.LOGIN_ERROR);
    }

    /**
     * 退出登录
     * @return
     */
    @GetMapping("/passport/logout")
    public Result logout(@RequestHeader("token") String token){
        userInfoService.logout(token);
        return Result.ok();
    }
}
