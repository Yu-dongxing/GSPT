package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.config.properties.FileProperties;
import com.wzz.gspt.dto.file.AdminFileQueryRequest;
import com.wzz.gspt.dto.file.FileBatchDeleteRequest;
import com.wzz.gspt.dto.file.FileCleanupRequest;
import com.wzz.gspt.dto.file.MyFileQueryRequest;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.enums.UserRole;
import com.wzz.gspt.exception.BusinessException;
import com.wzz.gspt.mapper.ArticleImageMapper;
import com.wzz.gspt.mapper.ArticleMapper;
import com.wzz.gspt.mapper.FileRecordMapper;
import com.wzz.gspt.mapper.UserMapper;
import com.wzz.gspt.pojo.Article;
import com.wzz.gspt.pojo.ArticleImage;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.utils.DateTimeRangeUtil;
import com.wzz.gspt.utils.file.FileSecurityChecker;
import com.wzz.gspt.utils.file.LocalFileStorageUtil;
import com.wzz.gspt.vo.FileCleanupResultVO;
import com.wzz.gspt.vo.FileRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord> implements FileRecordService {

    private final FileSecurityChecker fileSecurityChecker;

    private final LocalFileStorageUtil localFileStorageUtil;

    private final FileProperties fileProperties;

    private final ArticleMapper articleMapper;

    private final ArticleImageMapper articleImageMapper;

    private final UserMapper userMapper;

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
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文件记录保存失败");
            }
            return buildFileRecordVO(fileRecord);
        } catch (RuntimeException e) {
            deleteStoredFileQuietly(storedFileInfo.localPath());
            throw e;
        }
    }

    @Override
    public IPage<FileRecordVO> pageAdminFiles(AdminFileQueryRequest request) {
        ensureCurrentUserIsAdmin();
        validateAdminPageRequest(request);

        LocalDateTime startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        LocalDateTime endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);
        Page<FileRecord> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .like(StringUtils.hasText(request.getOriginalName()), FileRecord::getOriginalName, request.getOriginalName())
                .like(StringUtils.hasText(request.getStorageName()), FileRecord::getStorageName, request.getStorageName())
                .like(StringUtils.hasText(request.getFileSuffix()), FileRecord::getFileSuffix, request.getFileSuffix())
                .like(StringUtils.hasText(request.getUploaderName()), FileRecord::getUploaderName, request.getUploaderName())
                .ge(startCreateTime != null, FileRecord::getCreateTime, startCreateTime)
                .le(endCreateTime != null, FileRecord::getCreateTime, endCreateTime)
                .orderByDesc(FileRecord::getCreateTime);

        Page<FileRecord> filePage = page(page, wrapper);
        return convertPage(filePage);
    }

    @Override
    public IPage<FileRecordVO> pageMyFiles(MyFileQueryRequest request) {
        User currentUser = ensureCurrentUserCanManageOwnFiles();
        validateMyPageRequest(request);

        LocalDateTime startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        LocalDateTime endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);
        Page<FileRecord> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getUploaderId, currentUser.getId())
                .like(StringUtils.hasText(request.getOriginalName()), FileRecord::getOriginalName, request.getOriginalName())
                .like(StringUtils.hasText(request.getStorageName()), FileRecord::getStorageName, request.getStorageName())
                .like(StringUtils.hasText(request.getFileSuffix()), FileRecord::getFileSuffix, request.getFileSuffix())
                .ge(startCreateTime != null, FileRecord::getCreateTime, startCreateTime)
                .le(endCreateTime != null, FileRecord::getCreateTime, endCreateTime)
                .orderByDesc(FileRecord::getCreateTime);

        Page<FileRecord> filePage = page(page, wrapper);
        return convertPage(filePage);
    }

    @Override
    @Transactional
    public void deleteAdminFile(Long fileId) {
        ensureCurrentUserIsAdmin();
        deleteFileRecordById(fileId, false);
    }

    @Override
    @Transactional
    public void deleteAdminFiles(FileBatchDeleteRequest request) {
        ensureCurrentUserIsAdmin();
        List<Long> fileIds = validateBatchDeleteRequest(request);
        for (Long fileId : fileIds) {
            deleteFileRecordById(fileId, false);
        }
    }

    @Override
    @Transactional
    public void deleteMyFile(Long fileId) {
        ensureCurrentUserCanManageOwnFiles();
        deleteFileRecordById(fileId, true);
    }

    @Override
    @Transactional
    public void deleteMyFiles(FileBatchDeleteRequest request) {
        ensureCurrentUserCanManageOwnFiles();
        List<Long> fileIds = validateBatchDeleteRequest(request);
        for (Long fileId : fileIds) {
            deleteFileRecordById(fileId, true);
        }
    }

    @Override
    @Transactional
    public void deleteFileIfUnreferenced(Long fileId) {
        if (fileId == null) {
            return;
        }
        FileRecord fileRecord = getById(fileId);
        if (fileRecord == null || isFileReferenced(fileId)) {
            return;
        }
        deletePhysicalFile(fileRecord.getLocalPath());
        removeById(fileId);
    }

    @Override
    @Transactional
    public void deleteFilesIfUnreferenced(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            deleteFileIfUnreferenced(fileId);
        }
    }

    @Override
    public FileCleanupResultVO cleanupUnusedFiles(FileCleanupRequest request) {
        ensureCurrentUserIsAdmin();

        boolean dryRun = request != null && Boolean.TRUE.equals(request.getDryRun());
        List<FileRecord> allFiles = list();
        List<FileRecord> safeFiles = allFiles == null ? new ArrayList<>() : allFiles;
        Set<Long> referencedFileIds = collectReferencedFileIds();

        List<FileRecord> unusedFiles = safeFiles.stream()
                .filter(Objects::nonNull)
                .filter(fileRecord -> fileRecord.getId() != null)
                .filter(fileRecord -> !referencedFileIds.contains(fileRecord.getId()))
                .collect(Collectors.toList());

        FileCleanupResultVO.FileCleanupResultVOBuilder resultBuilder = FileCleanupResultVO.builder()
                .dryRun(dryRun)
                .totalScanned(safeFiles.size())
                .unusedCount(unusedFiles.size())
                .unusedFiles(unusedFiles.stream().map(this::buildFileRecordVO).collect(Collectors.toList()));

        if (dryRun) {
            return resultBuilder
                    .deletedCount(0)
                    .failedCount(0)
                    .deletedFileIds(new ArrayList<>())
                    .failedFiles(new ArrayList<>())
                    .build();
        }

        List<Long> deletedFileIds = new ArrayList<>();
        List<FileCleanupResultVO.FailedFileItem> failedFiles = new ArrayList<>();
        for (FileRecord unusedFile : unusedFiles) {
            try {
                deleteFileIfUnreferenced(unusedFile.getId());
                if (getById(unusedFile.getId()) == null) {
                    deletedFileIds.add(unusedFile.getId());
                } else {
                    failedFiles.add(buildFailedFileItem(unusedFile, "文件当前仍被引用，未执行删除"));
                }
            } catch (BusinessException ex) {
                failedFiles.add(buildFailedFileItem(unusedFile, ex.getMessage()));
            } catch (RuntimeException ex) {
                failedFiles.add(buildFailedFileItem(unusedFile, "删除失败: " + ex.getMessage()));
            }
        }

        return resultBuilder
                .deletedCount(deletedFileIds.size())
                .failedCount(failedFiles.size())
                .deletedFileIds(deletedFileIds)
                .failedFiles(failedFiles)
                .build();
    }

    private void fillUploaderInfo(FileRecord fileRecord) {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            fileRecord.setUploaderId(null);
            fileRecord.setUploaderName(resolveClientIp());
            return;
        }

        try {
            Long uploaderId = Long.parseLong(String.valueOf(loginId));
            fileRecord.setUploaderId(uploaderId);

            User uploader = userMapper.selectById(uploaderId);
            if (uploader != null) {
                fileRecord.setUploaderName(uploader.getUsername());
            } else {
                fileRecord.setUploaderName(resolveClientIp());
            }
        } catch (NumberFormatException ignored) {
            fileRecord.setUploaderId(null);
            fileRecord.setUploaderName(resolveClientIp());
        }
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "未知IP";
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr.trim() : "未知IP";
    }

    private IPage<FileRecordVO> convertPage(Page<FileRecord> filePage) {
        Page<FileRecordVO> resultPage = new Page<>(filePage.getCurrent(), filePage.getSize(), filePage.getTotal());
        resultPage.setRecords(filePage.getRecords().stream().map(this::buildFileRecordVO).collect(Collectors.toList()));
        return resultPage;
    }

    private FileRecordVO buildFileRecordVO(FileRecord fileRecord) {
        return FileRecordVO.builder()
                .id(fileRecord.getId())
                .originalName(fileRecord.getOriginalName())
                .storageName(fileRecord.getStorageName())
                .fileSuffix(fileRecord.getFileSuffix())
                .fileSize(fileRecord.getFileSize())
                .fileUrl(fileRecord.getUrlPath())
                .filePath(fileRecord.getLocalPath())
                .accessUrl(buildAccessUrl(fileRecord.getUrlPath()))
                .uploaderId(fileRecord.getUploaderId())
                .uploaderName(fileRecord.getUploaderName())
                .createTime(fileRecord.getCreateTime())
                .build();
    }

    private void deleteFileRecordById(Long fileId, boolean onlyOwner) {
        if (fileId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文件记录ID不能为空");
        }

        FileRecord fileRecord = getById(fileId);
        if (fileRecord == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文件记录不存在");
        }

        if (onlyOwner) {
            Long currentUserId = getCurrentUserId();
            if (!currentUserId.equals(fileRecord.getUploaderId())) {
                throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "只能删除自己上传的文件");
            }
        }
        ensureFileNotReferenced(fileId);

        deletePhysicalFile(fileRecord.getLocalPath());
        boolean removed = removeById(fileId);
        if (!removed) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "删除文件记录失败");
        }
    }

    private void deletePhysicalFile(String localPath) {
        if (!StringUtils.hasText(localPath)) {
            return;
        }

        Path filePath = Paths.get(localPath).toAbsolutePath().normalize();
        Path basePath = Paths.get(fileProperties.getUploadPath()).toAbsolutePath().normalize();
        if (!filePath.startsWith(basePath)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文件路径不合法，禁止删除");
        }
        try {
            Files.deleteIfExists(filePath);
            deleteEmptyParentDirectories(filePath.getParent());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "删除本地文件失败");
        }
    }

    private void deleteEmptyParentDirectories(Path currentDir) throws IOException {
        Path basePath = Paths.get(fileProperties.getUploadPath()).toAbsolutePath().normalize();
        while (currentDir != null && currentDir.startsWith(basePath) && !currentDir.equals(basePath)) {
            if (directoryNotEmpty(currentDir)) {
                break;
            }
            Files.deleteIfExists(currentDir);
            currentDir = currentDir.getParent();
        }
    }

    private boolean directoryNotEmpty(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return false;
        }
        try (var pathStream = Files.list(directory)) {
            return pathStream.findAny().isPresent();
        }
    }

    private void validateAdminPageRequest(AdminFileQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "查询参数不能为空");
        }
        validatePage(request.getPageNum(), request.getPageSize());
    }

    private void validateMyPageRequest(MyFileQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "查询参数不能为空");
        }
        validatePage(request.getPageNum(), request.getPageSize());
    }

    private void validatePage(Long pageNum, Long pageSize) {
        if (pageNum == null || pageNum < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "页码必须大于等于1");
        }
        if (pageSize == null || pageSize < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "每页条数必须大于等于1");
        }
    }

    private List<Long> validateBatchDeleteRequest(FileBatchDeleteRequest request) {
        if (request == null || request.getFileIds() == null || request.getFileIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "批量删除文件ID不能为空");
        }
        return request.getFileIds();
    }

    private void ensureFileNotReferenced(Long fileId) {
        Article articleReference = findArticleReference(fileId);
        if (articleReference != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前文件<" + fileId + ">已被文章<" + articleReference.getTitle() + ">引用，不能直接删除");
        }

        ArticleImage articleImageReference = findArticleImageReference(fileId);
        if (articleImageReference != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前文件<" + fileId + ">已被文章图片墙引用，不能直接删除");
        }

        User userReference = findUserReference(fileId);
        if (userReference != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前文件<" + fileId + ">已被用户资料<" + userReference.getUsername() + ">引用，不能直接删除");
        }
    }

    private boolean isFileReferenced(Long fileId) {
        return findArticleReference(fileId) != null
                || findArticleImageReference(fileId) != null
                || findUserReference(fileId) != null;
    }

    private Set<Long> collectReferencedFileIds() {
        Set<Long> referencedFileIds = new HashSet<>();

        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getCoverFileId, Article::getPreviewImageId));
        if (articles != null) {
            for (Article article : articles) {
                if (article == null) {
                    continue;
                }
                if (article.getCoverFileId() != null) {
                    referencedFileIds.add(article.getCoverFileId());
                }
                if (article.getPreviewImageId() != null) {
                    referencedFileIds.add(article.getPreviewImageId());
                }
            }
        }

        List<ArticleImage> articleImages = articleImageMapper.selectList(new LambdaQueryWrapper<ArticleImage>()
                .select(ArticleImage::getFileId));
        if (articleImages != null) {
            for (ArticleImage articleImage : articleImages) {
                if (articleImage != null && articleImage.getFileId() != null) {
                    referencedFileIds.add(articleImage.getFileId());
                }
            }
        }

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getLicenseFileId));
        if (users != null) {
            for (User user : users) {
                if (user == null) {
                    continue;
                }
                if (user.getLicenseFileId() != null) {
                    referencedFileIds.add(user.getLicenseFileId());
                }
            }
        }

        return referencedFileIds;
    }

    private Article findArticleReference(Long fileId) {
        return articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .and(wrapper -> wrapper.eq(Article::getCoverFileId, fileId)
                        .or()
                        .eq(Article::getPreviewImageId, fileId))
                .last("limit 1"));
    }

    private ArticleImage findArticleImageReference(Long fileId) {
        return articleImageMapper.selectOne(new LambdaQueryWrapper<ArticleImage>()
                .eq(ArticleImage::getFileId, fileId)
                .last("limit 1"));
    }

    private User findUserReference(Long fileId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getLicenseFileId, fileId)
                .last("limit 1"));
    }

    private FileCleanupResultVO.FailedFileItem buildFailedFileItem(FileRecord fileRecord, String reason) {
        return FileCleanupResultVO.FailedFileItem.builder()
                .fileId(fileRecord.getId())
                .originalName(fileRecord.getOriginalName())
                .reason(reason)
                .build();
    }

    private void ensureCurrentUserIsAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅管理员可执行该操作");
        }
    }

    private User ensureCurrentUserCanManageOwnFiles() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.USER && currentUser.getRole() != UserRole.ENTERPRISE) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅普通用户或企业用户可执行该操作");
        }
        return currentUser;
    }

    private User getCurrentUser() {
        User user = userMapper.selectById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }
        return user;
    }

    private Long getCurrentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGGED_IN.getCode(), "当前未登录");
        }
        try {
            return Long.parseLong(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "当前登录状态异常");
        }
    }

    private String buildAccessUrl(String urlPath) {
        if (!StringUtils.hasText(urlPath)) {
            return "";
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(urlPath)
                .toUriString();
    }

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
