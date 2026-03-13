package com.wzz.gspt.utils.file;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 文件上传辅助工具类
 *
 * <p>负责处理文件扩展名、访问路径等与上传流程相关的基础字符串操作。</p>
 */
public final class FileUploadHelper {

    /**
     * 私有构造方法，禁止外部实例化
     */
    private FileUploadHelper() {
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 标准化后的扩展名，格式为 .ext；若不存在则返回空字符串
     */
    public static String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        String extension = filename.substring(filename.lastIndexOf('.'));
        return normalizeExtension(extension);
    }

    /**
     * 标准化扩展名
     *
     * @param extension 原始扩展名
     * @return 小写且以 . 开头的扩展名
     */
    public static String normalizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }

    /**
     * 标准化访问路径前缀
     *
     * @param accessPath 配置中的访问路径
     * @return 适合拼接文件 URL 的访问前缀
     */
    public static String normalizeAccessPath(String accessPath) {
        if (!StringUtils.hasText(accessPath)) {
            return "";
        }
        String normalized = accessPath.trim().replace("\\", "/");
        if (normalized.endsWith("/**")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
