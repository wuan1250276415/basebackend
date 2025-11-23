package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.task.Task;

import java.util.Date;

/**
 * 任务摘要数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskSummaryDTO", description = "任务摘要信息")
public class TaskSummaryDTO {

    /**
     * 任务 ID
     */
    @Schema(description = "任务 ID", example = "12345")
    private String id;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称", example = "审批订单")
    private String name;

    /**
     * 任务分配人
     */
    @Schema(description = "任务分配人", example = "alice")
    private String assignee;

    /**
     * 任务拥有者
     */
    @Schema(description = "任务拥有者", example = "bob")
    private String owner;

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:67890")
    private String processDefinitionId;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "12345")
    private String processInstanceId;

    /**
     * 任务创建时间
     */
    @Schema(description = "任务创建时间", example = "2025-01-01 10:00:00")
    private Date createTime;

    /**
     * 任务到期时间
     */
    @Schema(description = "任务到期时间", example = "2025-01-01 18:00:00")
    private Date dueDate;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 任务描述
     */
    @Schema(description = "任务描述", example = "请审批订单")
    private String description;

    /**
     * 任务定义 Key
     */
    @Schema(description = "任务定义 Key", example = "UserTask_1")
    private String taskDefinitionKey;

    /**
     * 任务跟进时间
     */
    @Schema(description = "任务跟进时间", example = "2025-01-01 16:00:00")
    private Date followUpDate;

    /**
     * 任务优先级
     */
    @Schema(description = "任务优先级", example = "50")
    private Integer priority;

    /**
     * 是否挂起
     */
    @Schema(description = "是否挂起", example = "false")
    private Boolean suspended;

    /**
     * 从 Camunda Task 转换为 DTO
     *
     * @param task Camunda 任务
     * @return DTO 对象
     */
    public static TaskSummaryDTO from(Task task) {
        if (task == null) {
            return null;
        }
        return TaskSummaryDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .suspended(task.isSuspended())
                .tenantId(task.getTenantId())
                .build();
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置流程定义Key（兼容性方法）
     * @param processDefinitionKey 流程定义Key
     */
    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.taskDefinitionKey = processDefinitionKey;
    }

    /**
     * 获取流程定义Key（兼容性方法）
     * @return 流程定义Key
     */
    public String getProcessDefinitionKey() {
        return this.taskDefinitionKey;
    }

    /**
     * 设置创建时间（兼容性方法）
     * @param created 创建时间
     */
    public void setCreated(java.time.Instant created) {
        this.createTime = created == null ? null : java.util.Date.from(created);
    }

    /**
     * 设置创建时间（兼容性方法 - Date重载）
     * @param created 创建时间
     */
    public void setCreated(java.util.Date created) {
        this.createTime = created;
    }

    /**
     * 获取创建时间（兼容性方法）
     * @return 创建时间
     */
    public java.time.Instant getCreated() {
        return this.createTime == null ? null : createTime.toInstant();
    }
}
