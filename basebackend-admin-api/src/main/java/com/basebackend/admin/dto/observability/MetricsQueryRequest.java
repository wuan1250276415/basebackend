package com.basebackend.admin.dto.observability;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标查询请求
 */
@Data
@Schema(description = "指标查询请求")
public class MetricsQueryRequest {

    @Schema(description = "指标名称", example = "api.calls.total")
    private String metricName;

    @Schema(description = "标签过滤（JSON格式）", example = "{\"method\":\"GET\",\"uri\":\"/api/users\"}")
    private String tags;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "聚合类型：sum, avg, max, min", example = "avg")
    private String aggregation = "avg";

    @Schema(description = "时间步长（秒）", example = "60")
    private Integer step = 60;
}
