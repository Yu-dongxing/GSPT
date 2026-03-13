package com.wzz.gspt.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserAuditStatus {

    NONE(0, "无需审核(普通用户/后台)"),
    PENDING(1, "待审核"),
    APPROVED(2, "审核通过"),
    REJECTED(3, "审核拒绝");

    @EnumValue
    @JsonValue
    private final Integer value;
    private final String description;
}