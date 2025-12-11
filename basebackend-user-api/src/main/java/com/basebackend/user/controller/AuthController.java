package com.basebackend.user.controller;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.exception.BusinessException;
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
        } catch (BusinessException e) {
            log.warn("用户登录业务异常，用户名：{}，错误：{}", loginRequest.getUsername(), e.getMessage());
            return e.getErrorCode() != null 
                    ? Result.error(e.getErrorCode(), e.getMessage()) 
                    : Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录系统异常，用户名：{}", loginRequest.getUsername(), e);
            return Result.error("系统繁忙，请稍后重试");
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
        } catch (BusinessException e) {
            log.warn("用户登出业务异常：{}", e.getMessage());
            return e.getErrorCode() != null 
                    ? Result.error(e.getErrorCode(), e.getMessage()) 
                    : Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户登出系统异常", e);
            return Result.error("系统繁忙，请稍后重试");
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
        } catch (BusinessException e) {
            log.warn("刷新Token业务异常：{}", e.getMessage());
            return e.getErrorCode() != null 
                    ? Result.error(e.getErrorCode(), e.getMessage()) 
                    : Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("刷新Token系统异常", e);
            return Result.error("系统繁忙，请稍后重试");
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
        } catch (BusinessException e) {
            log.warn("获取用户信息业务异常：{}", e.getMessage());
            return e.getErrorCode() != null 
                    ? Result.error(e.getErrorCode(), e.getMessage()) 
                    : Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取用户信息系统异常", e);
            return Result.error("系统繁忙，请稍后重试");
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
        } catch (BusinessException e) {
            log.warn("修改密码业务异常：{}", e.getMessage());
            return e.getErrorCode() != null 
                    ? Result.error(e.getErrorCode(), e.getMessage()) 
                    : Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("修改密码系统异常", e);
            return Result.error("系统繁忙，请稍后重试");
        }
    }
}
