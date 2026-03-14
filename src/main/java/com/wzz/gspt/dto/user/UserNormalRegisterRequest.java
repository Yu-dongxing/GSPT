package com.wzz.gspt.dto.user;

import lombok.Data;

/**
 * 普通用户注册请求对象
 */
@Data
public class UserNormalRegisterRequest {

    /**
     * 手机号，同时作为用户名
     */
    private String phone;

    /**
     * 登录密码
     */
    private String password;
}
