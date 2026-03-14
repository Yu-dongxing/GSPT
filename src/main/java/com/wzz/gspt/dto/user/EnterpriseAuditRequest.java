package com.wzz.gspt.dto.user;

import com.wzz.gspt.enums.UserAuditStatus;
import lombok.Data;

/**
 * 企业审核请求对象
 */
@Data
public class EnterpriseAuditRequest {

    /**
     * 企业用户 ID
     */
    private Long userId;

    /**
     * 审核状态，只允许通过或拒绝
     */
    private UserAuditStatus auditStatus;

    /**
     * 审核备注或拒绝原因
     */
    private String auditRemark;
}
