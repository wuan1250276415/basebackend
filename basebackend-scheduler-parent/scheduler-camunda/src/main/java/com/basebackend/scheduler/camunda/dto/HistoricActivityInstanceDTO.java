package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.HistoricActivityInstance;

import java.time.Instant;

/**
 * 历史活动实例数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HistoricActivityInstanceDTO", description = "历史流程活动执行记录")
public class HistoricActivityInstanceDTO {

    @Schema(description = "活动实例 ID") private String id;
    @Schema(description = "活动 ID") private String activityId;
    @Schema(description = "活动名称") private String activityName;
    @Schema(description = "活动类型") private String activityType;
    @Schema(description = "执行 ID") private String executionId;
    @Schema(description = "流程实例 ID") private String processInstanceId;
    @Schema(description = "流程定义 ID") private String processDefinitionId;
    @Schema(description = "任务 ID") private String taskId;
    @Schema(description = "受理人") private String assignee;
    @Schema(description = "调用的子流程实例 ID") private String calledProcessInstanceId;
    @Schema(description = "调用的子案例实例 ID") private String calledCaseInstanceId;
    @Schema(description = "开始时间") private Instant startTime;
    @Schema(description = "结束时间") private Instant endTime;
    @Schema(description = "持续时长（毫秒）") private Long durationInMillis;
    @Schema(description = "租户 ID") private String tenantId;

    public static HistoricActivityInstanceDTO from(HistoricActivityInstance instance) {
        if (instance == null) return null;
        HistoricActivityInstanceDTO dto = new HistoricActivityInstanceDTO();
        dto.setId(instance.getId());
        dto.setActivityId(instance.getActivityId());
        dto.setActivityName(instance.getActivityName());
        dto.setActivityType(instance.getActivityType());
        dto.setExecutionId(instance.getExecutionId());
        dto.setProcessInstanceId(instance.getProcessInstanceId());
        dto.setProcessDefinitionId(instance.getProcessDefinitionId());
        dto.setTaskId(instance.getTaskId());
        dto.setAssignee(instance.getAssignee());
        dto.setCalledProcessInstanceId(instance.getCalledProcessInstanceId());
        dto.setStartTime(toInstant(instance.getStartTime()));
        dto.setEndTime(toInstant(instance.getEndTime()));
        dto.setDurationInMillis(instance.getDurationInMillis());
        dto.setTenantId(instance.getTenantId());
        return dto;
    }

    private static Instant toInstant(java.util.Date date) {
        return date == null ? null : date.toInstant();
    }
}
