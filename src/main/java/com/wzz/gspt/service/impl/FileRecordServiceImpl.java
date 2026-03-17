package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.config.properties.FileProperties;
import com.wzz.gspt.dto.file.AdminFileQueryRequest;
import com.wzz.gspt.dto.file.FileBatchDeleteRequest;
import com.wzz.gspt.dto.file.MyFileQueryRequest;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.enums.UserRole;
import com.wzz.gspt.exception.BusinessException;
import com.wzz.gspt.mapper.ArticleMapper;
import com.wzz.gspt.mapper.FileRecordMapper;
import com.wzz.gspt.mapper.UserMapper;
import com.wzz.gspt.pojo.Article;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.utils.DateTimeRangeUtil;
import com.wzz.gspt.utils.file.FileSecurityChecker;
import com.wzz.gspt.utils.file.LocalFileStorageUtil;
import com.wzz.gspt.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件记录服务实现类
 *
 * <p>负责串联文件安全检查、本地保存、分页查询、权限校验与删除清理逻辑。</p>
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
     * 文件上传配置
     */
    private final FileProperties fileProperties;

    /**
     * 文章数据访问层
     */
    private final ArticleMapper articleMapper;

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
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文件记录保存失败");
            }
            return buildFileRecordVO(fileRecord);
        } catch (RuntimeException e) {
            deleteStoredFileQuietly(storedFileInfo.localPath());
            throw e;
        }
    }

    /**
     * 管理员分页查询文件记录
     *
     * @param request 查询请求
     * @return 文件分页结果
     */
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

    /**
     * 普通用户或企业用户分页查询自己上传的文件记录
     *
     * @param request 查询请求
     * @return 文件分页结果
     */
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

    /**
     * 管理员删除单个文件记录
     *
     * @param fileId 文件记录 ID
     */
    @Override
    @Transactional
    public void deleteAdminFile(Long fileId) {
        ensureCurrentUserIsAdmin();
        deleteFileRecordById(fileId, false);
    }

    /**
     * 管理员批量删除文件记录
     *
     * @param request 批量删除请求
     */
    @Override
    @Transactional
    public void deleteAdminFiles(FileBatchDeleteRequest request) {
        ensureCurrentUserIsAdmin();
        List<Long> fileIds = validateBatchDeleteRequest(request);
        for (Long fileId : fileIds) {
            deleteFileRecordById(fileId, false);
        }
    }

    /**
     * 普通用户或企业用户删除自己上传的单个文件记录
     *
     * @param fileId 文件记录 ID
     */
    @Override
    @Transactional
    public void deleteMyFile(Long fileId) {
        ensureCurrentUserCanManageOwnFiles();
        deleteFileRecordById(fileId, true);
    }

    /**
     * 普通用户或企业用户批量删除自己上传的文件记录
     *
     * @param request 批量删除请求
     */
    @Override
    @Transactional
    public void deleteMyFiles(FileBatchDeleteRequest request) {
        ensureCurrentUserCanManageOwnFiles();
        List<Long> fileIds = validateBatchDeleteRequest(request);
        for (Long fileId : fileIds) {
            deleteFileRecordById(fileId, true);
        }
    }

    /**
     * 删除已经不存在业务引用的文件
     *
     * @param fileId 文件记录 ID
     */
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

    /**
     * 批量删除已经不存在业务引用的文件
     *
     * @param fileIds 文件记录 ID 列表
     */
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

    /**
     * 填充上传人信息
     *
     * @param fileRecord 文件记录对象
     */
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

    /**
     * 获取当前请求的客户端 IP 地址
     *
     * @return 客户端 IP
     */
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

    /**
     * 转换文件分页结果
     *
     * @param filePage 原始分页结果
     * @return 转换后的分页结果
     */
    private IPage<FileRecordVO> convertPage(Page<FileRecord> filePage) {
        Page<FileRecordVO> resultPage = new Page<>(filePage.getCurrent(), filePage.getSize(), filePage.getTotal());
        resultPage.setRecords(filePage.getRecords().stream().map(this::buildFileRecordVO).collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 构造文件记录返回对象
     *
     * @param fileRecord 文件记录对象
     * @return 返回对象
     */
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

    /**
     * 删除单个文件记录并清理本地文件
     *
     * @param fileId 文件记录 ID
     * @param onlyOwner 是否仅允许删除自己的文件
     */
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

    /**
     * 删除本地物理文件并清理空目录
     *
     * @param localPath 本地绝对路径
     */
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

    /**
     * 删除空的父级目录
     *
     * @param currentDir 当前目录
     */
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

    /**
     * 判断目录是否仍有其他文件或子目录
     *
     * @param directory 目标目录
     * @return 是否非空
     */
    private boolean directoryNotEmpty(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return false;
        }
        try (var pathStream = Files.list(directory)) {
            return pathStream.findAny().isPresent();
        }
    }

    /**
     * 校验管理员分页请求
     *
     * @param request 查询请求
     */
    private void validateAdminPageRequest(AdminFileQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "查询参数不能为空");
        }
        validatePage(request.getPageNum(), request.getPageSize());
    }

    /**
     * 校验我的文件分页请求
     *
     * @param request 查询请求
     */
    private void validateMyPageRequest(MyFileQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "查询参数不能为空");
        }
        validatePage(request.getPageNum(), request.getPageSize());
    }

    /**
     * 校验分页参数
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     */
    private void validatePage(Long pageNum, Long pageSize) {
        if (pageNum == null || pageNum < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "页码必须大于等于1");
        }
        if (pageSize == null || pageSize < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "每页条数必须大于等于1");
        }
    }

    /**
     * 校验批量删除请求
     *
     * @param request 批量删除请求
     * @return 有效的文件记录 ID 列表
     */
    private List<Long> validateBatchDeleteRequest(FileBatchDeleteRequest request) {
        if (request == null || request.getFileIds() == null || request.getFileIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "批量删除文件ID不能为空");
        }
        return request.getFileIds();
    }

    /**
     * 校验文件当前是否仍被业务数据引用
     *
     * @param fileId 文件记录 ID
     */
    private void ensureFileNotReferenced(Long fileId) {
        Article articleReference = findArticleReference(fileId);
        if (articleReference != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前文件<"+fileId+">已被文章<"+articleReference.getTitle()+">引用,不能直接删除");
        }

        User userReference = findUserReference(fileId);
        if (userReference != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前文件<"+fileId+">已被用户资料<"+userReference.getUsername()+">引用,不能直接删除");
        }
    }

    /**
     * 判断文件是否仍被业务数据引用
     *
     * @param fileId 文件记录 ID
     * @return 是否仍被引用
     */
    private boolean isFileReferenced(Long fileId) {
        return findArticleReference(fileId) != null || findUserReference(fileId) != null;
    }

    /**
     * 查询文件是否被文章引用
     *
     * @param fileId 文件记录 ID
     * @return 引用该文件的文章
     */
    private Article findArticleReference(Long fileId) {
        return articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .and(wrapper -> wrapper.eq(Article::getCoverFileId, fileId)
                        .or()
                        .eq(Article::getPreviewImageId, fileId))
                .last("limit 1"));
    }

    /**
     * 查询文件是否被用户资料引用
     *
     * @param fileId 文件记录 ID
     * @return 引用该文件的用户
     */
    private User findUserReference(Long fileId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getLicenseFileId, fileId)
                .last("limit 1"));
    }

    /**
     * 确保当前登录用户是管理员
     */
    private void ensureCurrentUserIsAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅管理员可执行该操作");
        }
    }

    /**
     * 确保当前登录用户是普通用户或企业用户
     *
     * @return 当前登录用户
     */
    private User ensureCurrentUserCanManageOwnFiles() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.USER && currentUser.getRole() != UserRole.ENTERPRISE) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅普通用户或企业用户可执行该操作");
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户
     */
    private User getCurrentUser() {
        User user = userMapper.selectById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }
        return user;
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 当前登录用户 ID
     */
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
