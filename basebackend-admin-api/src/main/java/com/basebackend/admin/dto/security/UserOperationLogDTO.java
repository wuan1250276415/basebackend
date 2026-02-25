package com.basebackend.admin.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户操作日志 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Schema(description = "用户操作日志")
public record UserOperationLogDTO(
    @Schema(description = "日志ID") Long id,
    @Schema(description = "操作类型") String operationType,
    @Schema(description = "操作描述") String operationDesc,
    @Schema(description = "IP地址") String ipAddress,
    @Schema(description = "位置") String location,
    @Schema(description = "浏览器") String browser,
    @Schema(description = "操作系统") String os,
    @Schema(description = "操作状态") Integer status,
    @Schema(description = "错误信息") String errorMsg,
    @Schema(description = "创建时间") LocalDateTime createTime
) {}
