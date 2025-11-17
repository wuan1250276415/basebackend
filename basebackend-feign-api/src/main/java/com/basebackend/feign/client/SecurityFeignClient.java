package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.fallback.SecurityFeignFallbackFactory;
import com.basebackend.feign.dto.security.User2FADTO;
import com.basebackend.feign.dto.security.UserDeviceDTO;
import com.basebackend.feign.dto.security.UserOperationLogDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 安全服务 Feign 客户端
 * 用于 auth-service 调用 admin-api 的安全服务
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@FeignClient(
        name = FeignServiceConstants.ADMIN_SERVICE,
        contextId = "securityFeignClient",
        path = "/api/admin/security",
        fallbackFactory = SecurityFeignFallbackFactory.class
)
public interface SecurityFeignClient {

    /**
     * 获取当前用户设备列表
     *
     * @return 设备列表
     */
    @GetMapping("/devices")
    @Operation(summary = "获取当前用户设备列表", description = "获取当前登录用户的所有登录设备")
    Result<List<UserDeviceDTO>> getCurrentUserDevices();

    /**
     * 移除设备
     *
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @DeleteMapping("/devices/{deviceId}")
    @Operation(summary = "移除设备", description = "移除指定的登录设备")
    Result<Void> removeDevice(@Parameter(description = "设备ID") @PathVariable("deviceId") Long deviceId);

    /**
     * 信任设备
     *
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @PutMapping("/devices/{deviceId}/trust")
    @Operation(summary = "信任设备", description = "将指定设备标记为受信任")
    Result<Void> trustDevice(@Parameter(description = "设备ID") @PathVariable("deviceId") Long deviceId);

    /**
     * 获取操作日志
     *
     * @param limit 限制数量
     * @return 操作日志列表
     */
    @GetMapping("/operation-logs")
    @Operation(summary = "获取操作日志", description = "获取当前用户的操作日志")
    Result<List<UserOperationLogDTO>> getOperationLogs(
            @Parameter(description = "限制数量") @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit
    );

    /**
     * 获取2FA配置
     *
     * @return 2FA配置
     */
    @GetMapping("/2fa")
    @Operation(summary = "获取2FA配置", description = "获取当前用户的双因素认证配置")
    Result<User2FADTO> get2FAConfig();

    /**
     * 启用2FA
     *
     * @param type 2FA类型
     * @param verifyCode 验证码
     * @return 操作结果
     */
    @PostMapping("/2fa/enable")
    @Operation(summary = "启用2FA", description = "启用双因素认证")
    Result<Void> enable2FA(
            @Parameter(description = "2FA类型") @RequestParam("type") String type,
            @Parameter(description = "验证码") @RequestParam("verifyCode") String verifyCode
    );

    /**
     * 禁用2FA
     *
     * @param verifyCode 验证码
     * @return 操作结果
     */
    @PostMapping("/2fa/disable")
    @Operation(summary = "禁用2FA", description = "禁用双因素认证")
    Result<Void> disable2FA(@Parameter(description = "验证码") @RequestParam("verifyCode") String verifyCode);
}
