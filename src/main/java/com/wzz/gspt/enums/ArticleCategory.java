package com.wzz.gspt.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章分类枚举
 */
@Getter
@AllArgsConstructor
public enum ArticleCategory {

    /**
     * 需求标签文章
     */
    DEMAND(1, "需求"),

    /**
     * 产品标签文章
     */
    PRODUCT(2, "产品"),

    /**
     * 公司标签文章
     */
    COMPANY(3, "公司");

    /**
     * 数据库存储值
     */
    @EnumValue
    @JsonValue
    private final Integer value;

    /**
     * 分类说明
     */
    private final String description;
}
