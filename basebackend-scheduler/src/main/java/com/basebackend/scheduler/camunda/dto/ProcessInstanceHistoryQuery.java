package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 历史流程实例分页查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProcessInstanceHistoryQuery", description = "历史流程实例分页查询请求")
public class ProcessInstanceHistoryQuery extends BasePageQuery {

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID，支持多租户过滤",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 业务键（精确匹配）
     */
    @Schema(
        description = "业务键，精确匹配",
        example = "ORDER_20250101_001"
    )
    private String businessKey;

    /**
     * 流程定义 Key（精确匹配）
     */
    @Schema(
        description = "流程定义 Key",
        example = "order_approval"
    )
    private String processDefinitionKey;

    /**
     * 流程定义 ID（精确匹配）
     */
    @Schema(
        description = "流程定义 ID",
        example = "order_approval:2:12345"
    )
    private String processDefinitionId;

    /**
     * 启动人（精确匹配）
     */
    @Schema(
        description = "启动人",
        example = "alice"
    )
    private String startedBy;

    /**
     * 是否只查询已完成实例
     */
    @Schema(
        description = "是否只查询已完成实例：true=已完成，false=未完成，null=不限制",
        example = "true"
    )
    private Boolean finished;

    /**
     * 开始时间（过滤用）
     */
    @Schema(
        description = "开始时间过滤",
        example = "2025-01-01T00:00:00Z"
    )
    private java.time.Instant startedAfter;

    /**
     * 开始时间（过滤用）
     */
    @Schema(
        description = "开始时间过滤",
        example = "2025-01-31T23:59:59Z"
    )
    private java.time.Instant startedBefore;

    /**
     * 是否已完成
     */
    public Boolean isFinished() {
        return finished;
    }

    /**
     * 获取开始时间（过滤条件）
     */
    public java.time.Instant getStartedAfter() {
        return startedAfter;
    }

    /**
     * 获取开始时间（过滤条件）
     */
    public java.time.Instant getStartedBefore() {
        return startedBefore;
    }
}
