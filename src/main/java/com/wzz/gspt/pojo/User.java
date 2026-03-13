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
     * 角色标识
     */
    @TableField("role")
    @ColumnComment("角色标识：1-普通用户, 2-会员用户, 3-总后台")
    @DefaultValue("1")
    private UserRole role;
}