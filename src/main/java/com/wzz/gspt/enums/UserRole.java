package com.wzz.gspt.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    USER(1, "普通用户"),
    VIP(2, "会员用户"),
    ADMIN(3, "总后台");

    /**
     * 标记数据库存储的值
     * 标记 JSON 返回和接收的值
     */
    @EnumValue
    @JsonValue
    private final Integer value;

    private final String description;
}