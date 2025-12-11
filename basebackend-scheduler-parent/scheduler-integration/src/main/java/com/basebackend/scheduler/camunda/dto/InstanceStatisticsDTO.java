package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 流程实例统计数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InstanceStatisticsDTO", description = "流程实例统计信息")
public class InstanceStatisticsDTO {

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

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
     * 统计时间范围起始
     */
    @Schema(description = "统计时间范围起始", example = "2025-01-01T00:00:00Z")
    private Instant timeRangeStart;

    /**
     * 统计时间范围结束
     */
    @Schema(description = "统计时间范围结束", example = "2025-01-31T23:59:59Z")
    private Instant timeRangeEnd;

    /**
     * 总实例数
     */
    @Schema(description = "总实例数", example = "100")
    private Long totalInstances;

    /**
     * 运行中实例数
     */
    @Schema(description = "运行中实例数", example = "15")
    private Long runningInstances;

    /**
     * 已完成实例数
     */
    @Schema(description = "已完成实例数", example = "80")
    private Long completedInstances;

    /**
     * 已终止实例数
     */
    @Schema(description = "已终止实例数", example = "5")
    private Long terminatedInstances;

    /**
     * 完成率（百分比）
     */
    @Schema(description = "完成率（百分比）", example = "80.0")
    private Double completionRate;

    /**
     * 终止率（百分比）
     */
    @Schema(description = "终止率（百分比）", example = "5.0")
    private Double terminationRate;

    /**
     * 平均处理时长（毫秒）
     */
    @Schema(description = "平均处理时长（毫秒）", example = "3600000")
    private Long averageProcessingTime;

    /**
     * 最小处理时长（毫秒）
     */
    @Schema(description = "最小处理时长（毫秒）", example = "600000")
    private Long minProcessingTime;

    /**
     * 最大处理时长（毫秒）
     */
    @Schema(description = "最大处理时长（毫秒）", example = "86400000")
    private Long maxProcessingTime;

    /**
     * 吞吐量（每天实例数）
     */
    @Schema(description = "吞吐量（每天实例数）", example = "10.5")
    private Double throughputPerDay;

    /**
     * 设置活跃实例数
     */
    public void setActiveInstances(Long activeInstances) {
        this.runningInstances = activeInstances;
    }

    /**
     * 设置挂起实例数
     */
    public void setSuspendedInstances(Long suspendedInstances) {
        // 使用新的字段存储，这里用 terminatedInstances 暂存
        this.terminatedInstances = suspendedInstances;
    }

    /**
     * 设置时间段内实例数
     */
    public void setPeriodInstances(Long periodInstances) {
        this.totalInstances = periodInstances;
    }

    /**
     * 设置时间段内已完成实例数
     */
    public void setPeriodCompletedInstances(Long periodCompletedInstances) {
        this.completedInstances = periodCompletedInstances;
    }

    /**
     * 设置平均时长（毫秒）
     */
    public void setAverageDurationInMillis(Long averageDurationInMillis) {
        this.averageProcessingTime = averageDurationInMillis;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置平均处理时长（兼容性方法）
     * @param averageDuration 平均处理时长（毫秒）
     */
    public void setAverageDuration(long averageDuration) {
        this.averageProcessingTime = averageDuration;
    }

    /**
     * 设置每个流程定义的实例数（兼容性方法）
     * @param instancesPerDefinition 实例数分布
     */
    public void setInstancesPerDefinition(java.util.Map<String, Long> instancesPerDefinition) {
        // 将Map转换为总数，设置到totalInstances
        if (instancesPerDefinition != null && !instancesPerDefinition.isEmpty()) {
            this.totalInstances = instancesPerDefinition.values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
        }
    }
}
