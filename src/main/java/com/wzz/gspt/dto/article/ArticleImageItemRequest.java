package com.wzz.gspt.dto.article;

import lombok.Data;

@Data
public class ArticleImageItemRequest {

    /**
     * 文件id
     */
    private Long fileId;

    /**
     * 图片访问地址
     */
    private String imageUrl;

    /**
     * 排序
     */
    private Integer sortOrder;
}
