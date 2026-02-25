package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.camunda.bpm.engine.runtime.Incident;

import java.util.Date;

/**
 * 异常事件数据传输对象
 *
 * <p>
 * 用于展示 Camunda 异常事件（Incident）的信息，包括流程执行中的错误和失败。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Schema(name = "IncidentDTO", description = "异常事件信息")
public record IncidentDTO(
        @Schema(description = "异常事件 ID", example = "12345")
        String id,

        @Schema(description = "异常类型", example = "failedJob")
        String incidentType,

        @Schema(description = "异常消息", example = "Connection timeout")
        String incidentMessage,

        @Schema(description = "异常发生时间", example = "2025-01-01T10:00:00")
        Date incidentTimestamp,

        @Schema(description = "流程定义 ID", example = "order_approval:1:12345")
        String processDefinitionId,

        @Schema(description = "流程实例 ID", example = "67890")
        String processInstanceId,

        @Schema(description = "执行实例 ID", example = "11111")
        String executionId,

        @Schema(description = "活动 ID", example = "ServiceTask_1")
        String activityId,

        @Schema(description = "失败的活动 ID", example = "ServiceTask_1")
        String failedActivityId,

        @Schema(description = "作业定义 ID", example = "job_def_123")
        String jobDefinitionId,

        @Schema(description = "配置信息", example = "job_123")
        String configuration,

        @Schema(description = "根异常事件 ID", example = "root_incident_123")
        String rootCauseIncidentId,

        @Schema(description = "原因异常事件 ID", example = "cause_incident_123")
        String causeIncidentId,

        @Schema(description = "租户 ID", example = "tenant_001")
        String tenantId,

        @Schema(description = "是否已解决", example = "false")
        Boolean resolved,

        @Schema(description = "解决时间", example = "2025-01-01T12:00:00")
        Date resolvedTimestamp,

        @Schema(description = "注解/备注", example = "手动处理完成")
        String annotation
) {
    /**
     * 从 Camunda Incident 转换为 DTO
     *
     * @param incident Camunda 异常事件对象
     * @return DTO 对象
     */
    public static IncidentDTO from(Incident incident) {
        if (incident == null) {
            return null;
        }
        return new IncidentDTO(
                incident.getId(),
                incident.getIncidentType(),
                incident.getIncidentMessage(),
                incident.getIncidentTimestamp(),
                incident.getProcessDefinitionId(),
                incident.getProcessInstanceId(),
                incident.getExecutionId(),
                incident.getActivityId(),
                incident.getFailedActivityId(),
                incident.getJobDefinitionId(),
                incident.getConfiguration(),
                incident.getRootCauseIncidentId(),
                incident.getCauseIncidentId(),
                incident.getTenantId(),
                null,
                null,
                incident.getAnnotation()
        );
    }
}
