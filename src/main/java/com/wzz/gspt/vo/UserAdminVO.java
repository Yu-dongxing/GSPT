package com.wzz.gspt.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminVO {

    private Long id;

    private String username;

    private String password;

    private String email;

    private String companyName;

    private String licenseName;

    private Long licenseFileId;

    private String licenseUrl;

    private UserRole role;

    private UserAuditStatus auditStatus;

    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
