package com.wzz.gspt.dto.user;

import lombok.Data;

/**
 * 用户修改密码请求对象
 */
@Data
public class UserPasswordChangeRequest {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
