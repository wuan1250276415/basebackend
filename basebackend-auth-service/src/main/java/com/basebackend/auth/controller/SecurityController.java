package com.basebackend.auth.controller;

import com.basebackend.auth.service.SecurityService;
import com.basebackend.feign.dto.security.User2FADTO;
import com.basebackend.feign.dto.security.UserDeviceDTO;
import com.basebackend.feign.dto.security.UserOperationLogDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 安全管理控制器
 * 设备管理、双因素认证、操作日志
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Tag(name = "安全管理", description = "账号安全相关接口")
public class SecurityController {

    private final SecurityService securityService;

    @Operation(summary = "获取当前用户设备列表", description = "获取当前登录用户的所有登录设备")
    @GetMapping("/devices")
    public Result<List<UserDeviceDTO>> getDevices() {
        List<UserDeviceDTO> devices = securityService.getCurrentUserDevices();
        return Result.success(devices);
    }

    @Operation(summary = "移除设备", description = "移除指定的登录设备")
    @DeleteMapping("/devices/{deviceId}")
    public Result<Void> removeDevice(@Parameter(description = "设备ID") @PathVariable Long deviceId) {
        securityService.removeDevice(deviceId);
        return Result.success();
    }

    @Operation(summary = "信任设备", description = "将指定设备标记为受信任")
    @PutMapping("/devices/{deviceId}/trust")
    public Result<Void> trustDevice(@Parameter(description = "设备ID") @PathVariable Long deviceId) {
        securityService.trustDevice(deviceId);
        return Result.success();
    }

    @Operation(summary = "获取操作日志", description = "获取当前用户的操作日志")
    @GetMapping("/operation-logs")
    public Result<List<UserOperationLogDTO>> getOperationLogs(
            @Parameter(description = "限制数量") @RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<UserOperationLogDTO> logs = securityService.getCurrentUserOperationLogs(limit);
        return Result.success(logs);
    }

    @Operation(summary = "获取2FA配置", description = "获取当前用户的双因素认证配置")
    @GetMapping("/2fa")
    public Result<User2FADTO> get2FAConfig() {
        User2FADTO config = securityService.getCurrent2FAConfig();
        return Result.success(config);
    }

    @Operation(summary = "启用2FA", description = "启用双因素认证")
    @PostMapping("/2fa/enable")
    public Result<Void> enable2FA(
            @Parameter(description = "2FA类型") @RequestParam String type,
            @Parameter(description = "验证码") @RequestParam String verifyCode) {
        securityService.enable2FA(type, verifyCode);
        return Result.success();
    }

    @Operation(summary = "禁用2FA", description = "禁用双因素认证")
    @PostMapping("/2fa/disable")
    public Result<Void> disable2FA(@Parameter(description = "验证码") @RequestParam String verifyCode) {
        securityService.disable2FA(verifyCode);
        return Result.success();
    }
}
