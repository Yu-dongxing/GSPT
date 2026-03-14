package com.wzz.gspt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wzz.gspt.dto.user.EnterpriseAuditRequest;
import com.wzz.gspt.dto.user.UserAdminQueryRequest;
import com.wzz.gspt.dto.user.UserAdminSaveRequest;
import com.wzz.gspt.dto.user.UserBatchDeleteRequest;
import com.wzz.gspt.dto.user.UserEnterpriseRegisterRequest;
import com.wzz.gspt.dto.user.UserLoginRequest;
import com.wzz.gspt.dto.user.UserNormalRegisterRequest;
import com.wzz.gspt.dto.user.UserPasswordChangeRequest;
import com.wzz.gspt.dto.user.UserProfileUpdateRequest;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.vo.UserLoginVO;
import com.wzz.gspt.vo.UserRegisterVO;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 普通用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    UserRegisterVO registerNormal(UserNormalRegisterRequest request);

    /**
     * 企业用户注册并提交审核
     *
     * @param request 注册请求
     * @return 注册结果
     */
    UserRegisterVO registerEnterprise(UserEnterpriseRegisterRequest request);

    /**
     * 普通用户或企业用户登录
     *
     * @param request 登录请求
     * @return 登录结果
     */
    UserLoginVO login(UserLoginRequest request);

    /**
     * 管理员登录
     *
     * @param request 登录请求
     * @return 登录结果
     */
    UserLoginVO adminLogin(UserLoginRequest request);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户信息
     */
    UserRegisterVO getCurrentUser();

    /**
     * 修改当前登录用户个人信息
     *
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    UserRegisterVO updateProfile(UserProfileUpdateRequest request);

    /**
     * 修改当前登录用户密码
     *
     * @param request 修改密码请求
     */
    void changePassword(UserPasswordChangeRequest request);

    /**
     * 后台审核企业用户
     *
     * @param request 审核请求
     * @return 审核后的用户信息
     */
    UserRegisterVO auditEnterprise(EnterpriseAuditRequest request);

    /**
     * 管理员分页查询用户
     *
     * @param request 查询请求
     * @return 用户分页结果
     */
    IPage<UserRegisterVO> pageUsers(UserAdminQueryRequest request);

    /**
     * 管理员获取用户详情
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    UserRegisterVO getUserDetail(Long userId);

    /**
     * 管理员新增用户
     *
     * @param request 新增请求
     * @return 新增后的用户信息
     */
    UserRegisterVO createUser(UserAdminSaveRequest request);

    /**
     * 管理员修改用户
     *
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    UserRegisterVO updateUser(UserAdminSaveRequest request);

    /**
     * 管理员删除单个用户
     *
     * @param userId 用户 ID
     */
    void deleteUser(Long userId);

    /**
     * 管理员批量删除用户
     *
     * @param request 批量删除请求
     */
    void deleteUsers(UserBatchDeleteRequest request);
}
