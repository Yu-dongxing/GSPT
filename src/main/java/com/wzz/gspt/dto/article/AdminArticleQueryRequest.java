package com.wzz.gspt.dto.article;

import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import lombok.Data;

/**
 * 管理员文章分页查询请求对象
 */
@Data
public class AdminArticleQueryRequest {

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
     * 文章状态
     */
    private ArticleStatus status;

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
