package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

import java.util.Collections;
import java.util.List;

/**
 * 历史流程实例详情数据传输对象
 *
 * <p>继承 {@link HistoricProcessInstanceDTO}，在摘要信息基础上增加活动轨迹、变量等详情字段。</p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "HistoricProcessInstanceDetailDTO", description = "历史流程实例详情（含变量和活动轨迹）")
public class HistoricProcessInstanceDetailDTO extends HistoricProcessInstanceDTO {

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
    private List<HistoricVariableInstanceDTO> variables;

    /**
     * 活动执行历史
     */
    @Schema(description = "活动执行历史")
    private List<HistoricActivityInstanceDTO> activities;

    /**
     * 从 Camunda HistoricProcessInstance 转换为 DTO
     *
     * @param instance   Camunda 历史流程实例
     * @param variables  流程变量列表
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
     * 获取变量，保证非空
     */
    public List<HistoricVariableInstanceDTO> getVariables() {
        return variables == null ? Collections.emptyList() : variables;
    }

    /**
     * 获取活动历史，保证非空
     */
    public List<HistoricActivityInstanceDTO> getActivities() {
        return activities == null ? Collections.emptyList() : activities;
    }

    private static <T> List<T> defaultList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
