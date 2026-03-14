package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.dto.user.EnterpriseAuditRequest;
import com.wzz.gspt.dto.user.UserAdminQueryRequest;
import com.wzz.gspt.dto.user.UserAdminSaveRequest;
import com.wzz.gspt.dto.user.UserBatchDeleteRequest;
import com.wzz.gspt.dto.user.UserEnterpriseRegisterRequest;
import com.wzz.gspt.dto.user.UserLoginRequest;
import com.wzz.gspt.dto.user.UserNormalRegisterRequest;
import com.wzz.gspt.dto.user.UserPasswordChangeRequest;
import com.wzz.gspt.dto.user.UserProfileUpdateRequest;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import com.wzz.gspt.exception.BusinessException;
import com.wzz.gspt.mapper.FileRecordMapper;
import com.wzz.gspt.mapper.UserMapper;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.service.UserService;
import com.wzz.gspt.vo.UserLoginVO;
import com.wzz.gspt.vo.UserRegisterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 文件记录数据访问层
     */
    private final FileRecordMapper fileRecordMapper;

    /**
     * 普通用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @Override
    @Transactional
    public UserRegisterVO registerNormal(UserNormalRegisterRequest request) {
        validateNormalRequest(request);

        User existedUser = getByUsername(request.getPhone());
        if (existedUser != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "该手机号已注册");
        }

        User user = new User();
        user.setUsername(request.getPhone().trim());
        user.setPassword(request.getPassword().trim());
        user.setRole(UserRole.USER);
        user.setAuditStatus(UserAuditStatus.NONE);

        boolean saved = save(user);
        if (!saved) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "普通用户注册失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 企业用户注册并提交审核
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @Override
    @Transactional
    public UserRegisterVO registerEnterprise(UserEnterpriseRegisterRequest request) {
        validateEnterpriseRequest(request);
        FileRecord licenseFile = validateLicenseFile(request.getLicenseFileId(), request.getLicenseUrl());

        User usernameUser = getByUsername(request.getPhone());
        User emailUser = getByEmail(request.getEmail());
        User targetUser = resolveEnterpriseTarget(usernameUser, emailUser);

        if (targetUser == null) {
            User user = new User();
            fillEnterpriseUser(user, request, licenseFile);
            boolean saved = save(user);
            if (!saved) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业用户注册失败");
            }
            return buildRegisterVO(user);
        }

        fillEnterpriseUser(targetUser, request, licenseFile);
        boolean updated = updateById(targetUser);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业用户重新提交失败");
        }
        return buildRegisterVO(targetUser);
    }

    /**
     * 普通用户或企业用户登录
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @Override
    public UserLoginVO login(UserLoginRequest request) {
        User user = validateLoginCredentials(request);
        validateLoginStatus(user);
        return doLogin(user);
    }

    /**
     * 管理员登录
     *
     * @param request 登录请求
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
     * 获取当前登录用户信息
     *
     * @return 当前用户信息
     */
    @Override
    public UserRegisterVO getCurrentUser() {
        Long currentUserId = getCurrentLoginUserId();
        User user = getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }
        return buildRegisterVO(user);
    }

    /**
     * 修改当前登录用户个人信息
     *
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    @Override
    @Transactional
    public UserRegisterVO updateProfile(UserProfileUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "修改个人信息参数不能为空");
        }

        Long currentUserId = getCurrentLoginUserId();
        User user = getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (StringUtils.hasText(request.getEmail())) {
            User existedEmailUser = getByEmail(request.getEmail());
            if (existedEmailUser != null && !Objects.equals(existedEmailUser.getId(), user.getId())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "邮箱已存在");
            }
            user.setEmail(request.getEmail().trim());
        }

        if (user.getRole() == UserRole.ENTERPRISE) {
            applyEnterpriseProfile(user, request);
        } else {
            if (StringUtils.hasText(request.getCompanyName())) {
                user.setCompanyName(request.getCompanyName().trim());
            }
        }

        boolean updated = updateById(user);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改个人信息失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 修改当前登录用户密码
     *
     * @param request 修改密码请求
     */
    @Override
    @Transactional
    public void changePassword(UserPasswordChangeRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "修改密码参数不能为空");
        }
        if (!StringUtils.hasText(request.getOldPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "原密码不能为空");
        }
        if (!StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "新密码不能为空");
        }

        Long currentUserId = getCurrentLoginUserId();
        User user = getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }
        if (!request.getOldPassword().trim().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "原密码错误");
        }

        user.setPassword(request.getNewPassword().trim());
        boolean updated = updateById(user);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改密码失败");
        }
    }

    /**
     * 后台审核企业用户
     *
     * @param request 审核请求
     * @return 审核结果
     */
    @Override
    @Transactional
    public UserRegisterVO auditEnterprise(EnterpriseAuditRequest request) {
        validateAuditRequest(request);
        ensureCurrentUserIsAdmin();

        User user = getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业用户不存在");
        }
        if (user.getRole() != UserRole.ENTERPRISE) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "该用户不是企业用户");
        }
        if (user.getAuditStatus() != UserAuditStatus.PENDING) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前用户不处于待审核状态");
        }

        user.setAuditStatus(request.getAuditStatus());
        user.setAuditRemark(StringUtils.hasText(request.getAuditRemark()) ? request.getAuditRemark().trim() : null);

        boolean updated = updateById(user);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "企业审核失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理员分页查询用户
     *
     * @param request 查询请求
     * @return 用户分页结果
     */
    @Override
    public IPage<UserRegisterVO> pageUsers(UserAdminQueryRequest request) {
        ensureCurrentUserIsAdmin();
        validatePageRequest(request);

        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(request.getUsername()), User::getUsername, request.getUsername())
                .like(StringUtils.hasText(request.getEmail()), User::getEmail, request.getEmail())
                .like(StringUtils.hasText(request.getCompanyName()), User::getCompanyName, request.getCompanyName())
                .like(StringUtils.hasText(request.getLicenseName()), User::getLicenseName, request.getLicenseName())
                .eq(request.getRole() != null, User::getRole, request.getRole())
                .eq(request.getAuditStatus() != null, User::getAuditStatus, request.getAuditStatus())
                .orderByDesc(User::getCreateTime);

        Page<User> userPage = page(page, wrapper);
        Page<UserRegisterVO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userPage.getRecords().stream().map(this::buildRegisterVO).collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 管理员获取用户详情
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @Override
    public UserRegisterVO getUserDetail(Long userId) {
        ensureCurrentUserIsAdmin();
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户ID不能为空");
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户不存在");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理员新增用户
     *
     * @param request 新增请求
     * @return 新增后的用户信息
     */
    @Override
    @Transactional
    public UserRegisterVO createUser(UserAdminSaveRequest request) {
        ensureCurrentUserIsAdmin();
        validateAdminSaveRequest(request, false);

        User existedUser = getByUsername(request.getUsername());
        if (existedUser != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户名已存在");
        }

        if (StringUtils.hasText(request.getEmail())) {
            User existedEmailUser = getByEmail(request.getEmail());
            if (existedEmailUser != null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "邮箱已存在");
            }
        }

        FileRecord licenseFile = resolveAdminLicenseFile(request);
        User user = new User();
        applyAdminSaveRequest(user, request, licenseFile);

        boolean saved = save(user);
        if (!saved) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "新增用户失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理员修改用户
     *
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    @Override
    @Transactional
    public UserRegisterVO updateUser(UserAdminSaveRequest request) {
        ensureCurrentUserIsAdmin();
        validateAdminSaveRequest(request, true);

        User user = getById(request.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户不存在");
        }

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

        boolean updated = updateById(user);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改用户失败");
        }
        return buildRegisterVO(user);
    }

    /**
     * 管理员删除单个用户
     *
     * @param userId 用户 ID
     */
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        ensureCurrentUserIsAdmin();
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户ID不能为空");
        }

        Long currentUserId = getCurrentLoginUserId();
        if (Objects.equals(currentUserId, userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "不允许删除当前登录管理员");
        }

        boolean removed = removeById(userId);
        if (!removed) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "删除用户失败");
        }
    }

    /**
     * 管理员批量删除用户
     *
     * @param request 批量删除请求
     */
    @Override
    @Transactional
    public void deleteUsers(UserBatchDeleteRequest request) {
        ensureCurrentUserIsAdmin();
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "批量删除用户ID不能为空");
        }

        Long currentUserId = getCurrentLoginUserId();
        if (request.getUserIds().contains(currentUserId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "不允许批量删除当前登录管理员");
        }

        boolean removed = removeByIds(request.getUserIds());
        if (!removed) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "批量删除用户失败");
        }
    }

    /**
     * 校验普通用户注册参数
     */
    private void validateNormalRequest(UserNormalRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "注册参数不能为空");
        }
        if (!StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "手机号不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "密码不能为空");
        }
    }

    /**
     * 校验企业用户注册参数
     */
    private void validateEnterpriseRequest(UserEnterpriseRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "注册参数不能为空");
        }
        if (!StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "手机号不能为空");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "密码不能为空");
        }
        if (!StringUtils.hasText(request.getLicenseName())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "营业执照名称不能为空");
        }
        if (request.getLicenseFileId() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "营业执照文件ID不能为空");
        }
        if (!StringUtils.hasText(request.getLicenseUrl())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "营业执照访问路径不能为空");
        }
    }

    /**
     * 校验登录参数并返回登录用户
     */
    private User validateLoginCredentials(UserLoginRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "登录参数不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "密码不能为空");
        }

        User user = getByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "账号或密码错误");
        }
        if (!request.getPassword().trim().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "账号或密码错误");
        }
        return user;
    }

    /**
     * 执行登录动作
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

    /**
     * 校验审核参数
     */
    private void validateAuditRequest(EnterpriseAuditRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "审核参数不能为空");
        }
        if (request.getUserId() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户ID不能为空");
        }
        if (request.getAuditStatus() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "审核状态不能为空");
        }
        if (request.getAuditStatus() != UserAuditStatus.APPROVED
                && request.getAuditStatus() != UserAuditStatus.REJECTED) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "审核状态仅支持通过或拒绝");
        }
        if (request.getAuditStatus() == UserAuditStatus.REJECTED
                && !StringUtils.hasText(request.getAuditRemark())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "拒绝审核时必须填写原因");
        }
    }

    /**
     * 校验分页请求
     */
    private void validatePageRequest(UserAdminQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "分页查询参数不能为空");
        }
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "页码必须大于等于1");
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "每页条数必须大于等于1");
        }
    }

    /**
     * 校验管理员新增或修改用户参数
     */
    private void validateAdminSaveRequest(UserAdminSaveRequest request, boolean isUpdate) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户参数不能为空");
        }
        if (isUpdate && request.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "修改用户时ID不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "密码不能为空");
        }
        if (request.getRole() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "用户角色不能为空");
        }

        if (request.getRole() == UserRole.ENTERPRISE) {
            if (!StringUtils.hasText(request.getEmail())) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "企业用户邮箱不能为空");
            }
            if (!StringUtils.hasText(request.getLicenseName())) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "企业用户营业执照名称不能为空");
            }
            if (request.getLicenseFileId() == null) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "企业用户营业执照文件ID不能为空");
            }
            if (!StringUtils.hasText(request.getLicenseUrl())) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "企业用户营业执照访问路径不能为空");
            }
        }
    }

    /**
     * 校验营业执照文件是否存在且与请求路径一致
     */
    private FileRecord validateLicenseFile(Long licenseFileId, String licenseUrl) {
        FileRecord fileRecord = fileRecordMapper.selectById(licenseFileId);
        if (fileRecord == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "营业执照文件不存在");
        }

        String trimmedLicenseUrl = licenseUrl.trim();
        if (!trimmedLicenseUrl.equals(fileRecord.getUrlPath())
                && !trimmedLicenseUrl.endsWith(fileRecord.getUrlPath())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "营业执照文件路径不匹配");
        }
        return fileRecord;
    }

    /**
     * 根据用户名查询用户
     */
    private User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).last("limit 1"));
    }

    /**
     * 根据邮箱查询用户
     */
    private User getByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email.trim()).last("limit 1"));
    }

    /**
     * 解析企业注册应写入的新用户或可重提的目标用户
     */
    private User resolveEnterpriseTarget(User usernameUser, User emailUser) {
        if (usernameUser == null && emailUser == null) {
            return null;
        }
        if (usernameUser != null && emailUser != null && !usernameUser.getId().equals(emailUser.getId())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "手机号或邮箱已被其他账号占用，不可重复提交");
        }

        User existedUser = usernameUser != null ? usernameUser : emailUser;
        if (existedUser.getAuditStatus() == UserAuditStatus.REJECTED) {
            return existedUser;
        }
        throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "不可重复提交");
    }

    /**
     * 填充企业用户信息
     */
    private void fillEnterpriseUser(User user, UserEnterpriseRegisterRequest request, FileRecord licenseFile) {
        user.setUsername(request.getPhone().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(request.getPassword().trim());
        user.setLicenseName(request.getLicenseName().trim());
        user.setCompanyName(request.getLicenseName().trim());
        user.setLicenseFileId(licenseFile.getId());
        user.setLicenseUrl(request.getLicenseUrl().trim());
        user.setRole(UserRole.ENTERPRISE);
        user.setAuditStatus(UserAuditStatus.PENDING);
        user.setAuditRemark("企业注册已提交，等待审核");
    }

    /**
     * 应用企业用户个人资料修改
     *
     * @param user 用户对象
     * @param request 修改请求
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
     * 根据管理员保存请求解析营业执照文件
     */
    private FileRecord resolveAdminLicenseFile(UserAdminSaveRequest request) {
        if (request.getRole() != UserRole.ENTERPRISE) {
            return null;
        }
        return validateLicenseFile(request.getLicenseFileId(), request.getLicenseUrl());
    }

    /**
     * 应用管理员新增或修改用户请求
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
            user.setCompanyName(StringUtils.hasText(request.getCompanyName())
                    ? request.getCompanyName().trim()
                    : request.getLicenseName().trim());
            user.setLicenseFileId(licenseFile.getId());
            user.setLicenseUrl(request.getLicenseUrl().trim());
            user.setAuditStatus(request.getAuditStatus() != null ? request.getAuditStatus() : UserAuditStatus.PENDING);
        } else {
            user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
            user.setCompanyName(StringUtils.hasText(request.getCompanyName()) ? request.getCompanyName().trim() : null);
            user.setLicenseName(null);
            user.setLicenseFileId(null);
            user.setLicenseUrl(null);
            user.setAuditStatus(request.getAuditStatus() != null ? request.getAuditStatus() : UserAuditStatus.NONE);
        }
    }

    /**
     * 校验当前用户的登录状态与审核状态是否允许登录
     */
    private void validateLoginStatus(User user) {
        if (user.getRole() == UserRole.ENTERPRISE) {
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
    }

    /**
     * 获取当前登录用户 ID
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
     * 确保当前登录用户为后台管理员
     */
    private void ensureCurrentUserIsAdmin() {
        Long currentUserId = getCurrentLoginUserId();
        User currentUser = getById(currentUserId);
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅后台管理员可执行该操作");
        }
    }

    /**
     * 构造注册返回对象
     */
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
}
