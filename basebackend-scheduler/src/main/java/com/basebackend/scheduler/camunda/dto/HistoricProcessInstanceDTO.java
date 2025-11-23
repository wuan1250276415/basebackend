package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

import java.time.Instant;

/**
 * 历史流程实例数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HistoricProcessInstanceDTO", description = "历史流程实例信息")
public class HistoricProcessInstanceDTO {

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "12345")
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
     * 业务键
     */
    @Schema(description = "业务键", example = "ORDER_20250101_001")
    private String businessKey;

    /**
     * 启动时间
     */
    @Schema(description = "启动时间", example = "2025-01-01T10:00:00Z")
    private Instant startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2025-01-01T11:00:00Z")
    private Instant endTime;

    /**
     * 持续时长（毫秒）
     */
    @Schema(description = "持续时长（毫秒）", example = "3600000")
    private Long durationInMillis;

    /**
     * 启动人
     */
    @Schema(description = "启动人", example = "alice")
    private String startUserId;

    /**
     * 删除原因
     */
    @Schema(description = "删除原因", example = "Cancelled by user")
    private String deleteReason;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "COMPLETED")
    private String state;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 流程定义名称
     */
    @Schema(description = "流程定义名称", example = "订单审批流程")
    private String processDefinitionName;

    /**
     * 从 Camunda HistoricProcessInstance 转换为 DTO
     *
     * @param instance Camunda 历史流程实例
     * @return DTO 对象
     */
    public static HistoricProcessInstanceDTO from(HistoricProcessInstance instance) {
        if (instance == null) {
            return null;
        }
        return HistoricProcessInstanceDTO.builder()
                .id(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(instance.getProcessDefinitionKey())
                .businessKey(instance.getBusinessKey())
                .startTime(toInstant(instance.getStartTime()))
                .endTime(toInstant(instance.getEndTime()))
                .durationInMillis(instance.getDurationInMillis())
                .startUserId(instance.getStartUserId())
                .deleteReason(instance.getDeleteReason())
                .state(instance.getState())
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
