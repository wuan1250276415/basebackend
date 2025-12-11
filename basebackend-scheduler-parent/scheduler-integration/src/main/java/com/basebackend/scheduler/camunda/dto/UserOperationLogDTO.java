package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.UserOperationLogEntry;

import java.time.Instant;

/**
 * 用户操作审计日志数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserOperationLogDTO", description = "Camunda 用户操作审计日志")
public class UserOperationLogDTO {

    /**
     * 日志 ID
     */
    @Schema(description = "日志 ID", example = "log_12345")
    private String id;

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:67890")
    private String processDefinitionId;

    /**
     * 流程定义 Key
     */
    @Schema(description = "流程定义 Key", example = "order_approval")
    private String processDefinitionKey;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "12345")
    private String processInstanceId;

    /**
     * 任务 ID
     */
    @Schema(description = "任务 ID", example = "task_12345")
    private String taskId;

    /**
     * 执行 ID
     */
    @Schema(description = "执行 ID", example = "exec_12345")
    private String executionId;

    /**
     * 操作 ID
     */
    @Schema(description = "操作 ID", example = "op_12345")
    private String operationId;

    /**
     * 作业定义 ID
     */
    @Schema(description = "作业定义 ID", example = "job_def_12345")
    private String jobDefinitionId;

    /**
     * 批次 ID
     */
    @Schema(description = "批次 ID", example = "batch_12345")
    private String batchId;

    /**
     * 部署 ID
     */
    @Schema(description = "部署 ID", example = "dep_12345")
    private String deploymentId;

    /**
     * 实体类型
     */
    @Schema(description = "实体类型", example = "Task")
    private String entityType;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型", example = "Complete")
    private String operationType;

    /**
     * 修改属性名
     */
    @Schema(description = "修改属性名", example = "assignee")
    private String property;

    /**
     * 原始值
     */
    @Schema(description = "原始值", example = "alice")
    private String orgValue;

    /**
     * 新值
     */
    @Schema(description = "新值", example = "bob")
    private String newValue;

    /**
     * 操作人
     */
    @Schema(description = "操作人", example = "admin")
    private String userId;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳", example = "2025-01-01T10:00:00Z")
    private Instant timestamp;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 从 Camunda UserOperationLogEntry 转换为 DTO
     *
     * @param entry Camunda 用户操作日志条目
     * @return DTO 对象
     */
    public static UserOperationLogDTO from(UserOperationLogEntry entry) {
        if (entry == null) {
            return null;
        }
        return UserOperationLogDTO.builder()
                .id(entry.getId())
                .processDefinitionId(entry.getProcessDefinitionId())
                .processDefinitionKey(entry.getProcessDefinitionKey())
                .processInstanceId(entry.getProcessInstanceId())
                .taskId(entry.getTaskId())
                .jobDefinitionId(entry.getJobDefinitionId())
                .batchId(entry.getBatchId())
                .deploymentId(entry.getDeploymentId())
                .entityType(entry.getEntityType())
                .operationType(entry.getOperationType())
                .property(entry.getProperty())
                .orgValue(entry.getOrgValue())
                .newValue(entry.getNewValue())
                .userId(entry.getUserId())
                .timestamp(toInstant(entry.getTimestamp()))
                .tenantId(entry.getTenantId())
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

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置操作类型（兼容性方法）
     * @param operation 操作类型
     */
    public void setOperation(String operation) {
        this.operationType = operation;
    }

    /**
     * 获取操作类型（兼容性方法）
     * @return 操作类型
     */
    public String getOperation() {
        return this.operationType;
    }

    /**
     * 设置时间戳（兼容性方法）
     * @param time 时间戳
     */
    public void setTime(Instant time) {
        this.timestamp = time;
    }

    /**
     * 获取时间戳（兼容性方法）
     * @return 时间戳
     */
    public Instant getTime() {
        return this.timestamp;
    }

    /**
     * 设置详细信息（兼容性方法）
     * @param details 详细信息
     */
    public void setDetails(String details) {
        // 将详细信息合并到 newValue 字段
        this.newValue = details;
    }

    /**
     * 获取详细信息（兼容性方法）
     * @return 详细信息
     */
    public String getDetails() {
        return this.newValue;
    }
}
