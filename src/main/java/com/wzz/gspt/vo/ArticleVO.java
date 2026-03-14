package com.wzz.gspt.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文章返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleVO {

    /**
     * 文章 ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 预览内容
     */
    private String previewContent;

    /**
     * 封面文件 ID
     */
    private Long coverFileId;

    /**
     * 封面访问路径
     */
    private String coverImageUrl;

    /**
     * 预览图文件 ID
     */
    private Long previewImageId;

    /**
     * 预览图访问路径
     */
    private String previewImageUrl;

    /**
     * 作者 ID
     */
    private Long authorId;

    /**
     * 作者用户名
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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
