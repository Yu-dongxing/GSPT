package com.wzz.gspt.dto.article;

import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import lombok.Data;

/**
 * 查询当前用户文章列表请求对象
 */
@Data
public class MyArticleQueryRequest {

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 10L;

    /**
     * 标题关键字
     */
    private String title;

    /**
     * 文章状态
     */
    private ArticleStatus status;

    /**
     * 文章分类
     */
    private ArticleCategory category;
}
