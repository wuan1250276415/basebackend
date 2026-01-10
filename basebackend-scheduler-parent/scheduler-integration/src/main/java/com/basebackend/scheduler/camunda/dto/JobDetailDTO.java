package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 作业详情数据传输对象
 *
 * <p>
 * 包含作业的完整信息，包括异常堆栈等详细内容。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "JobDetailDTO", description = "作业详情信息")
public class JobDetailDTO {

    /**
     * 作业 ID
     */
    @Schema(description = "作业 ID", example = "12345")
    private String id;

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
     * 执行实例 ID
     */
    @Schema(description = "执行实例 ID", example = "11111")
    private String executionId;

    /**
     * 作业处理器类型
     */
    @Schema(description = "作业处理器类型", example = "async-continuation")
    private String jobHandlerType;

    /**
     * 作业处理器配置
     */
    @Schema(description = "作业处理器配置", example = "{...}")
    private String jobHandlerConfiguration;

    /**
     * 作业截止时间（下次执行时间）
     */
    @Schema(description = "作业截止时间", example = "2025-01-01T10:00:00")
    private Date duedate;

    /**
     * 作业创建时间
     */
    @Schema(description = "作业创建时间", example = "2025-01-01T09:00:00")
    private Date createTime;

    /**
     * 重试次数
     */
    @Schema(description = "剩余重试次数", example = "3")
    private Integer retries;

    /**
     * 异常消息
     */
    @Schema(description = "异常消息（若作业失败）", example = "Connection timeout")
    private String exceptionMessage;

    /**
     * 异常堆栈
     */
    @Schema(description = "异常堆栈（完整错误信息）")
    private String exceptionStacktrace;

    /**
     * 作业优先级
     */
    @Schema(description = "作业优先级", example = "0")
    private Long priority;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 是否挂起
     */
    @Schema(description = "是否挂起", example = "false")
    private Boolean suspended;

    /**
     * 部署 ID
     */
    @Schema(description = "部署 ID", example = "deploy_123")
    private String deploymentId;

    /**
     * 活动 ID（BPMN 节点 ID）
     */
    @Schema(description = "活动 ID", example = "ServiceTask_1")
    private String activityId;

    /**
     * 活动名称
     */
    @Schema(description = "活动名称", example = "发送邮件")
    private String activityName;

    /**
     * 是否失败作业
     */
    @Schema(description = "是否失败作业（重试次数为0且有异常消息）", example = "true")
    private Boolean failed;

    /**
     * 是否可立即执行
     */
    @Schema(description = "是否可立即执行（截止时间已到且有重试次数）", example = "true")
    private Boolean executable;

    /**
     * 失败作业 ID（关联）
     */
    @Schema(description = "失败作业 ID", example = "failed_job_123")
    private String failedActivityId;
}
