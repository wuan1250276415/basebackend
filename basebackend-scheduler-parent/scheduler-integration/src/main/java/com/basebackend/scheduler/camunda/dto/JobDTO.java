package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.runtime.Job;

import java.util.Date;

/**
 * 作业数据传输对象
 *
 * <p>
 * 用于展示 Camunda 作业（Job）的信息，包括定时作业、异步作业、失败作业等。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "JobDTO", description = "作业信息")
public class JobDTO {

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
     * 作业类型
     */
    @Schema(description = "作业类型（如 async-continuation, timer-transition 等）", example = "async-continuation")
    private String jobType;

    /**
     * 作业处理器类型
     */
    @Schema(description = "作业处理器类型", example = "async-continuation")
    private String jobHandlerType;

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
     * 是否失败作业
     */
    @Schema(description = "是否失败作业（重试次数为0且有异常消息）", example = "true")
    private Boolean failed;

    /**
     * 从 Camunda Job 转换为 DTO
     *
     * @param job Camunda 作业对象
     * @return DTO 对象
     */
    public static JobDTO from(Job job) {
        if (job == null) {
            return null;
        }

        // Camunda Job.getRetries() 返回 int 原始类型，不能为 null
        int retries = job.getRetries();
        boolean isFailed = retries == 0 && job.getExceptionMessage() != null;

        return JobDTO.builder()
                .id(job.getId())
                .processInstanceId(job.getProcessInstanceId())
                .processDefinitionId(job.getProcessDefinitionId())
                .processDefinitionKey(job.getProcessDefinitionKey())
                .executionId(job.getExecutionId())
                // Job 接口没有 getJobHandlerType()，设置为 null 或从其他源获取
                .jobHandlerType(null)
                .duedate(job.getDuedate())
                .createTime(job.getCreateTime())
                .retries(retries)
                .exceptionMessage(job.getExceptionMessage())
                .priority(job.getPriority())
                .tenantId(job.getTenantId())
                .suspended(job.isSuspended())
                .deploymentId(job.getDeploymentId())
                .failed(isFailed)
                .build();
    }
}
