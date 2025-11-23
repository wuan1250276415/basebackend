package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程定义统计数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessStatisticsDTO", description = "流程定义统计信息")
public class ProcessStatisticsDTO {

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
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 总部署次数
     */
    @Schema(description = "总部署次数", example = "5")
    private Long totalDeployments;

    /**
     * 总实例数
     */
    @Schema(description = "总实例数", example = "100")
    private Long totalInstances;

    /**
     * 运行中实例数
     */
    @Schema(description = "运行中实例数", example = "10")
    private Long runningInstances;

    /**
     * 已完成实例数
     */
    @Schema(description = "已完成实例数", example = "85")
    private Long completedInstances;

    /**
     * 已终止实例数
     */
    @Schema(description = "已终止实例数", example = "5")
    private Long terminatedInstances;

    /**
     * 完成率（百分比）
     */
    @Schema(description = "完成率（百分比）", example = "85.0")
    private Double completionRate;

    /**
     * 平均处理时长（毫秒）
     */
    @Schema(description = "平均处理时长（毫秒）", example = "3600000")
    private Long averageProcessingTime;

    /**
     * 首次部署时间
     */
    @Schema(description = "首次部署时间", example = "2025-01-01T10:00:00Z")
    private String firstDeploymentTime;

    /**
     * 最近部署时间
     */
    @Schema(description = "最近部署时间", example = "2025-01-15T14:30:00Z")
    private String lastDeploymentTime;

    /**
     * 总流程定义数
     */
    @Schema(description = "总流程定义数", example = "20")
    private Long totalDefinitions;

    /**
     * 最新版本流程定义数
     */
    @Schema(description = "最新版本流程定义数", example = "15")
    private Long latestVersionDefinitions;

    /**
     * 活跃流程定义数
     */
    @Schema(description = "活跃流程定义数", example = "18")
    private Long activeDefinitions;

    /**
     * 挂起流程定义数
     */
    @Schema(description = "挂起流程定义数", example = "2")
    private Long suspendedDefinitions;

    /**
     * 设置总流程定义数
     */
    public void setTotalDefinitions(Long totalDefinitions) {
        this.totalDefinitions = totalDefinitions;
    }

    /**
     * 设置最新版本流程定义数
     */
    public void setLatestVersionDefinitions(Long latestVersionDefinitions) {
        this.latestVersionDefinitions = latestVersionDefinitions;
    }

    /**
     * 设置活跃流程定义数
     */
    public void setActiveDefinitions(Long activeDefinitions) {
        this.activeDefinitions = activeDefinitions;
    }

    /**
     * 设置挂起流程定义数
     */
    public void setSuspendedDefinitions(Long suspendedDefinitions) {
        this.suspendedDefinitions = suspendedDefinitions;
    }

    /**
     * 设置指定Key的运行中实例数
     */
    public void setKeyInstancesRunning(Long keyInstancesRunning) {
        // 设置到 runningInstances
        this.runningInstances = keyInstancesRunning;
    }

    /**
     * 设置指定Key的已完成实例数
     */
    public void setKeyInstancesCompleted(Long keyInstancesCompleted) {
        // 设置到 completedInstances
        this.completedInstances = keyInstancesCompleted;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置最新版本数（兼容性方法）
     * @param latestVersionCount 最新版本数
     */
    public void setLatestVersionCount(long latestVersionCount) {
        this.latestVersionDefinitions = latestVersionCount;
    }

    /**
     * 设置旧版本数（兼容性方法）
     * @param olderVersionCount 旧版本数
     */
    public void setOlderVersionCount(long olderVersionCount) {
        // 计算旧版本 = 总定义 - 最新版本
        this.totalDefinitions = (this.totalDefinitions == null ? 0 : this.totalDefinitions) + olderVersionCount;
    }
}
