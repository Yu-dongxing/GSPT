package com.wzz.gspt.dto.user;

import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import lombok.Data;

@Data
public class UserArticleQueryRequest {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 分页页码默认为1
     */
    private Long pageNum = 1L;

    /**
     * 分页数量，默认为10
     */
    private Long pageSize = 10L;

    /**
     * 主题
     */
    private String title;

    /**
     * 状态
     */
    private ArticleStatus status;

    /**
     * 文章分类
     */
    private ArticleCategory category;

    /**
     * 开始时间
     */
    private String startCreateTime;


    /**
     * 结束时间
     */
    private String endCreateTime;
}
