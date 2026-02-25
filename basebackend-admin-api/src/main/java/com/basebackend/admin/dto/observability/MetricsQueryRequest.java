package com.basebackend.admin.dto.observability;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 指标查询请求
 */
@Schema(description = "指标查询请求")
public record MetricsQueryRequest(
    @Schema(description = "指标名称", example = "api.calls.total") String metricName,
    @Schema(description = "标签过滤（JSON格式）", example = "{\"method\":\"GET\",\"uri\":\"/api/users\"}") String tags,
    @Schema(description = "开始时间") LocalDateTime startTime,
    @Schema(description = "结束时间") LocalDateTime endTime,
    @Schema(description = "聚合类型：sum, avg, max, min", example = "avg") String aggregation,
    @Schema(description = "时间步长（秒）", example = "60") Integer step
) {
    public MetricsQueryRequest {
        if (aggregation == null) aggregation = "avg";
        if (step == null) step = 60;
    }
}
