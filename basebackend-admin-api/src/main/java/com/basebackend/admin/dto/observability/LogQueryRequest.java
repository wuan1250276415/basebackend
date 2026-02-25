package com.basebackend.admin.dto.observability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 日志查询请求
 */
@Schema(description = "日志查询请求")
public record LogQueryRequest(
    @Schema(description = "关键词搜索", example = "error") String keyword,
    @Schema(description = "日志级别：INFO, WARN, ERROR, DEBUG", example = "ERROR") String level,
    @Schema(description = "TraceId", example = "abc123") String traceId,
    @Schema(description = "应用名称", example = "basebackend-admin-api") String application,
    @Schema(description = "开始时间") LocalDateTime startTime,
    @Schema(description = "结束时间") LocalDateTime endTime,
    @Schema(description = "分页大小", example = "100") Integer limit
) {
    public LogQueryRequest {
        if (limit == null) limit = 100;
    }
}
