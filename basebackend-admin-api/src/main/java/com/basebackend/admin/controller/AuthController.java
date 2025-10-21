package com.basebackend.admin.controller;

import com.basebackend.admin.dto.LoginRequest;
import com.basebackend.admin.dto.LoginResponse;
import com.basebackend.admin.dto.PasswordChangeDTO;
import com.basebackend.admin.service.AuthService;
import com.basebackend.common.model.Result;
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
@RequestMapping("/api/admin/auth")
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
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());
        try {
            LoginResponse response = authService.login(loginRequest);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出接口")
    public Result<String> logout() {
        log.info("用户登出请求");
        try {
            authService.logout();
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("用户登出失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "刷新访问令牌")
    public Result<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("刷新Token请求");
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return Result.success("Token刷新成功", response);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户信息")
    public Result<LoginResponse.UserInfo> getCurrentUserInfo() {
        log.info("获取当前用户信息请求");
        try {
            LoginResponse.UserInfo userInfo = authService.getCurrentUserInfo();
            return Result.success("获取用户信息成功", userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    public Result<String> changePassword(@Validated @RequestBody PasswordChangeDTO passwordChangeDTO) {
        log.info("修改密码请求");
        try {
            authService.changePassword(passwordChangeDTO);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            log.error("密码修改失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
