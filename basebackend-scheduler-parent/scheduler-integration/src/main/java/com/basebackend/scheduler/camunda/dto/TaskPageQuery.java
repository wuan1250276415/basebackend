package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 任务分页查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TaskPageQuery", description = "任务分页查询请求")
public class TaskPageQuery extends BasePageQuery {

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID，支持多租户过滤",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 流程实例 ID
     */
    @Schema(
        description = "流程实例 ID",
        example = "12345"
    )
    private String processInstanceId;

    /**
     * 任务分配人
     */
    @Schema(
        description = "任务分配人，精确匹配",
        example = "alice"
    )
    private String assignee;

    /**
     * 候选用户
     */
    @Schema(
        description = "候选用户，精确匹配",
        example = "bob"
    )
    private String candidateUser;

    /**
     * 候选组
     */
    @Schema(
        description = "候选组，精确匹配",
        example = "manager"
    )
    private String candidateGroup;

    /**
     * 任务名称模糊匹配
     */
    @Schema(
        description = "任务名称模糊匹配",
        example = "审批"
    )
    private String nameLike;

    /**
     * 任务创建时间起始（ISO-8601 格式）
     */
    @Schema(
        description = "任务创建时间起始（ISO-8601 格式，如 2025-01-01T10:00:00Z）",
        example = "2025-01-01T00:00:00Z"
    )
    private Instant createdAfter;

    /**
     * 任务创建时间截止（ISO-8601 格式）
     */
    @Schema(
        description = "任务创建时间截止（ISO-8601 格式，如 2025-01-01T23:59:59Z）",
        example = "2025-01-01T23:59:59Z"
    )
    private Instant createdBefore;

    /**
     * 流程定义 Key
     */
    @Schema(
        description = "流程定义 Key",
        example = "order_approval"
    )
    private String processDefinitionKey;
}
