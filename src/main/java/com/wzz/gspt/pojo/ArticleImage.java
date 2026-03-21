package com.wzz.gspt.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzz.gspt.annotation.db.ColumnComment;
import com.wzz.gspt.annotation.db.ForeignKey;
import com.wzz.gspt.annotation.db.Index;
import com.wzz.gspt.annotation.db.TableComment;
import com.wzz.gspt.common.BaseEntity;
import com.wzz.gspt.enums.db.ForeignKeyAction;
import com.wzz.gspt.enums.db.IndexType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_article_image")
@TableComment("文章图片墙关联表")
@Index(name = "idx_article_image_article_id", columns = {"article_id"}, comment = "文章ID索引")
@Index(name = "idx_article_image_file_id", columns = {"file_id"}, comment = "文件ID索引")
@Index(name = "uk_article_image_article_file", columns = {"article_id", "file_id"}, type = IndexType.UNIQUE, comment = "文章与文件唯一关联")
@ForeignKey(
        name = "fk_article_image_article_id",
        columns = {"article_id"},
        referenceEntity = Article.class,
        referencedColumns = {"id"},
        onDelete = ForeignKeyAction.CASCADE,
        onUpdate = ForeignKeyAction.RESTRICT
)
@ForeignKey(
        name = "fk_article_image_file_id",
        columns = {"file_id"},
        referenceEntity = FileRecord.class,
        referencedColumns = {"id"},
        onDelete = ForeignKeyAction.RESTRICT,
        onUpdate = ForeignKeyAction.RESTRICT
)
public class ArticleImage extends BaseEntity {

    /**
     * 文章ID
     */
    @TableField("article_id")
    @ColumnComment("文章ID")
    private Long articleId;

    /**
     * 文件记录ID
     */
    @TableField("file_id")
    @ColumnComment("文件记录ID")
    private Long fileId;

    /**
     * 图片访问地址
     */
    @TableField("image_url")
    @ColumnComment("图片访问地址")
    private String imageUrl;

    /**
     * 排序值
     */
    @TableField("sort_order")
    @ColumnComment("排序值")
    private Integer sortOrder;
}
