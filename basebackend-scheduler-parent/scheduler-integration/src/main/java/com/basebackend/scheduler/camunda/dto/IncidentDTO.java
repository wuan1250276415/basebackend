package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IncidentDTO", description = "异常事件信息")
public class IncidentDTO {

    /**
     * 异常事件 ID
     */
    @Schema(description = "异常事件 ID", example = "12345")
    private String id;

    /**
     * 异常类型
     */
    @Schema(description = "异常类型", example = "failedJob")
    private String incidentType;

    /**
     * 异常消息
     */
    @Schema(description = "异常消息", example = "Connection timeout")
    private String incidentMessage;

    /**
     * 异常发生时间
     */
    @Schema(description = "异常发生时间", example = "2025-01-01T10:00:00")
    private Date incidentTimestamp;

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:12345")
    private String processDefinitionId;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "67890")
    private String processInstanceId;

    /**
     * 执行实例 ID
     */
    @Schema(description = "执行实例 ID", example = "11111")
    private String executionId;

    /**
     * 活动 ID（发生异常的 BPMN 节点 ID）
     */
    @Schema(description = "活动 ID", example = "ServiceTask_1")
    private String activityId;

    /**
     * 失败的活动 ID
     */
    @Schema(description = "失败的活动 ID", example = "ServiceTask_1")
    private String failedActivityId;

    /**
     * 关联的作业定义 ID
     */
    @Schema(description = "作业定义 ID", example = "job_def_123")
    private String jobDefinitionId;

    /**
     * 关联的配置（如作业 ID）
     */
    @Schema(description = "配置信息", example = "job_123")
    private String configuration;

    /**
     * 根异常事件 ID
     */
    @Schema(description = "根异常事件 ID", example = "root_incident_123")
    private String rootCauseIncidentId;

    /**
     * 原因异常事件 ID
     */
    @Schema(description = "原因异常事件 ID", example = "cause_incident_123")
    private String causeIncidentId;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 历史异常事件是否已解决
     */
    @Schema(description = "是否已解决", example = "false")
    private Boolean resolved;

    /**
     * 历史异常事件解决时间
     */
    @Schema(description = "解决时间", example = "2025-01-01T12:00:00")
    private Date resolvedTimestamp;

    /**
     * 注解/备注
     */
    @Schema(description = "注解/备注", example = "手动处理完成")
    private String annotation;

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

        return IncidentDTO.builder()
                .id(incident.getId())
                .incidentType(incident.getIncidentType())
                .incidentMessage(incident.getIncidentMessage())
                .incidentTimestamp(incident.getIncidentTimestamp())
                .processDefinitionId(incident.getProcessDefinitionId())
                .processInstanceId(incident.getProcessInstanceId())
                .executionId(incident.getExecutionId())
                .activityId(incident.getActivityId())
                .failedActivityId(incident.getFailedActivityId())
                .jobDefinitionId(incident.getJobDefinitionId())
                .configuration(incident.getConfiguration())
                .rootCauseIncidentId(incident.getRootCauseIncidentId())
                .causeIncidentId(incident.getCauseIncidentId())
                .tenantId(incident.getTenantId())
                .annotation(incident.getAnnotation())
                .build();
    }
}
