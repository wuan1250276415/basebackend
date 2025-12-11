package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.runtime.ProcessInstance;

import java.util.Collections;
import java.util.Map;

/**
 * 流程实例详情数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessInstanceDetailDTO", description = "流程实例详情信息")
public class ProcessInstanceDetailDTO {

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
     * 是否已挂起
     */
    @Schema(description = "是否已挂起", example = "false")
    private boolean suspended;

    /**
     * 流程实例 ID（别名）
     */
    @Schema(description = "流程实例 ID", example = "12345")
    private String processInstanceId;

    /**
     * 案例实例 ID
     */
    @Schema(description = "案例实例 ID", example = "case_12345")
    private String caseInstanceId;

    /**
     * 是否已结束
     */
    @Schema(description = "是否已结束", example = "false")
    private boolean ended;

    /**
     * 流程变量集合
     */
    @Schema(description = "流程变量集合")
    @Builder.Default
    private Map<String, Object> variables = Collections.emptyMap();

    /**
     * 从 Camunda ProcessInstance 转换为 DTO
     *
     * @param instance Camunda 流程实例
     * @param variables 流程变量
     * @return DTO 对象
     */
    public static ProcessInstanceDetailDTO from(ProcessInstance instance, Map<String, Object> variables) {
        if (instance == null) {
            return null;
        }
        return ProcessInstanceDetailDTO.builder()
                .id(instance.getId())
                .processInstanceId(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .businessKey(instance.getBusinessKey())
                .tenantId(instance.getTenantId())
                .caseInstanceId(instance.getCaseInstanceId())
                .suspended(instance.isSuspended())
                .ended(instance.isEnded())
                .variables(variables == null ? Collections.emptyMap() : variables)
                .build();
    }
}
