package com.wzz.gspt.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRole {

    /**
     * 普通用户
     */
    USER(1, "普通用户"),

    /**
     * 企业用户
     */
    ENTERPRISE(2, "企业用户"),

    /**
     * 后台管理员
     */
    ADMIN(3, "总后台");

    /**
     * 数据库存储值
     */
    @EnumValue
    @JsonValue
    private final Integer value;

    /**
     * 角色说明
     */
    private final String description;
}
