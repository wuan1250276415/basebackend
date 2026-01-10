package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 作业分页查询参数
 *
 * <p>
 * 用于分页查询 Camunda 作业（Job）列表，支持按状态、流程定义、执行时间等过滤。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "JobPageQuery", description = "作业分页查询参数")
public class JobPageQuery extends BasePageQuery {

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
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 是否只查询失败作业（重试次数为0）
     */
    @Schema(description = "是否只查询失败作业", example = "true")
    private Boolean failedOnly;

    /**
     * 是否只查询挂起作业
     */
    @Schema(description = "是否只查询挂起作业", example = "false")
    private Boolean suspendedOnly;

    /**
     * 是否只查询可执行作业（截止时间已到且有重试次数）
     */
    @Schema(description = "是否只查询可执行作业", example = "true")
    private Boolean executableOnly;

    /**
     * 是否只查询定时器作业
     */
    @Schema(description = "是否只查询定时器作业", example = "false")
    private Boolean timersOnly;

    /**
     * 是否只查询消息作业（异步作业）
     */
    @Schema(description = "是否只查询消息作业", example = "false")
    private Boolean messagesOnly;

    /**
     * 是否只查询有异常的作业
     */
    @Schema(description = "是否只查询有异常的作业", example = "true")
    private Boolean withException;

    /**
     * 是否只查询无异常的作业
     */
    @Schema(description = "是否只查询无异常的作业", example = "false")
    private Boolean noException;

    /**
     * 截止时间早于（查询即将执行的作业）
     */
    @Schema(description = "截止时间早于", example = "2025-01-01T12:00:00")
    private Date duedateBefore;

    /**
     * 截止时间晚于
     */
    @Schema(description = "截止时间晚于", example = "2025-01-01T08:00:00")
    private Date duedateAfter;

    /**
     * 创建时间早于
     */
    @Schema(description = "创建时间早于", example = "2025-01-01T12:00:00")
    private Date createTimeBefore;

    /**
     * 创建时间晚于
     */
    @Schema(description = "创建时间晚于", example = "2025-01-01T08:00:00")
    private Date createTimeAfter;

    /**
     * 排序字段
     * 可选值：jobId, executionId, processInstanceId, processDefinitionId,
     * processDefinitionKey, jobRetries, jobDuedate, jobPriority
     */
    @Schema(description = "排序字段", example = "jobDuedate")
    private String sortBy;

    /**
     * 排序方向
     * 可选值：asc, desc
     */
    @Schema(description = "排序方向", example = "asc")
    private String sortOrder;
}
