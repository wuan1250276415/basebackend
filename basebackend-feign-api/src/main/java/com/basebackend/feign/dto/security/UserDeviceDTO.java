package com.basebackend.feign.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设备DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Data
@Schema(name = "UserDevice", description = "用户登录设备")
public class UserDeviceDTO {

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型：web-网页，mobile-移动端，desktop-桌面端")
    private String deviceType;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "登录位置")
    private String location;

    @Schema(description = "是否受信任：0-否，1-是")
    private Integer isTrusted;

    @Schema(description = "首次登录时间")
    private LocalDateTime firstLoginTime;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
