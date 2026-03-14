package com.wzz.gspt.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户注册结果返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterVO {

    /**
     * 用户 ID
     */
    private Long id;

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
     * 营业执照文件记录 ID
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

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
