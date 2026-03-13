package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.exception.BusinessException;
import com.wzz.gspt.mapper.FileRecordMapper;
import com.wzz.gspt.mapper.UserMapper;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.utils.file.FileSecurityChecker;
import com.wzz.gspt.utils.file.LocalFileStorageUtil;
import com.wzz.gspt.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件记录服务实现类
 *
 * <p>负责串联文件安全检查、本地保存、上传人信息写入、数据库落库与结果返回。</p>
 */
@Service
@RequiredArgsConstructor
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord> implements FileRecordService {

    /**
     * 文件安全检查器
     */
    private final FileSecurityChecker fileSecurityChecker;

    /**
     * 本地文件存储工具
     */
    private final LocalFileStorageUtil localFileStorageUtil;

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * 执行文件上传业务
     *
     * @param file 上传文件
     * @return 文件上传成功后的记录对象
     */
    @Override
    @Transactional
    public FileRecordVO upload(MultipartFile file) {
        fileSecurityChecker.check(file);
        LocalFileStorageUtil.StoredFileInfo storedFileInfo = localFileStorageUtil.save(file);

        try {
            FileRecord fileRecord = new FileRecord();
            fileRecord.setOriginalName(file.getOriginalFilename());
            fileRecord.setStorageName(storedFileInfo.storageName());
            fileRecord.setFileSuffix(storedFileInfo.fileSuffix());
            fileRecord.setFileSize(String.valueOf(file.getSize()));
            fileRecord.setLocalPath(storedFileInfo.localPath());
            fileRecord.setUrlPath(storedFileInfo.urlPath());

            fillUploaderInfo(fileRecord);

            boolean saved = save(fileRecord);
            if (!saved) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "File record save failed");
            }

            String accessUrl = buildAccessUrl(fileRecord.getUrlPath());
            return FileRecordVO.builder()
                    .id(fileRecord.getId())
                    .originalName(fileRecord.getOriginalName())
                    .storageName(fileRecord.getStorageName())
                    .fileSuffix(fileRecord.getFileSuffix())
                    .fileSize(fileRecord.getFileSize())
                    .fileUrl(fileRecord.getUrlPath())
                    .filePath(fileRecord.getLocalPath())
                    .accessUrl(accessUrl)
                    .uploaderId(fileRecord.getUploaderId())
                    .uploaderName(fileRecord.getUploaderName())
                    .createTime(fileRecord.getCreateTime())
                    .build();
        } catch (RuntimeException e) {
            deleteStoredFileQuietly(storedFileInfo.localPath());
            throw e;
        }
    }

    /**
     * 填充上传人信息
     *
     * @param fileRecord 文件记录对象
     */
    private void fillUploaderInfo(FileRecord fileRecord) {
        Object loginId = StpUtil.getLoginId();
        if (loginId == null) {
            return;
        }

        try {
            Long uploaderId = Long.parseLong(String.valueOf(loginId));
            fileRecord.setUploaderId(uploaderId);

            User uploader = userMapper.selectById(uploaderId);
            if (uploader != null) {
                fileRecord.setUploaderName(uploader.getUsername());
            }
        } catch (NumberFormatException ignored) {
            fileRecord.setUploaderId(null);
            fileRecord.setUploaderName(null);
        }
    }

    /**
     * 生成文件可直接访问的完整地址
     *
     * @param urlPath 相对访问路径
     * @return 完整访问地址
     */
    private String buildAccessUrl(String urlPath) {
        if (!StringUtils.hasText(urlPath)) {
            return "";
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(urlPath)
                .toUriString();
    }

    /**
     * 在数据库保存失败时删除本地已落盘文件
     *
     * @param localPath 本地文件绝对路径
     */
    private void deleteStoredFileQuietly(String localPath) {
        if (!StringUtils.hasText(localPath)) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(localPath));
        } catch (IOException ignored) {
        }
    }
}
