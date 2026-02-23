package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;

import java.util.Collections;
import java.util.Map;

/**
 * 流程实例详情数据传输对象
 *
 * <p>继承 {@link ProcessInstanceDTO}，在摘要信息基础上增加流程变量等详情字段。</p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProcessInstanceDetailDTO", description = "流程实例详情信息")
public class ProcessInstanceDetailDTO extends ProcessInstanceDTO {

    /**
     * 流程变量集合
     */
    @Schema(description = "流程变量集合")
    private Map<String, Object> variables;

    /**
     * 从 Camunda ProcessInstance 转换为 DTO
     *
     * @param instance  Camunda 流程实例
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

    /**
     * 获取变量，保证非空
     */
    public Map<String, Object> getVariables() {
        return variables == null ? Collections.emptyMap() : variables;
    }
}
