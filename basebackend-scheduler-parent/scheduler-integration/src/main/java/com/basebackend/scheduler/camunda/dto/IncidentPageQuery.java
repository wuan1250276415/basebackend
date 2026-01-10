package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 异常事件分页查询参数
 *
 * <p>
 * 用于分页查询 Camunda 异常事件（Incident）列表，支持按类型、流程定义、时间等过滤。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "IncidentPageQuery", description = "异常事件分页查询参数")
public class IncidentPageQuery extends BasePageQuery {

    /**
     * 异常类型
     * 常见类型：failedJob, failedExternalTask
     */
    @Schema(description = "异常类型", example = "failedJob")
    private String incidentType;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "67890")
    private String processInstanceId;

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:12345")
    private String processDefinitionId;

    /**
     * 流程定义 Key
     */
    @Schema(description = "流程定义 Key", example = "order_approval")
    private String processDefinitionKey;

    /**
     * 活动 ID（BPMN 节点 ID）
     */
    @Schema(description = "活动 ID", example = "ServiceTask_1")
    private String activityId;

    /**
     * 执行实例 ID
     */
    @Schema(description = "执行实例 ID", example = "11111")
    private String executionId;

    /**
     * 作业定义 ID
     */
    @Schema(description = "作业定义 ID", example = "job_def_123")
    private String jobDefinitionId;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 异常发生时间早于
     */
    @Schema(description = "异常发生时间早于", example = "2025-01-01T12:00:00")
    private Date incidentTimestampBefore;

    /**
     * 异常发生时间晚于
     */
    @Schema(description = "异常发生时间晚于", example = "2025-01-01T08:00:00")
    private Date incidentTimestampAfter;

    /**
     * 排序字段
     * 可选值：incidentId, incidentTimestamp, incidentType, executionId,
     * activityId, processInstanceId, processDefinitionId
     */
    @Schema(description = "排序字段", example = "incidentTimestamp")
    private String sortBy;

    /**
     * 排序方向
     * 可选值：asc, desc
     */
    @Schema(description = "排序方向", example = "desc")
    private String sortOrder;
}
