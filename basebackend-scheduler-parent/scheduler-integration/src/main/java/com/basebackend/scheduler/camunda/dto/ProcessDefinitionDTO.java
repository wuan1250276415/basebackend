package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.repository.ProcessDefinition;

/**
 * 流程定义数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessDefinitionDTO", description = "流程定义信息")
public class ProcessDefinitionDTO {

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:12345")
    private String id;

    /**
     * 流程定义 Key
     */
    @Schema(description = "流程定义 Key", example = "order_approval")
    private String key;

    /**
     * 流程定义名称
     */
    @Schema(description = "流程定义名称", example = "订单审批流程")
    private String name;

    /**
     * 流程定义版本
     */
    @Schema(description = "流程定义版本", example = "1")
    private Integer version;

    /**
     * 版本标签
     */
    @Schema(description = "版本标签", example = "v1.0")
    private String versionTag;

    /**
     * 部署 ID
     */
    @Schema(description = "部署 ID", example = "12345")
    private String deploymentId;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 流程分类
     */
    @Schema(description = "流程分类", example = "审批流程")
    private String category;

    /**
     * 流程描述
     */
    @Schema(description = "流程描述", example = "订单审批工作流")
    private String description;

    /**
     * 是否已挂起
     */
    @Schema(description = "是否已挂起", example = "false")
    private boolean suspended;

    /**
     * 资源名称（BPMN 文件名）
     */
    @Schema(description = "资源名称", example = "order_approval.bpmn")
    private String resourceName;

    /**
     * 流程图资源名称
     */
    @Schema(description = "流程图资源名称", example = "order_approval.png")
    private String diagramResourceName;

    /**
     * 部署时间
     */
    @Schema(description = "部署时间")
    private java.util.Date deploymentTime;

    /**
     * 历史生存时间
     */
    @Schema(description = "历史生存时间", example = "180")
    private Integer historyTimeToLive;

    /**
     * 是否可在任务列表中启动
     */
    @Schema(description = "是否可在任务列表中启动", example = "true")
    private boolean startableInTasklist;

    /**
     * 从 Camunda ProcessDefinition 转换为 DTO
     *
     * @param definition Camunda 流程定义
     * @return DTO 对象
     */
    public static ProcessDefinitionDTO from(ProcessDefinition definition) {
        if (definition == null) {
            return null;
        }
        return ProcessDefinitionDTO.builder()
                .id(definition.getId())
                .key(definition.getKey())
                .name(definition.getName())
                .version(definition.getVersion())
                .versionTag(definition.getVersionTag())
                .deploymentId(definition.getDeploymentId())
                .tenantId(definition.getTenantId())
                .category(definition.getCategory())
                .description(definition.getDescription())
                .suspended(definition.isSuspended())
                .resourceName(definition.getResourceName())
                .diagramResourceName(definition.getDiagramResourceName())
                .historyTimeToLive(definition.getHistoryTimeToLive())
                .startableInTasklist(definition.isStartableInTasklist())
                .build();
    }
}
