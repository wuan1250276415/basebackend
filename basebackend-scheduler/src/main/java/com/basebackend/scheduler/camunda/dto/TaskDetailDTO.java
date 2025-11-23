package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.task.Task;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 任务详情数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskDetailDTO", description = "任务详情信息")
public class TaskDetailDTO {

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
     * 任务描述
     */
    @Schema(description = "任务描述", example = "请审批订单")
    private String description;

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
     * 案例实例 ID
     */
    @Schema(description = "案例实例 ID（CMMN 场景）", example = "CASE_123")
    private String caseInstanceId;

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
     * 任务跟进时间
     */
    @Schema(description = "任务跟进时间", example = "2025-01-01 16:00:00")
    private Date followUpDate;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 任务定义 Key
     */
    @Schema(description = "任务定义 Key", example = "UserTask_1")
    private String taskDefinitionKey;

    /**
     * 执行 ID
     */
    @Schema(description = "执行 ID", example = "exec_123")
    private String executionId;

    /**
     * 表单 Key
     */
    @Schema(description = "表单 Key", example = "taskForm")
    private String formKey;

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
     * 委托状态
     */
    @Schema(description = "委托状态", example = "PENDING")
    private String delegationState;

    /**
     * 任务变量集合
     */
    @Schema(description = "任务变量集合")
    private Map<String, Object> variables = Collections.emptyMap();

    /**
     * 从 Camunda Task 转换为 DTO
     *
     * @param task Camunda 任务
     * @param variables 任务变量
     * @return DTO 对象
     */
    public static TaskDetailDTO from(Task task, Map<String, Object> variables) {
        if (task == null) {
            return null;
        }
        return TaskDetailDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .executionId(task.getExecutionId())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .suspended(task.isSuspended())
                .tenantId(task.getTenantId())
                .formKey(task.getFormKey())
                .delegationState(task.getDelegationState() != null
                        ? task.getDelegationState().name() : null)
                .variables(variables == null ? Collections.emptyMap() : variables)
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

    /**
     * 设置到期时间（兼容性方法）
     * @param due 到期时间
     */
    public void setDue(java.time.Instant due) {
        this.dueDate = due == null ? null : java.util.Date.from(due);
    }

    /**
     * 设置到期时间（兼容性方法 - Date重载）
     * @param due 到期时间
     */
    public void setDue(java.util.Date due) {
        this.dueDate = due;
    }

    /**
     * 获取到期时间（兼容性方法）
     * @return 到期时间
     */
    public java.time.Instant getDue() {
        return this.dueDate == null ? null : dueDate.toInstant();
    }
}
