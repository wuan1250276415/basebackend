package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.camunda.bpm.engine.history.HistoricVariableInstance;

import java.time.Instant;

/**
 * 历史流程变量数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Schema(name = "HistoricVariableInstanceDTO", description = "历史流程变量信息")
public record HistoricVariableInstanceDTO(
        @Schema(description = "变量实例 ID", example = "var_12345")
        String id,

        @Schema(description = "变量名", example = "amount")
        String name,

        @Schema(description = "变量类型", example = "Integer")
        String typeName,

        @Schema(description = "变量值")
        Object value,

        @Schema(description = "所属任务 ID（若为任务局部变量）", example = "task_12345")
        String taskId,

        @Schema(description = "活动实例 ID", example = "act_12345")
        String activityInstanceId,

        @Schema(description = "创建时间", example = "2025-01-01T10:00:00Z")
        Instant createTime,

        @Schema(description = "最后更新时间", example = "2025-01-01T10:05:00Z")
        Instant lastUpdatedTime,

        @Schema(description = "租户 ID", example = "tenant_001")
        String tenantId,

        @Schema(description = "流程实例 ID", example = "proc_12345")
        String processInstanceId,

        @Schema(description = "执行实例 ID", example = "exec_12345")
        String executionId
) {
    /**
     * 从 Camunda HistoricVariableInstance 转换为 DTO
     *
     * @param variable Camunda 历史变量实例
     * @return DTO 对象
     */
    public static HistoricVariableInstanceDTO from(HistoricVariableInstance variable) {
        if (variable == null) {
            return null;
        }
        return new HistoricVariableInstanceDTO(
                variable.getId(),
                variable.getName(),
                variable.getTypeName(),
                variable.getValue(),
                variable.getTaskId(),
                variable.getActivityInstanceId(),
                variable.getCreateTime() != null ? variable.getCreateTime().toInstant() : null,
                null,
                variable.getTenantId(),
                variable.getProcessInstanceId(),
                variable.getExecutionId()
        );
    }
}
