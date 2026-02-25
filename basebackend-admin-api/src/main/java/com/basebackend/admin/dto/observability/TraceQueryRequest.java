package com.basebackend.admin.dto.observability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 追踪查询请求
 */
@Schema(description = "追踪查询请求")
public record TraceQueryRequest(
    @Schema(description = "TraceId", example = "abc123def456") String traceId,
    @Schema(description = "服务名称", example = "basebackend-admin-api") String serviceName,
    @Schema(description = "操作名称", example = "GET /api/users") String operationName,
    @Schema(description = "最小持续时间（毫秒）", example = "100") Long minDuration,
    @Schema(description = "最大持续时间（毫秒）", example = "5000") Long maxDuration,
    @Schema(description = "开始时间") LocalDateTime startTime,
    @Schema(description = "结束时间") LocalDateTime endTime,
    @Schema(description = "分页大小", example = "20") Integer limit
) {
    public TraceQueryRequest {
        if (limit == null) limit = 20;
    }
}
