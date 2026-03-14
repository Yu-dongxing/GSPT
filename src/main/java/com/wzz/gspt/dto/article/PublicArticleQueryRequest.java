package com.wzz.gspt.dto.article;

import com.wzz.gspt.enums.ArticleCategory;
import lombok.Data;

/**
 * 访客公共文章分页查询请求对象
 */
@Data
public class PublicArticleQueryRequest {

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
     * 作者名称
     */
    private String authorName;

    /**
     * 文章分类
     */
    private ArticleCategory category;

    /**
     * 创建开始时间
     */
    private String startCreateTime;

    /**
     * 创建结束时间
     */
    private String endCreateTime;
}
