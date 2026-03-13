package com.wzz.gspt.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzz.gspt.annotation.db.*;
import com.wzz.gspt.common.BaseEntity;
import com.wzz.gspt.enums.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableComment("文章信息表")
@TableName("sys_article")
public class Article extends BaseEntity {

    /**
     * 文章标题
     */
    @TableField("title")
    @ColumnComment("文章标题")
    private String title;

    /**
     * 文章内容
     */
    @TableField("content")
    @ColumnComment("文章正文内容")
    @ColumnType("TEXT")
    private String content;

    /**
     * 封面图片文件ID(关联文件表)
     */
    @TableField("cover_file_id")
    @ColumnComment("封面图片文件ID(关联sys_file表ID)")
    private Long coverFileId;

    /**
     * 封面图片地址(冗余字段，提升查询性能)
     */
    @TableField("cover_image_url")
    @ColumnComment("封面图片完整访问地址")
    private String coverImageUrl;

    /**
     * 预览图地址(用于列表展示的缩略图等)
     */
    @TableField("preview_image_url")
    @ColumnComment("文章预览图/缩略图地址")
    private String previewImageUrl;

    /**
     * 作者ID(关联用户id)
     */
    @TableField("author_id")
    @ColumnComment("作者ID(关联sys_user表ID)")
    private Long authorId;

    /**
     * 作者名(关联用户名)
     */
    @TableField("author_name")
    @ColumnComment("作者name(关联sys_user表用户名)")
    private Long authorName;

    /**
     * 文章状态使用枚举类
     * 数据库存储：0/1
     * 前端交互：0/1
     */
    @TableField("status")
    @ColumnComment("状态：0-草稿, 1-已发布")
    @DefaultValue("1")
    private ArticleStatus status;
}