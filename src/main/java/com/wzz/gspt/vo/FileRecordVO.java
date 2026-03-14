package com.wzz.gspt.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件上传结果返回对象
 *
 * <p>用于向前端返回文件记录、访问路径以及上传人信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileRecordVO {

    /**
     * 文件记录主键 ID
     */
    private Long id;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storageName;

    /**
     * 文件后缀名
     */
    private String fileSuffix;

    /**
     * 文件大小
     */
    private String fileSize;

    /**
     * 文件访问相对路径
     */
    private String fileUrl;

    /**
     * 文件本地绝对路径
     */
    private String filePath;

    /**
     * 文件可直接访问的完整地址
     */
    private String accessUrl;

    /**
     * 上传者用户 ID
     */
    private Long uploaderId;

    /**
     * 上传者用户名
     */
    private String uploaderName;

    /**
     * 文件记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
