package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.HistoricActivityInstance;

import java.time.Instant;

/**
 * 历史活动实例数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HistoricActivityInstanceDTO", description = "历史流程活动执行记录")
public class HistoricActivityInstanceDTO {

    /**
     * 活动实例 ID
     */
    @Schema(description = "活动实例 ID", example = "act_12345")
    private String id;

    /**
     * 活动 ID（BPMN 元素 ID）
     */
    @Schema(description = "活动 ID（BPMN 元素 ID）", example = "UserTask_Approve")
    private String activityId;

    /**
     * 活动名称
     */
    @Schema(description = "活动名称", example = "审批订单")
    private String activityName;

    /**
     * 活动类型
     */
    @Schema(description = "活动类型", example = "userTask")
    private String activityType;

    /**
     * 执行 ID
     */
    @Schema(description = "执行 ID", example = "exec_67890")
    private String executionId;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "12345")
    private String processInstanceId;

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:67890")
    private String processDefinitionId;

    /**
     * 任务 ID（仅任务活动类型）
     */
    @Schema(description = "任务 ID（仅任务活动类型）", example = "task_12345")
    private String taskId;

    /**
     * 受理人（仅任务活动类型）
     */
    @Schema(description = "受理人（仅任务活动类型）", example = "alice")
    private String assignee;

    /**
     * 调用的子流程实例 ID
     */
    @Schema(description = "调用的子流程实例 ID", example = "sub_12345")
    private String calledProcessInstanceId;

    /**
     * 调用的子案例实例 ID
     */
    @Schema(description = "调用的子案例实例 ID", example = "case_12345")
    private String calledCaseInstanceId;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间", example = "2025-01-01T10:00:00Z")
    private Instant startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2025-01-01T10:30:00Z")
    private Instant endTime;

    /**
     * 持续时长（毫秒）
     */
    @Schema(description = "持续时长（毫秒）", example = "1800000")
    private Long durationInMillis;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 从 Camunda HistoricActivityInstance 转换为 DTO
     *
     * @param instance Camunda 历史活动实例
     * @return DTO 对象
     */
    public static HistoricActivityInstanceDTO from(HistoricActivityInstance instance) {
        if (instance == null) {
            return null;
        }
        return HistoricActivityInstanceDTO.builder()
                .id(instance.getId())
                .activityId(instance.getActivityId())
                .activityName(instance.getActivityName())
                .activityType(instance.getActivityType())
                .executionId(instance.getExecutionId())
                .processInstanceId(instance.getProcessInstanceId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .taskId(instance.getTaskId())
                .assignee(instance.getAssignee())
                .calledProcessInstanceId(instance.getCalledProcessInstanceId())
                .startTime(toInstant(instance.getStartTime()))
                .endTime(toInstant(instance.getEndTime()))
                .durationInMillis(instance.getDurationInMillis())
                .tenantId(instance.getTenantId())
                .build();
    }

    /**
     * 将 Date 转换为 Instant
     *
     * @param date 日期
     * @return Instant
     */
    private static Instant toInstant(java.util.Date date) {
        return date == null ? null : date.toInstant();
    }
}
