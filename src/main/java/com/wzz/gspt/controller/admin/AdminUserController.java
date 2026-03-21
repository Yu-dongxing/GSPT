package com.wzz.gspt.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wzz.gspt.common.Result;
import com.wzz.gspt.dto.user.EnterpriseAuditRequest;
import com.wzz.gspt.dto.user.UserAdminQueryRequest;
import com.wzz.gspt.dto.user.UserAdminSaveRequest;
import com.wzz.gspt.dto.user.UserBatchDeleteRequest;
import com.wzz.gspt.dto.user.UserLoginRequest;
import com.wzz.gspt.service.UserService;
import com.wzz.gspt.vo.UserAdminVO;
import com.wzz.gspt.vo.UserLoginVO;
import com.wzz.gspt.vo.UserRegisterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户管理控制器
 *
 * <p>提供管理员登录、企业审核、用户分页查询以及增删改查接口。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/user")
public class AdminUserController {

    /**
     * 用户服务
     */
    private final UserService userService;

    /**
     * 管理员登录接口
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<UserLoginVO> adminLogin(@RequestBody UserLoginRequest request) {
        return Result.success(userService.adminLogin(request));
    }

    /**
     * 后台审核企业用户接口
     *
     * @param request 审核请求
     * @return 审核结果
     */
    @PostMapping("/audit/enterprise")
    public Result<UserRegisterVO> auditEnterprise(@RequestBody EnterpriseAuditRequest request) {
        return Result.success(userService.auditEnterprise(request));
    }

    /**
     * 管理员分页查询用户接口
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    public Result<IPage<UserAdminVO>> pageUsers(@RequestBody UserAdminQueryRequest request) {
        return Result.success(userService.pageUsers(request));
    }

    /**
     * 管理员获取用户详情接口
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    public Result<UserRegisterVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetail(userId));
    }

    /**
     * 管理员新增用户接口
     *
     * @param request 新增请求
     * @return 新增后的用户信息
     */
    @PostMapping("/add")
    public Result<UserRegisterVO> createUser(@RequestBody UserAdminSaveRequest request) {
        return Result.success(userService.createUser(request));
    }

    /**
     * 管理员修改用户接口
     *
     * @param request 修改请求
     * @return 修改后的用户信息
     */
    @PostMapping("/update")
    public Result<UserRegisterVO> updateUser(@RequestBody UserAdminSaveRequest request) {
        return Result.success(userService.updateUser(request));
    }

    /**
     * 管理员删除单个用户接口
     *
     * @param userId 用户 ID
     * @return 删除结果
     */
    @GetMapping("/delete/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return Result.success();
    }

    /**
     * 管理员批量删除用户接口
     *
     * @param request 批量删除请求
     * @return 删除结果
     */
    @PostMapping("/delete/batch")
    public Result<?> deleteUsers(@RequestBody UserBatchDeleteRequest request) {
        userService.deleteUsers(request);
        return Result.success();
    }
}
