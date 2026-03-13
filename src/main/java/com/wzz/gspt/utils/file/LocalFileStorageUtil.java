package com.wzz.gspt.utils.file;

import cn.hutool.core.util.IdUtil;
import com.wzz.gspt.config.properties.FileProperties;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 本地文件存储工具类
 *
 * <p>负责将已通过安全校验的文件保存到本地磁盘，
 * 并返回文件保存后的关键路径信息。</p>
 */
@Component
@RequiredArgsConstructor
public class LocalFileStorageUtil {

    /**
     * 统一使用上海时区生成日期目录
     */
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 文件上传配置
     */
    private final FileProperties fileProperties;

    /**
     * 保存上传文件到本地磁盘
     *
     * @param file 上传文件
     * @return 保存后的文件信息
     */
    public StoredFileInfo save(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = FileUploadHelper.getExtension(originalFilename);
        String storageName = IdUtil.fastSimpleUUID() + extension;

        LocalDate today = LocalDate.now(ZONE_ID);
        String relativeDir = today.getYear() + "/" + pad(today.getMonthValue()) + "/" + pad(today.getDayOfMonth());
        Path basePath = Paths.get(fileProperties.getUploadPath()).toAbsolutePath().normalize();
        Path targetDir = basePath.resolve(relativeDir).normalize();
        Path targetFile = targetDir.resolve(storageName).normalize();

        if (!targetFile.startsWith(basePath)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "Invalid target file path");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
            if (!Files.exists(targetFile)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "File save failed");
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "File save failed");
        }

        String urlPath = FileUploadHelper.normalizeAccessPath(fileProperties.getAccessPath())
                + "/" + relativeDir.replace("\\", "/")
                + "/" + storageName;
        return new StoredFileInfo(storageName, extension, targetFile.toString(), urlPath);
    }

    /**
     * 将日期中的月或日补齐为两位数字
     *
     * @param value 月份或日期
     * @return 补零后的字符串
     */
    private String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    /**
     * 已保存文件的信息载体
     *
     * @param storageName 存储文件名
     * @param fileSuffix 文件后缀
     * @param localPath 本地绝对路径
     * @param urlPath 相对访问路径
     */
    public record StoredFileInfo(String storageName, String fileSuffix, String localPath, String urlPath) {
    }
}
