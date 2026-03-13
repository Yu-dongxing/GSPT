package com.wzz.gspt.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzz.gspt.annotation.db.ColumnComment;
import com.wzz.gspt.annotation.db.TableComment;
import com.wzz.gspt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传记录实体
 *
 * <p>用于持久化上传文件的基础信息、路径信息与上传人信息。</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableComment("文件上传记录表")
@TableName("sys_file")
public class FileRecord extends BaseEntity {

    /**
     * 原始文件名
     */
    @TableField("original_name")
    @ColumnComment("原始文件名")
    private String originalName;

    /**
     * 存储文件名
     */
    @TableField("storage_name")
    @ColumnComment("在磁盘上存储的唯一文件名")
    private String storageName;

    /**
     * 文件后缀名
     */
    @TableField("file_suffix")
    @ColumnComment("文件后缀名")
    private String fileSuffix;

    /**
     * 文件大小，单位为字节
     */
    @TableField("file_size")
    @ColumnComment("文件大小(字节)")
    private String fileSize;

    /**
     * 本地绝对路径
     */
    @TableField("local_path")
    @ColumnComment("服务器本地磁盘绝对路径")
    private String localPath;

    /**
     * 相对访问路径
     */
    @TableField("url_path")
    @ColumnComment("前端可访问的相对或绝对URL路径")
    private String urlPath;

    /**
     * 上传者用户 ID
     */
    @TableField("uploader_id")
    @ColumnComment("上传者ID(关联sys_user表ID)")
    private Long uploaderId;

    /**
     * 上传者用户名
     */
    @TableField("uploader_name")
    @ColumnComment("上传者用户名")
    private String uploaderName;
}
