package com.wzz.gspt.utils.file;

import com.wzz.gspt.config.properties.FileProperties;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件安全检查器
 *
 * <p>负责对上传文件进行基础安全校验，包括：
 * 文件是否为空、文件大小、文件名合法性、扩展名白名单、
 * 内容类型白名单以及文件头魔数校验。</p>
 */
@Component
@RequiredArgsConstructor
public class FileSecurityChecker {

    /**
     * 常见文件扩展名与魔数映射表
     */
    private static final Map<String, List<byte[]>> MAGIC_NUMBER_MAP = new HashMap<>();

    static {
        MAGIC_NUMBER_MAP.put(".jpg", List.of(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        ));
        MAGIC_NUMBER_MAP.put(".jpeg", MAGIC_NUMBER_MAP.get(".jpg"));
        MAGIC_NUMBER_MAP.put(".png", List.of(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        ));
        MAGIC_NUMBER_MAP.put(".gif", List.of(
                new byte[]{0x47, 0x49, 0x46, 0x38}
        ));
        MAGIC_NUMBER_MAP.put(".webp", List.of(
                new byte[]{0x52, 0x49, 0x46, 0x46}
        ));
        MAGIC_NUMBER_MAP.put(".pdf", List.of(
                new byte[]{0x25, 0x50, 0x44, 0x46}
        ));
        MAGIC_NUMBER_MAP.put(".zip", List.of(
                new byte[]{0x50, 0x4B, 0x03, 0x04},
                new byte[]{0x50, 0x4B, 0x05, 0x06},
                new byte[]{0x50, 0x4B, 0x07, 0x08}
        ));
        MAGIC_NUMBER_MAP.put(".docx", MAGIC_NUMBER_MAP.get(".zip"));
        MAGIC_NUMBER_MAP.put(".xlsx", MAGIC_NUMBER_MAP.get(".zip"));
    }

    /**
     * 文件上传配置
     */
    private final FileProperties fileProperties;

    /**
     * 执行文件安全检查
     *
     * @param file 上传文件
     */
    public void check(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Uploaded file must not be empty");
        }
        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Uploaded file exceeds the size limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Filename must not be empty");
        }

        String cleanedFilename = StringUtils.cleanPath(originalFilename);
        if (cleanedFilename.contains("..")) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Filename is invalid");
        }

        String extension = FileUploadHelper.getExtension(cleanedFilename);
        Set<String> allowedExtensions = fileProperties.getAllowedExtensions().stream()
                .map(FileUploadHelper::normalizeExtension)
                .collect(Collectors.toSet());
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Unsupported file extension: " + extension);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Unable to detect content type");
        }
        Set<String> allowedContentTypes = fileProperties.getAllowedContentTypes().stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Unsupported content type: " + contentType);
        }

        verifyMagicNumberIfNecessary(file, extension);
    }

    /**
     * 按需执行文件头魔数校验
     *
     * @param file 上传文件
     * @param extension 文件扩展名
     */
    private void verifyMagicNumberIfNecessary(MultipartFile file, String extension) {
        List<byte[]> magicNumbers = MAGIC_NUMBER_MAP.get(extension);
        if (magicNumbers == null || magicNumbers.isEmpty()) {
            return;
        }

        byte[] header = new byte[16];
        try (InputStream inputStream = file.getInputStream()) {
            int read = inputStream.read(header);
            if (read <= 0) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "Unable to read uploaded file");
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "File security check failed");
        }

        boolean matched = magicNumbers.stream().anyMatch(magic -> startsWith(header, magic));
        if (!matched) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "File content does not match the extension");
        }
    }

    /**
     * 判断字节数组是否以前缀字节开始
     *
     * @param source 原始字节数组
     * @param prefix 前缀字节数组
     * @return 是否匹配
     */
    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
