package com.wzz.gspt.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArticleStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    /**
     * 标记数据库存储的值
     * 标记 JSON 返回和接收的值
     */
    @EnumValue
    @JsonValue
    private final Integer value;

    private final String description;
}