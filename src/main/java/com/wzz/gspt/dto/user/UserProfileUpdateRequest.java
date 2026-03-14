package com.wzz.gspt.dto.user;

import lombok.Data;

/**
 * 用户修改个人信息请求对象
 */
@Data
public class UserProfileUpdateRequest {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 营业执照名称
     */
    private String licenseName;

    /**
     * 营业执照文件 ID
     */
    private Long licenseFileId;

    /**
     * 营业执照访问路径
     */
    private String licenseUrl;
}
