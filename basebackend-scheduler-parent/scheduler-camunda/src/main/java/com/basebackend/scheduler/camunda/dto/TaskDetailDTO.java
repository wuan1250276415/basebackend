package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.camunda.bpm.engine.task.Task;

import java.util.Collections;
import java.util.Map;

/**
 * 任务详情数据传输对象
 *
 * <p>继承 {@link TaskSummaryDTO}，在摘要信息基础上增加执行ID、表单Key、变量等详情字段。</p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TaskDetailDTO", description = "任务详情信息")
public class TaskDetailDTO extends TaskSummaryDTO {

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
     * 委托状态
     */
    @Schema(description = "委托状态", example = "PENDING")
    private String delegationState;

    /**
     * 案例实例 ID
     */
    @Schema(description = "案例实例 ID（CMMN 场景）", example = "CASE_123")
    private String caseInstanceId;

    /**
     * 任务变量集合
     */
    @Schema(description = "任务变量集合")
    private Map<String, Object> variables;

    /**
     * 从 Camunda Task 转换为 DTO
     *
     * @param task      Camunda 任务
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

    /**
     * 获取变量，保证非空
     */
    public Map<String, Object> getVariables() {
        return variables == null ? Collections.emptyMap() : variables;
    }

    // ========== 兼容性方法（仅 TaskDetailDTO 额外需要）==========

    /**
     * 设置到期时间（兼容性方法）
     * @param due 到期时间
     */
    public void setDue(java.time.Instant due) {
        setDueDate(due == null ? null : java.util.Date.from(due));
    }

    /**
     * 设置到期时间（兼容性方法 - Date重载）
     * @param due 到期时间
     */
    public void setDue(java.util.Date due) {
        setDueDate(due);
    }

    /**
     * 获取到期时间（兼容性方法）
     * @return 到期时间
     */
    public java.time.Instant getDue() {
        return getDueDate() == null ? null : getDueDate().toInstant();
    }
}
