package com.basebackend.user.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户操作日志 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@Schema(description = "用户操作日志")
public class UserOperationLogDTO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作描述")
    private String operationDesc;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "操作状态")
    private Integer status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
