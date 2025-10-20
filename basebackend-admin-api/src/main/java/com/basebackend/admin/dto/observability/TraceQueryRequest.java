package com.basebackend.admin.dto.observability;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 追踪查询请求
 */
@Data
@Schema(description = "追踪查询请求")
public class TraceQueryRequest {

    @Schema(description = "TraceId", example = "abc123def456")
    private String traceId;

    @Schema(description = "服务名称", example = "basebackend-admin-api")
    private String serviceName;

    @Schema(description = "操作名称", example = "GET /api/users")
    private String operationName;

    @Schema(description = "最小持续时间（毫秒）", example = "100")
    private Long minDuration;

    @Schema(description = "最大持续时间（毫秒）", example = "5000")
    private Long maxDuration;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "分页大小", example = "20")
    private Integer limit = 20;
}
