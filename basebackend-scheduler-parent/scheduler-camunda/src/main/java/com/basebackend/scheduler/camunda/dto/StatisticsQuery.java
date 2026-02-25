package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 统计分析查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Schema(name = "StatisticsQuery", description = "统计分析查询请求参数")
public record StatisticsQuery(
        @Schema(description = "租户 ID，支持多租户过滤", example = "tenant_001")
        String tenantId,

        @Schema(description = "流程定义 ID，精确匹配", example = "order_approval:1:67890")
        String processDefinitionId,

        @Schema(description = "流程定义 Key", example = "order_approval")
        String processDefinitionKey,

        @Schema(description = "统计时间范围起始（ISO-8601 格式，如 2025-01-01T00:00:00Z）", example = "2025-01-01T00:00:00Z")
        Instant startTime,

        @Schema(description = "统计时间范围结束（ISO-8601 格式，如 2025-01-31T23:59:59Z）", example = "2025-01-31T23:59:59Z")
        Instant endTime,

        @Schema(description = "分配人", example = "alice")
        String assignee
) {
}
