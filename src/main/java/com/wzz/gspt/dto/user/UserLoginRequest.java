package com.wzz.gspt.dto.user;

import lombok.Data;

/**
 * 用户登录请求对象
 */
@Data
public class UserLoginRequest {

    /**
     * 用户名，当前业务中使用手机号作为登录账号
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;
}
