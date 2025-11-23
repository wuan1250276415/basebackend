package com.basebackend.user.controller;

import com.basebackend.common.context.UserContext;
import com.basebackend.user.dto.LoginRequest;
import com.basebackend.user.dto.LoginResponse;
import com.basebackend.user.dto.PasswordChangeDTO;
import com.basebackend.user.service.AuthService;
import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口")
    @OperationLog(operation="用户登录", businessType = BusinessType.SELECT)
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());
        try {
            LoginResponse response = authService.login(loginRequest);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录失败，用户名：{}，错误：{}", loginRequest.getUsername(), e.getMessage(), e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出接口")
    @OperationLog(operation="用户登出", businessType = BusinessType.SELECT)
    public Result<String> logout() {
        log.info("用户登出请求");
        try {
            authService.logout();
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("用户登出失败，错误：{}", e.getMessage(), e);
            return Result.error("登出失败，请稍后重试");
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "刷新访问令牌")
    @OperationLog(operation="刷新Token", businessType = BusinessType.SELECT)
    public Result<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("刷新Token请求");
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return Result.success("Token刷新成功", response);
        } catch (Exception e) {
            log.error("刷新token失败，错误：{}", e.getMessage(), e);
            return Result.error("刷新失败，请稍后重试");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户信息")
    @OperationLog(operation="获取用户信息", businessType = BusinessType.SELECT)
    public Result<UserContext> getCurrentUserInfo() {
        log.info("获取当前用户信息请求");
        try {
            UserContext userInfo = authService.getCurrentUserInfo();
            return Result.success("获取用户信息成功", userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败，错误：{}", e.getMessage(), e);
            return Result.error("获取用户信息失败，请稍后重试");
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    @OperationLog(operation="修改密码", businessType = BusinessType.UPDATE)
    public Result<String> changePassword(@Validated @RequestBody PasswordChangeDTO passwordChangeDTO) {
        log.info("修改密码请求");
        try {
            authService.changePassword(passwordChangeDTO);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            log.error("密码修改失败，错误：{}", e.getMessage(), e);
            return Result.error("密码修改失败，请稍后重试");
        }
    }
}
