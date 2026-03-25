package com.wzz.gspt.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传配置类
 *
 * <p>用于统一读取 application.yml 中的 file.* 配置，
 * 包括本地存储路径、访问前缀、大小限制、允许的扩展名与内容类型。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /**
     * 上传文件的本地根目录
     */
    private String uploadPath = "./uploads/";

    /**
     * 文件静态访问路径映射
     */
    private String accessPath = "/uploads/**";

    /**
     * 单个文件允许上传的最大字节数
     */
    private DataSize maxSize = DataSize.ofMegabytes(500);

    /**
     * 允许上传的文件后缀白名单
     */
    private List<String> allowedExtensions = new ArrayList<>(List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".pdf", ".txt", ".doc", ".docx", ".xls", ".xlsx", ".zip"
    ));

    /**
     * 允许上传的文件内容类型白名单
     */
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip", "application/x-zip-compressed"
    ));
}
