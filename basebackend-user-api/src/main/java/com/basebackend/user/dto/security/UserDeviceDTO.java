package com.basebackend.user.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设备信息 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@Schema(description = "用户设备信息")
public class UserDeviceDTO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "浏览器信息")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "登录位置")
    private String location;

    @Schema(description = "是否信任设备")
    private Integer isTrusted;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveTime;

    @Schema(description = "首次登录时间")
    private LocalDateTime firstLoginTime;
}
