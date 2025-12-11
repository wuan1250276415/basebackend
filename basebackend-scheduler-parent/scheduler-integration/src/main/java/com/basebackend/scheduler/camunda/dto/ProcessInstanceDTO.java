package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.runtime.ProcessInstance;

/**
 * 流程实例数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessInstanceDTO", description = "流程实例信息")
public class ProcessInstanceDTO {

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
     * 业务关联键
     */
    @Schema(description = "业务关联键", example = "ORDER_20250101_001")
    private String businessKey;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

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
     * 是否挂起
     */
    @Schema(description = "是否挂起", example = "false")
    private Boolean suspended;

    /**
     * 是否已结束
     */
    @Schema(description = "是否已结束", example = "false")
    private Boolean ended;

    /**
     * 从 Camunda ProcessInstance 转换为 DTO
     *
     * @param instance Camunda 流程实例
     * @return DTO 对象
     */
    public static ProcessInstanceDTO from(ProcessInstance instance) {
        if (instance == null) {
            return null;
        }
        return ProcessInstanceDTO.builder()
                .id(instance.getId())
                .processInstanceId(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .businessKey(instance.getBusinessKey())
                .tenantId(instance.getTenantId())
                .caseInstanceId(instance.getCaseInstanceId())
                .suspended(instance.isSuspended())
                .ended(instance.isEnded())
                .build();
    }
}
