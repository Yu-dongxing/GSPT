package com.wzz.gspt.vo;

import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录结果返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO {

    /**
     * 当前登录用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private UserRole role;

    /**
     * 审核状态
     */
    private UserAuditStatus auditStatus;

    /**
     * token 名称
     */
    private String tokenName;

    /**
     * token 值
     */
    private String tokenValue;
}
