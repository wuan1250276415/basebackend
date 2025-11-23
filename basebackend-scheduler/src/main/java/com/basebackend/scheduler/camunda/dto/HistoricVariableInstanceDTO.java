package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.HistoricVariableInstance;

import java.time.Instant;

/**
 * 历史流程变量数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HistoricVariableInstanceDTO", description = "历史流程变量信息")
public class HistoricVariableInstanceDTO {

    /**
     * 变量实例 ID
     */
    @Schema(description = "变量实例 ID", example = "var_12345")
    private String id;

    /**
     * 变量名
     */
    @Schema(description = "变量名", example = "amount")
    private String name;

    /**
     * 变量类型
     */
    @Schema(description = "变量类型", example = "Integer")
    private String typeName;

    /**
     * 变量值
     */
    @Schema(description = "变量值")
    private Object value;

    /**
     * 所属任务 ID（若为任务局部变量）
     */
    @Schema(description = "所属任务 ID（若为任务局部变量）", example = "task_12345")
    private String taskId;

    /**
     * 活动实例 ID
     */
    @Schema(description = "活动实例 ID", example = "act_12345")
    private String activityInstanceId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-01-01T10:00:00Z")
    private Instant createTime;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间", example = "2025-01-01T10:05:00Z")
    private Instant lastUpdatedTime;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "proc_12345")
    private String processInstanceId;

    /**
     * 执行实例 ID
     */
    @Schema(description = "执行实例 ID", example = "exec_12345")
    private String executionId;

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
        return HistoricVariableInstanceDTO.builder()
                .id(variable.getId())
                .name(variable.getName())
                .typeName(variable.getTypeName())
                .value(variable.getValue())
                .processInstanceId(variable.getProcessInstanceId())
                .executionId(variable.getExecutionId())
                .taskId(variable.getTaskId())
                .activityInstanceId(variable.getActivityInstanceId())
                .createTime(variable.getCreateTime() != null ? variable.getCreateTime().toInstant() : null)
                .tenantId(variable.getTenantId())
                .build();
    }
}
