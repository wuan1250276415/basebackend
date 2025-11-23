package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 历史流程实例详情数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HistoricProcessInstanceDetailDTO", description = "历史流程实例详情（含变量和活动轨迹）")
public class HistoricProcessInstanceDetailDTO {

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
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 启动人
     */
    @Schema(description = "启动人", example = "alice")
    private String startUserId;

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
     * 状态
     */
    @Schema(description = "状态", example = "COMPLETED")
    private String state;

    /**
     * 删除原因
     */
    @Schema(description = "删除原因", example = "Cancelled by user")
    private String deleteReason;

    /**
     * 流程定义名称
     */
    @Schema(description = "流程定义名称", example = "Order Approval Process")
    private String processDefinitionName;

    /**
     * 流程定义版本
     */
    @Schema(description = "流程定义版本", example = "1")
    private Integer processDefinitionVersion;

    /**
     * 起始活动 ID
     */
    @Schema(description = "起始活动 ID", example = "StartEvent_1")
    private String startActivityId;

    /**
     * 结束活动 ID
     */
    @Schema(description = "结束活动 ID", example = "EndEvent_1")
    private String endActivityId;

    /**
     * 父流程实例 ID
     */
    @Schema(description = "父流程实例 ID", example = "parent_process_id")
    private String superProcessInstanceId;

    /**
     * 父案例实例 ID
     */
    @Schema(description = "父案例实例 ID", example = "parent_case_id")
    private String superCaseInstanceId;

    /**
     * 案例实例 ID
     */
    @Schema(description = "案例实例 ID", example = "case_instance_id")
    private String caseInstanceId;

    /**
     * 流程变量列表
     */
    @Schema(description = "流程变量列表")
    private List<HistoricVariableInstanceDTO> variables = Collections.emptyList();

    /**
     * 活动执行历史
     */
    @Schema(description = "活动执行历史")
    private List<HistoricActivityInstanceDTO> activities = Collections.emptyList();

    /**
     * 从 Camunda HistoricProcessInstance 转换为 DTO
     *
     * @param instance Camunda 历史流程实例
     * @param variables 流程变量列表
     * @param activities 活动历史列表
     * @return DTO 对象
     */
    public static HistoricProcessInstanceDetailDTO from(
            HistoricProcessInstance instance,
            List<HistoricVariableInstanceDTO> variables,
            List<HistoricActivityInstanceDTO> activities) {
        if (instance == null) {
            return null;
        }
        return HistoricProcessInstanceDetailDTO.builder()
                .id(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(instance.getProcessDefinitionKey())
                .businessKey(instance.getBusinessKey())
                .tenantId(instance.getTenantId())
                .startUserId(instance.getStartUserId())
                .startTime(toInstant(instance.getStartTime()))
                .endTime(toInstant(instance.getEndTime()))
                .durationInMillis(instance.getDurationInMillis())
                .state(instance.getState())
                .deleteReason(instance.getDeleteReason())
                .variables(defaultList(variables))
                .activities(defaultList(activities))
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

    /**
     * 默认列表处理
     *
     * @param source 源列表
     * @param <T> 泛型类型
     * @return 非空列表
     */
    private static <T> List<T> defaultList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
