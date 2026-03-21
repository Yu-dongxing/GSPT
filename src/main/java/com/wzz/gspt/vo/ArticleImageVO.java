package com.wzz.gspt.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageVO {

    /**
     * 图片墙id
     */
    private Long id;

    /**
     * 文件id
     */
    private Long fileId;

    /**
     * 图片访问路径地址
     */
    private String imageUrl;

    /**
     * 排序
     */
    private Integer sortOrder;
}
