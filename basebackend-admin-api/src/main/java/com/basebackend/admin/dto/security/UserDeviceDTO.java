package com.basebackend.admin.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户设备信息 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Schema(description = "用户设备信息")
public record UserDeviceDTO(
    @Schema(description = "设备ID") Long id,
    @Schema(description = "设备类型") String deviceType,
    @Schema(description = "设备名称") String deviceName,
    @Schema(description = "浏览器信息") String browser,
    @Schema(description = "操作系统") String os,
    @Schema(description = "IP地址") String ipAddress,
    @Schema(description = "登录位置") String location,
    @Schema(description = "是否信任设备") Integer isTrusted,
    @Schema(description = "最后活跃时间") LocalDateTime lastActiveTime,
    @Schema(description = "首次登录时间") LocalDateTime firstLoginTime
) {}
