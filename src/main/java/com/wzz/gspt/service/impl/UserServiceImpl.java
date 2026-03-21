package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.dto.user.*;
import com.wzz.gspt.enums.ArticleStatus;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.enums.UserAuditStatus;
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
import com.wzz.gspt.service.UserService;
import com.wzz.gspt.utils.DateTimeRangeUtil;
import com.wzz.gspt.vo.ArticleImageVO;
import com.wzz.gspt.vo.ArticleVO;
import com.wzz.gspt.vo.UserAdminVO;
import com.wzz.gspt.vo.UserLoginVO;
import com.wzz.gspt.vo.UserProfileVO;
import com.wzz.gspt.vo.UserRegisterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final FileRecordMapper fileRecordMapper;
    private final ArticleMapper articleMapper;
    private final ArticleImageMapper articleImageMapper;
    private final FileRecordService fileRecordService;

    /**
     * 普通用户注册（仅需手机号和密码）
     * @param request 注册参数
     * @return 注册结果VO
     */
    @Override
    @Transactional
    public UserRegisterVO registerNormal(UserNormalRegisterRequest request) {
        validateNormalRequest(request);
        // 手机号作为用户名，必须唯一
        if (getByUsername(request.getPhone()) != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "该手机号已注册");
        }

        User user = new User();
        user.setUsername(request.getPhone().trim());
        user.setPassword(request.getPassword().trim());
        user.setRole(UserRole.USER);
        user.setAuditStatus(UserAuditStatus.NONE);

        if (!save(user)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "普通用户注册失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 企业用户注册（需营业执照等信息，进入审核流程）
     * @param request 企业注册参数
     * @return 注册结果VO
     */
    @Override
    @Transactional
    public UserRegisterVO registerEnterprise(UserEnterpriseRegisterRequest request) {
        validateEnterpriseRequest(request);
        // 校验营业执照文件合法性
        FileRecord licenseFile = validateLicenseFile(request.getLicenseFileId(), request.getLicenseUrl());

        User usernameUser = getByUsername(request.getPhone());
        User emailUser = getByEmail(request.getEmail());

        // 解决潜在冲突：如果账号已存在且被拒绝，则允许重新提交更新
        User targetUser = resolveEnterpriseTarget(usernameUser, emailUser);

        if (targetUser == null) {
            targetUser = new User();
            fillEnterpriseUser(targetUser, request, licenseFile);
            if (!save(targetUser)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业用户注册失败");
            }
        } else {
            // 已存在且状态为被拒绝，更新信息重新进入审核
            fillEnterpriseUser(targetUser, request, licenseFile);
            if (!updateById(targetUser)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业用户重新提交失败");
            }
        }
        return buildRegisterVO(targetUser);
    }

    /**
     * 用户登录（通用）
     * @param request 登录参数
     * @return 登录成功后的Token及用户信息
     */
    @Override
    public UserLoginVO login(UserLoginRequest request) {
        User user = validateLoginCredentials(request);
        // 校验企业用户审核状态，未通过不可登录
        validateLoginStatus(user);
        return doLogin(user);
    }

    /**
     * 管理员登录
     * @param request 登录参数
     * @return 登录结果
     */
    @Override
    public UserLoginVO adminLogin(UserLoginRequest request) {
        User user = validateLoginCredentials(request);
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "当前账号不是管理员");
        }
        return doLogin(user);
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户的基础信息
     */
    @Override
    public UserRegisterVO getCurrentUser() {
        return buildRegisterVO(requireUser(getCurrentLoginUserId()));
    }

    /**
     * 获取指定用户的公开画像信息
     * @param userId 用户ID
     */
    @Override
    public UserProfileVO getUserProfile(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户ID不能为空");
        }
        return buildUserProfileVO(requireUser(userId));
    }

    /**
     * 分页查询特定用户的文章
     * 逻辑：作者本人或管理员可查所有状态；访客仅能查看已发布的。
     */
    @Override
    public IPage<ArticleVO> pageUserArticles(UserArticleQueryRequest request) {
        validateUserArticleQueryRequest(request);
        User targetUser = requireUser(request.getUserId());
        Long currentUserId = getCurrentLoginUserIdIfPresent();

        // 判断权限：是否为本人或管理员
        boolean canViewAll = canViewAllUserArticles(currentUserId, targetUser.getId());

        // 如果非本人且试图查询非发布状态，拦截
        if (!canViewAll && request.getStatus() != null && request.getStatus() != ArticleStatus.PUBLISHED) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅支持查看已发布文章");
        }

        LocalDateTime startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        LocalDateTime endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);

        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, request.getUserId())
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                // 权限过滤逻辑
                .eq(canViewAll && request.getStatus() != null, Article::getStatus, request.getStatus())
                .eq(!canViewAll, Article::getStatus, ArticleStatus.PUBLISHED)
                .ge(startCreateTime != null, Article::getCreateTime, startCreateTime)
                .le(endCreateTime != null, Article::getCreateTime, endCreateTime)
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = articleMapper.selectPage(page, wrapper);
        Page<ArticleVO> result = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        result.setRecords(articlePage.getRecords().stream().map(this::buildArticleVO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 更新当前登录用户的个人信息
     */
    @Override
    @Transactional
    public UserRegisterVO updateProfile(UserProfileUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "修改个人信息参数不能为空");
        }
        User user = requireUser(getCurrentLoginUserId());

        // 更新昵称
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        // 更新邮箱并校验唯一性
        if (StringUtils.hasText(request.getEmail())) {
            User existedEmailUser = getByEmail(request.getEmail());
            if (existedEmailUser != null && !Objects.equals(existedEmailUser.getId(), user.getId())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "邮箱已存在");
            }
            user.setEmail(request.getEmail().trim());
        }

        // 企业用户额外信息处理
        if (user.getRole() == UserRole.ENTERPRISE) {
            applyEnterpriseProfile(user, request);
        } else if (StringUtils.hasText(request.getCompanyName())) {
            user.setCompanyName(request.getCompanyName().trim());
        }

        if (!updateById(user)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改个人信息失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional
    public void changePassword(UserPasswordChangeRequest request) {
        if (request == null || !StringUtils.hasText(request.getOldPassword()) || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "修改密码参数不完整");
        }
        User user = requireUser(getCurrentLoginUserId());
        // 校验原密码
        if (!request.getOldPassword().trim().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "原密码错误");
        }
        user.setPassword(request.getNewPassword().trim());
        if (!updateById(user)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改密码失败");
        }
    }

    /**
     * 管理员审核企业账号
     */
    @Override
    @Transactional
    public UserRegisterVO auditEnterprise(EnterpriseAuditRequest request) {
        validateAuditRequest(request);
        ensureCurrentUserIsAdmin();

        User user = requireUser(request.getUserId());
        if (user.getRole() != UserRole.ENTERPRISE) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "该用户不是企业用户");
        }
        if (user.getAuditStatus() != UserAuditStatus.PENDING) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前用户不处于待审核状态");
        }

        user.setAuditStatus(request.getAuditStatus());
        user.setAuditRemark(StringUtils.hasText(request.getAuditRemark()) ? request.getAuditRemark().trim() : null);

        if (!updateById(user)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业审核失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理后台：分页查询所有用户
     */
    @Override
    public IPage<UserAdminVO> pageUsers(UserAdminQueryRequest request) {
        ensureCurrentUserIsAdmin();
        validatePageRequest(request);

        LocalDateTime startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        LocalDateTime endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);

        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(request.getUsername()), User::getUsername, request.getUsername())
                .like(StringUtils.hasText(request.getEmail()), User::getEmail, request.getEmail())
                .like(StringUtils.hasText(request.getCompanyName()), User::getCompanyName, request.getCompanyName())
                .like(StringUtils.hasText(request.getLicenseName()), User::getLicenseName, request.getLicenseName())
                .eq(request.getRole() != null, User::getRole, request.getRole())
                .eq(request.getAuditStatus() != null, User::getAuditStatus, request.getAuditStatus())
                .ge(startCreateTime != null, User::getCreateTime, startCreateTime)
                .le(endCreateTime != null, User::getCreateTime, endCreateTime)
                .orderByDesc(User::getCreateTime);

        Page<User> userPage = page(page, wrapper);
        Page<UserAdminVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream().map(this::buildAdminVO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 管理后台：查看用户详情
     */
    @Override
    public UserRegisterVO getUserDetail(Long userId) {
        ensureCurrentUserIsAdmin();
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户ID不能为空");
        }
        return buildRegisterVO(requireUser(userId));
    }

    /**
     * 管理后台：新增用户
     */
    @Override
    @Transactional
    public UserRegisterVO createUser(UserAdminSaveRequest request) {
        ensureCurrentUserIsAdmin();
        validateAdminSaveRequest(request, false);

        if (getByUsername(request.getUsername()) != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户名已存在");
        }
        if (StringUtils.hasText(request.getEmail()) && getByEmail(request.getEmail()) != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "邮箱已存在");
        }

        FileRecord licenseFile = resolveAdminLicenseFile(request);
        User user = new User();
        applyAdminSaveRequest(user, request, licenseFile);

        if (!save(user)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "新增用户失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理后台：修改用户信息
     */
    @Override
    @Transactional
    public UserRegisterVO updateUser(UserAdminSaveRequest request) {
        ensureCurrentUserIsAdmin();
        validateAdminSaveRequest(request, true);

        User user = requireUser(request.getId());
        User existedUser = getByUsername(request.getUsername());
        if (existedUser != null && !Objects.equals(existedUser.getId(), user.getId())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户名已存在");
        }
        if (StringUtils.hasText(request.getEmail())) {
            User existedEmailUser = getByEmail(request.getEmail());
            if (existedEmailUser != null && !Objects.equals(existedEmailUser.getId(), user.getId())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "邮箱已存在");
            }
        }

        FileRecord licenseFile = resolveAdminLicenseFile(request);
        applyAdminSaveRequest(user, request, licenseFile);

        if (!updateById(user)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改用户失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理后台：删除用户（级联删除文章、图片墙记录、文件引用）
     */
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        ensureCurrentUserIsAdmin();
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户ID不能为空");
        }
        // 禁止自杀操作
        if (Objects.equals(getCurrentLoginUserId(), userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "不允许删除当前登录管理员");
        }

        User user = requireUser(userId);
        // 1. 清理用户发布的文章及其关联数据
        deleteUserRelations(user);
        // 2. 执行逻辑/物理删除
        if (!removeById(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "删除用户失败");
        }
        // 3. 清理由该用户上传或持有的文件引用
        deleteUserFiles(user);
    }

    /**
     * 管理后台：批量删除用户
     */
    @Override
    @Transactional
    public void deleteUsers(UserBatchDeleteRequest request) {
        ensureCurrentUserIsAdmin();
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "批量删除用户ID不能为空");
        }
        if (request.getUserIds().contains(getCurrentLoginUserId())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "不允许批量删除当前登录管理员");
        }
        for (Long userId : request.getUserIds()) {
            deleteUser(userId); // 复用单删逻辑
        }
    }

    // --- 私有辅助校验方法 ---

    private void validateNormalRequest(UserNormalRegisterRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "注册参数不完整");
        }
    }

    private void validateEnterpriseRequest(UserEnterpriseRegisterRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getEmail())
                || !StringUtils.hasText(request.getPassword()) || !StringUtils.hasText(request.getLicenseName())
                || request.getLicenseFileId() == null || !StringUtils.hasText(request.getLicenseUrl())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "企业注册参数不完整");
        }
    }

    private void validateUserArticleQueryRequest(UserArticleQueryRequest request) {
        if (request == null || request.getUserId() == null || request.getPageNum() == null || request.getPageNum() < 1
                || request.getPageSize() == null || request.getPageSize() < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "查询参数不合法");
        }
    }

    /**
     * 校验登录名和密码
     */
    private User validateLoginCredentials(UserLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "登录参数不完整");
        }
        User user = getByUsername(request.getUsername());
        if (user == null || !request.getPassword().trim().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "账号或密码错误");
        }
        return user;
    }

    /**
     * 执行 Sa-Token 登录逻辑
     */
    private UserLoginVO doLogin(User user) {
        StpUtil.login(user.getId());
        return UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .auditStatus(user.getAuditStatus())
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .build();
    }

    private void validateAuditRequest(EnterpriseAuditRequest request) {
        if (request == null || request.getUserId() == null || request.getAuditStatus() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "审核参数不完整");
        }
        if (request.getAuditStatus() != UserAuditStatus.APPROVED && request.getAuditStatus() != UserAuditStatus.REJECTED) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "审核状态仅支持通过或拒绝");
        }
        if (request.getAuditStatus() == UserAuditStatus.REJECTED && !StringUtils.hasText(request.getAuditRemark())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "拒绝审核时必须填写原因");
        }
    }

    private void validatePageRequest(UserAdminQueryRequest request) {
        if (request == null || request.getPageNum() == null || request.getPageNum() < 1
                || request.getPageSize() == null || request.getPageSize() < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "分页查询参数不合法");
        }
    }

    /**
     * 管理员新增/修改用户时的参数校验
     */
    private void validateAdminSaveRequest(UserAdminSaveRequest request, boolean isUpdate) {
        if (request == null || (isUpdate && request.getId() == null) || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword()) || request.getRole() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户参数不完整");
        }
        // 如果是创建企业用户，必须提供营业执照信息
        if (request.getRole() == UserRole.ENTERPRISE && (!StringUtils.hasText(request.getEmail())
                || !StringUtils.hasText(request.getLicenseName()) || request.getLicenseFileId() == null
                || !StringUtils.hasText(request.getLicenseUrl()))) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "企业用户参数不完整");
        }
    }

    /**
     * 校验营业执照文件记录
     */
    private FileRecord validateLicenseFile(Long licenseFileId, String licenseUrl) {
        FileRecord fileRecord = fileRecordMapper.selectById(licenseFileId);
        if (fileRecord == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "营业执照文件不存在");
        }
        String trimmedUrl = licenseUrl.trim();
        if (!trimmedUrl.equals(fileRecord.getUrlPath()) && !trimmedUrl.endsWith(fileRecord.getUrlPath())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "营业执照文件路径不匹配");
        }
        return fileRecord;
    }

    private User getByUsername(String username) {
        return StringUtils.hasText(username)
                ? getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).last("limit 1"))
                : null;
    }

    private User getByEmail(String email) {
        return StringUtils.hasText(email)
                ? getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email.trim()).last("limit 1"))
                : null;
    }

    /**
     * 处理企业注册时的账号冲突与重试逻辑
     */
    private User resolveEnterpriseTarget(User usernameUser, User emailUser) {
        if (usernameUser == null && emailUser == null) {
            return null;
        }
        // 手机号和邮箱被不同人占用了
        if (usernameUser != null && emailUser != null && !usernameUser.getId().equals(emailUser.getId())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "手机号或邮箱已被其他账号占用，不可重复提交");
        }
        User existedUser = usernameUser != null ? usernameUser : emailUser;
        // 只有在之前被拒绝的情况下，才允许再次“提交注册”（实际上是更新）
        if (existedUser.getAuditStatus() == UserAuditStatus.REJECTED) {
            return existedUser;
        }
        throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "不可重复提交");
    }

    /**
     * 填充企业注册信息
     */
    private void fillEnterpriseUser(User user, UserEnterpriseRegisterRequest request, FileRecord licenseFile) {
        user.setUsername(request.getPhone().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(request.getPassword().trim());
        user.setLicenseName(request.getLicenseName().trim());
        user.setCompanyName(request.getLicenseName().trim()); // 默认公司名为执照名
        user.setLicenseFileId(licenseFile.getId());
        user.setLicenseUrl(request.getLicenseUrl().trim());
        user.setRole(UserRole.ENTERPRISE);
        user.setAuditStatus(UserAuditStatus.PENDING);
        user.setAuditRemark("企业注册已提交，等待审核");
    }

    /**
     * 用户自行更新资料时的企业信息逻辑处理
     */
    private void applyEnterpriseProfile(User user, UserProfileUpdateRequest request) {
        if (StringUtils.hasText(request.getLicenseName())) {
            user.setLicenseName(request.getLicenseName().trim());
        }
        if (StringUtils.hasText(request.getCompanyName())) {
            user.setCompanyName(request.getCompanyName().trim());
        } else if (StringUtils.hasText(request.getLicenseName())) {
            user.setCompanyName(request.getLicenseName().trim());
        }

        if (request.getLicenseFileId() != null || StringUtils.hasText(request.getLicenseUrl())) {
            if (request.getLicenseFileId() == null || !StringUtils.hasText(request.getLicenseUrl())) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "营业执照文件ID和访问路径必须同时提供");
            }
            FileRecord licenseFile = validateLicenseFile(request.getLicenseFileId(), request.getLicenseUrl());
            user.setLicenseFileId(licenseFile.getId());
            user.setLicenseUrl(request.getLicenseUrl().trim());
        }
    }

    /**
     * 管理员保存用户时解析营业执照
     */
    private FileRecord resolveAdminLicenseFile(UserAdminSaveRequest request) {
        return request.getRole() == UserRole.ENTERPRISE
                ? validateLicenseFile(request.getLicenseFileId(), request.getLicenseUrl())
                : null;
    }

    /**
     * 管理员保存用户时的字段应用映射逻辑
     */
    private void applyAdminSaveRequest(User user, UserAdminSaveRequest request, FileRecord licenseFile) {
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword().trim());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : null);
        user.setRole(request.getRole());
        user.setAuditRemark(StringUtils.hasText(request.getAuditRemark()) ? request.getAuditRemark().trim() : null);

        if (request.getRole() == UserRole.ENTERPRISE) {
            user.setEmail(request.getEmail().trim());
            user.setLicenseName(request.getLicenseName().trim());
            user.setCompanyName(StringUtils.hasText(request.getCompanyName()) ? request.getCompanyName().trim() : request.getLicenseName().trim());
            user.setLicenseFileId(licenseFile.getId());
            user.setLicenseUrl(request.getLicenseUrl().trim());
            user.setAuditStatus(request.getAuditStatus() != null ? request.getAuditStatus() : UserAuditStatus.PENDING);
        } else {
            // 普通用户或管理员清空企业特有字段
            user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
            user.setCompanyName(StringUtils.hasText(request.getCompanyName()) ? request.getCompanyName().trim() : null);
            user.setLicenseName(null);
            user.setLicenseFileId(null);
            user.setLicenseUrl(null);
            user.setAuditStatus(request.getAuditStatus() != null ? request.getAuditStatus() : UserAuditStatus.NONE);
        }
    }

    /**
     * 校验企业用户是否具备登录条件
     */
    private void validateLoginStatus(User user) {
        if (user.getRole() != UserRole.ENTERPRISE) {
            return;
        }
        if (user.getAuditStatus() == UserAuditStatus.PENDING) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业账号正在审核中，暂不可登录");
        }
        if (user.getAuditStatus() == UserAuditStatus.REJECTED) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业账号审核未通过，请修改后重新提交");
        }
        if (user.getAuditStatus() != UserAuditStatus.APPROVED) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业账号状态异常，暂不可登录");
        }
    }

    /**
     * 获取当前登录ID，若未登录抛异常
     */
    private Long getCurrentLoginUserId() {
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
     * 获取当前登录ID（静默模式，未登录返回null）
     */
    private Long getCurrentLoginUserIdIfPresent() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 判断当前操作者是否有权查看目标用户的全部文章（私有+草稿等）
     */
    private boolean canViewAllUserArticles(Long currentUserId, Long targetUserId) {
        if (currentUserId == null) {
            return false;
        }
        if (Objects.equals(currentUserId, targetUserId)) {
            return true;
        }
        User currentUser = getById(currentUserId);
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    /**
     * 强效检查当前用户是否为管理员
     */
    private void ensureCurrentUserIsAdmin() {
        User currentUser = requireUser(getCurrentLoginUserId());
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅后台管理员可执行该操作");
        }
    }

    /**
     * 根据ID获取用户，不存在则抛错
     */
    private User requireUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户不存在");
        }
        return user;
    }

    /**
     * 级联处理用户关联的文章及文章图片墙
     */
    private void deleteUserRelations(User user) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getAuthorId, user.getId()));
        if (articles == null || articles.isEmpty()) {
            return;
        }
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        if (!articleIds.isEmpty()) {
            articleMapper.deleteByIds(articleIds);
        }

        List<Long> fileIds = new ArrayList<>();
        for (Article article : articles) {
            // 收集文章封面、预览图
            fileIds.addAll(buildArticleFileIds(article));
            // 收集文章图片墙文件
            fileIds.addAll(buildArticleImageWallFileIds(article.getId()));
            // 删除图片墙关联记录
            articleImageMapper.delete(new LambdaQueryWrapper<ArticleImage>().eq(ArticleImage::getArticleId, article.getId()));
        }
        // 调用文件服务尝试清理物理文件
        fileRecordService.deleteFilesIfUnreferenced(fileIds);
    }

    /**
     * 清理用户持有的相关文件记录
     */
    private void deleteUserFiles(User user) {
        List<Long> fileIds = new ArrayList<>();
        if (user.getLicenseFileId() != null) {
            fileIds.add(user.getLicenseFileId());
        }
        // 查找所有该用户作为上传者产生的文件
        List<FileRecord> uploadedFiles = fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>().eq(FileRecord::getUploaderId, user.getId()));
        if (uploadedFiles != null) {
            for (FileRecord fileRecord : uploadedFiles) {
                if (fileRecord.getId() != null) {
                    fileIds.add(fileRecord.getId());
                }
            }
        }
        fileRecordService.deleteFilesIfUnreferenced(fileIds);
    }

    private List<Long> buildArticleFileIds(Article article) {
        List<Long> fileIds = new ArrayList<>();
        if (article.getCoverFileId() != null) {
            fileIds.add(article.getCoverFileId());
        }
        if (article.getPreviewImageId() != null) {
            fileIds.add(article.getPreviewImageId());
        }
        return fileIds;
    }

    private List<Long> buildArticleImageWallFileIds(Long articleId) {
        if (articleId == null) {
            return new ArrayList<>();
        }
        List<ArticleImage> articleImages = articleImageMapper.selectList(new LambdaQueryWrapper<ArticleImage>()
                .eq(ArticleImage::getArticleId, articleId)
                .select(ArticleImage::getFileId));
        if (articleImages == null) {
            return new ArrayList<>();
        }
        return articleImages.stream().map(ArticleImage::getFileId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    // --- VO 构建辅助方法 ---

    private UserRegisterVO buildRegisterVO(User user) {
        return UserRegisterVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .licenseName(user.getLicenseName())
                .licenseFileId(user.getLicenseFileId())
                .licenseUrl(user.getLicenseUrl())
                .role(user.getRole())
                .auditStatus(user.getAuditStatus())
                .auditRemark(user.getAuditRemark())
                .createTime(user.getCreateTime())
                .build();
    }

    private UserProfileVO buildUserProfileVO(User user) {
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .companyName(user.getCompanyName())
                .licenseName(user.getLicenseName())
                .role(user.getRole())
                .auditStatus(user.getAuditStatus())
                .createTime(user.getCreateTime())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }

    private UserAdminVO buildAdminVO(User user) {
        return UserAdminVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .licenseName(user.getLicenseName())
                .licenseFileId(user.getLicenseFileId())
                .licenseUrl(user.getLicenseUrl())
                .role(user.getRole())
                .auditStatus(user.getAuditStatus())
                .auditRemark(user.getAuditRemark())
                .createTime(user.getCreateTime())
                .build();
    }

    private ArticleVO buildArticleVO(Article article) {
        return ArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .previewContent(article.getPreviewContent())
                .coverFileId(article.getCoverFileId())
                .coverImageUrl(article.getCoverImageUrl())
                .previewImageId(article.getPreviewImageId())
                .previewImageUrl(article.getPreviewImageUrl())
                .imageWall(buildArticleImageVOs(article.getId()))
                .authorId(article.getAuthorId())
                .authorName(article.getAuthorName())
                .status(article.getStatus())
                .category(article.getCategory())
                .createTime(article.getCreateTime())
                .updateTime(article.getUpdateTime())
                .build();
    }

    private List<ArticleImageVO> buildArticleImageVOs(Long articleId) {
        if (articleId == null) {
            return new ArrayList<>();
        }
        List<ArticleImage> articleImages = articleImageMapper.selectList(new LambdaQueryWrapper<ArticleImage>()
                .eq(ArticleImage::getArticleId, articleId)
                .orderByAsc(ArticleImage::getSortOrder)
                .orderByAsc(ArticleImage::getId));
        if (articleImages == null) {
            return new ArrayList<>();
        }
        return articleImages.stream()
                .sorted(Comparator.comparing(ArticleImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ArticleImage::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> ArticleImageVO.builder()
                        .id(item.getId())
                        .fileId(item.getFileId())
                        .imageUrl(item.getImageUrl())
                        .sortOrder(item.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }
}
