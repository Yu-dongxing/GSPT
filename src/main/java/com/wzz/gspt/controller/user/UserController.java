package com.wzz.gspt.controller.user;

import com.wzz.gspt.common.Result;
import com.wzz.gspt.dto.user.UserEnterpriseRegisterRequest;
import com.wzz.gspt.dto.user.UserLoginRequest;
import com.wzz.gspt.dto.user.UserNormalRegisterRequest;
import com.wzz.gspt.service.UserService;
import com.wzz.gspt.vo.UserLoginVO;
import com.wzz.gspt.vo.UserRegisterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户前台控制器
 *
 * <p>仅提供普通用户和企业用户的注册、登录、退出登录与当前用户信息接口。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    /**
     * 用户服务
     */
    private final UserService userService;

    /**
     * 普通用户注册接口
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register/normal")
    public Result<UserRegisterVO> registerNormal(@RequestBody UserNormalRegisterRequest request) {
        return Result.success(userService.registerNormal(request));
    }

    /**
     * 企业用户注册并提交审核接口
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register/enterprise")
    public Result<UserRegisterVO> registerEnterprise(@RequestBody UserEnterpriseRegisterRequest request) {
        return Result.success(userService.registerEnterprise(request));
    }

    /**
     * 用户登录接口
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }

    /**
     * 退出登录接口
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        userService.logout();
        return Result.success();
    }

    /**
     * 获取当前登录用户信息接口
     *
     * @return 当前用户信息
     */
    @GetMapping("/current")
    public Result<UserRegisterVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }
}
