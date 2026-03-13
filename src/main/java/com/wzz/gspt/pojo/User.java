package com.wzz.gspt.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzz.gspt.annotation.db.*;
import com.wzz.gspt.common.*;
import com.wzz.gspt.enums.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableComment("系统用户表")
@TableName("sys_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @TableField("username")
    @ColumnComment("用户名/登录账号")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    @ColumnComment("密码")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    @ColumnComment("用户昵称")
    @DefaultValue("'默认用户昵称'")
    private String nickname;

    /**
     * 邮箱地址(企业用户必填,用于接收审核通知)
     */
    @TableField("email")
    @ColumnComment("邮箱地址(企业用户必填,用于接收审核通知)")
    private String email;

    // ================= 企业认证专用字段 =================

    /**
     * 公司名称(企业用户必填)
     */
    @TableField("company_name")
    @ColumnComment("公司名称(企业用户必填)")
    private String companyName;

    /**
     * 营业执照名称(企业用户必填)
     */
    @TableField("license_name")
    @ColumnComment("营业执照名称(企业用户必填)")
    private String licenseName;

    /**
     * 营业执照照片访问地址(关联sys_file表直接存URL)
     */
    @TableField("license_url")
    @ColumnComment("营业执照照片访问地址(关联sys_file表直接存URL)")
    private String licenseUrl;

    /**
     * 营业执照照片文件id(关联sys_file表直接存ID)
     */
    @TableField("license_file_id")
    @ColumnComment("营业执照照片文件id(关联sys_file表直接存ID)")
    private String licenseFileId;

    /**
     * 审核状态：0-无需审核, 1-待审核, 2-审核通过, 3-审核拒绝
     */
    @TableField("audit_status")
    @ColumnComment("审核状态：0-无需审核, 1-待审核, 2-审核通过, 3-审核拒绝")
    @DefaultValue("0")
    private UserAuditStatus auditStatus;

    /**
     * 审核备注/拒绝原因
     */
    @TableField("audit_remark")
    @ColumnComment("审核备注/拒绝原因")
    private String auditRemark;


    /**
     * 角色标识
     */
    @TableField("role")
    @ColumnComment("角色标识：1-普通用户, 2-企业用户, 3-总后台")
    @DefaultValue("1")
    private UserRole role;
}