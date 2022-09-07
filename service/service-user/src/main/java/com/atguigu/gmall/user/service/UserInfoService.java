package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.model.vo.user.LoginSuccessVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 柴小贝
* @description 针对表【user_info(用户表)】的数据库操作Service
* @createDate 2022-09-06 16:18:58
*/
public interface UserInfoService extends IService<UserInfo> {
    /**
     * 用户登录
     * @param loginName
     * @param passwd
     * @return
     */
    LoginSuccessVo login(String loginName, String passwd);

    /**
     * 退出登录
     * @param token
     */
    void logout(String token);
}
