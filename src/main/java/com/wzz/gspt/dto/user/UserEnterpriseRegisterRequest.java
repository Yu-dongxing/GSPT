package com.wzz.gspt.dto.user;

import lombok.Data;

/**
 * 企业用户注册请求对象
 */
@Data
public class UserEnterpriseRegisterRequest {

    /**
     * 手机号，同时作为用户名
     */
    private String phone;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 营业执照名称
     */
    private String licenseName;

    /**
     * 营业执照文件记录 ID
     */
    private Long licenseFileId;

    /**
     * 营业执照访问路径
     */
    private String licenseUrl;
}
