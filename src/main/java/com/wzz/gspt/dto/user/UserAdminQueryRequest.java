package com.wzz.gspt.dto.user;

import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import lombok.Data;

/**
 * 管理员查询用户分页请求对象
 */
@Data
public class UserAdminQueryRequest {

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 10L;

    /**
     * 用户名
     */
    private String username;

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
     * 用户角色
     */
    private UserRole role;

    /**
     * 审核状态
     */
    private UserAuditStatus auditStatus;
}
