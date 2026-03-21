package com.wzz.gspt.dto.article;

import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import lombok.Data;

import java.util.List;

/**
 * 发布或修改文章请求对象
 */
@Data
public class ArticlePublishRequest {

    /**
     * 文章 ID，新增时为空，修改时必填
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
     * 图片墙
     */
    private List<ArticleImageItemRequest> imageWall;

    /**
     * 文章状态
     */
    private ArticleStatus status;

    /**
     * 文章分类
     */
    private ArticleCategory category;
}
