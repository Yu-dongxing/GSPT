package com.wzz.gspt.dto.user;

import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import lombok.Data;

/**
 * 管理员新增或修改用户请求对象
 */
@Data
public class UserAdminSaveRequest {

    /**
     * 用户 ID，新增时可为空，修改时必填
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

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

    /**
     * 用户角色
     */
    private UserRole role;

    /**
     * 审核状态
     */
    private UserAuditStatus auditStatus;

    /**
     * 审核备注
     */
    private String auditRemark;
}
