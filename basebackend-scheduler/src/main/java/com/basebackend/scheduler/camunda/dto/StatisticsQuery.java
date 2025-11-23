package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * 统计分析查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "StatisticsQuery", description = "统计分析查询请求参数")
public class StatisticsQuery {

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID，支持多租户过滤",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 流程定义 ID
     */
    @Schema(
        description = "流程定义 ID，精确匹配",
        example = "order_approval:1:67890"
    )
    private String processDefinitionId;

    /**
     * 流程定义 Key
     */
    @Schema(
        description = "流程定义 Key",
        example = "order_approval"
    )
    private String processDefinitionKey;

    /**
     * 统计时间范围起始（ISO-8601 格式）
     */
    @Schema(
        description = "统计时间范围起始（ISO-8601 格式，如 2025-01-01T00:00:00Z）",
        example = "2025-01-01T00:00:00Z"
    )
    private Instant startTime;

    /**
     * 统计时间范围结束（ISO-8601 格式）
     */
    @Schema(
        description = "统计时间范围结束（ISO-8601 格式，如 2025-01-31T23:59:59Z）",
        example = "2025-01-31T23:59:59Z"
    )
    private Instant endTime;

    /**
     * 分配人
     */
    @Schema(
        description = "分配人",
        example = "alice"
    )
    private String assignee;

    /**
     * 获取分配人
     */
    public String getAssignee() {
        return assignee;
    }
}
