package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程定义详情DTO
 *
 * <p>继承 {@link ProcessDefinitionDTO}，在摘要信息基础上增加表单、活动列表等详情字段。</p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProcessDefinitionDetailDTO", description = "流程定义详情")
public class ProcessDefinitionDetailDTO extends ProcessDefinitionDTO {

    /**
     * 是否包含开始表单
     */
    @Schema(description = "是否包含开始表单")
    private Boolean hasStartForm;

    /**
     * 任务列表
     */
    @Schema(description = "任务列表")
    private List<TaskSummaryDTO> tasks;

    /**
     * 活动列表
     */
    @Schema(description = "活动列表")
    private List<String> activities;

    /**
     * 历史变量存储类型
     */
    @Schema(description = "历史变量存储类型")
    private String historicVariableStorageType;

    /**
     * 是否可启动
     */
    @Schema(description = "是否可启动")
    private Boolean startable;

    /**
     * 是否有开始表单Key
     */
    @Schema(description = "是否有开始表单Key")
    private Boolean hasStartFormKey;

    /**
     * 部署来源
     */
    @Schema(description = "部署来源")
    private String deploymentSource;

    /**
     * 部署时间（LocalDateTime 格式，覆盖父类的 Date 类型用于详情展示）
     */
    @Schema(description = "部署时间（详情）")
    private LocalDateTime detailDeploymentTime;

    /**
     * 获取详情部署时间
     */
    public LocalDateTime getDetailDeploymentTime() {
        return detailDeploymentTime;
    }

    /**
     * 设置详情部署时间
     */
    public void setDetailDeploymentTime(LocalDateTime detailDeploymentTime) {
        this.detailDeploymentTime = detailDeploymentTime;
    }
}
