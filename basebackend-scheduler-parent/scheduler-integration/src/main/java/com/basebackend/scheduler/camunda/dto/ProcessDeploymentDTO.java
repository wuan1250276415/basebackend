package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.repository.Deployment;

import java.util.Date;

/**
 * 流程部署信息数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessDeploymentDTO", description = "流程部署信息")
public class ProcessDeploymentDTO {

    /**
     * 部署 ID
     */
    @Schema(description = "部署 ID", example = "12345")
    private String id;

    /**
     * 部署名称
     */
    @Schema(description = "部署名称", example = "订单审批流程 v1.0")
    private String name;

    /**
     * 部署分类
     */
    @Schema(description = "部署分类", example = "审批流程")
    private String category;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 部署时间
     */
    @Schema(description = "部署时间", example = "2025-01-01 10:00:00")
    private Date deploymentTime;

    /**
     * 从 Camunda Deployment 转换为 DTO
     *
     * @param deployment Camunda 部署对象
     * @return DTO 对象
     */
    public static ProcessDeploymentDTO from(Deployment deployment) {
        if (deployment == null) {
            return null;
        }
        return ProcessDeploymentDTO.builder()
                .id(deployment.getId())
                .name(deployment.getName())
                .tenantId(deployment.getTenantId())
                .deploymentTime(deployment.getDeploymentTime())
                .build();
    }
}
