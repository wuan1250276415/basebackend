package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 任务统计数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaskStatisticsDTO", description = "任务统计信息")
public class TaskStatisticsDTO {

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
     * 总任务数
     */
    @Schema(description = "总任务数", example = "200")
    private Long totalTasks;

    /**
     * 未完成任务数
     */
    @Schema(description = "未完成任务数", example = "30")
    private Long openTasks;

    /**
     * 已完成任务数
     */
    @Schema(description = "已完成任务数", example = "170")
    private Long completedTasks;

    /**
     * 逾期任务数
     */
    @Schema(description = "逾期任务数", example = "5")
    private Long overdueTasks;

    /**
     * 完成率（百分比）
     */
    @Schema(description = "完成率（百分比）", example = "85.0")
    private Double completionRate;

    /**
     * 逾期率（百分比）
     */
    @Schema(description = "逾期率（百分比）", example = "2.5")
    private Double overdueRate;

    /**
     * 平均任务处理时长（毫秒）
     */
    @Schema(description = "平均任务处理时长（毫秒）", example = "1800000")
    private Long averageTaskDuration;

    /**
     * 最小任务处理时长（毫秒）
     */
    @Schema(description = "最小任务处理时长（毫秒）", example = "300000")
    private Long minTaskDuration;

    /**
     * 最大任务处理时长（毫秒）
     */
    @Schema(description = "最大任务处理时长（毫秒）", example = "43200000")
    private Long maxTaskDuration;

    /**
     * 任务吞吐量（每天任务数）
     */
    @Schema(description = "任务吞吐量（每天任务数）", example = "15.5")
    private Double throughputPerDay;

    /**
     * 当前未办任务中各优先级分布
     */
    @Schema(description = "当前未办任务中各优先级分布")
    private Map<String, Long> priorityDistribution;

    /**
     * 当前未办任务中各分配人分布
     */
    @Schema(description = "当前未办任务中各分配人分布")
    private Map<String, Long> assigneeDistribution;

    /**
     * 已分配任务数
     */
    @Schema(description = "已分配任务数", example = "50")
    private Long assignedTasks;

    /**
     * 未分配任务数
     */
    @Schema(description = "未分配任务数", example = "20")
    private Long unassignedTasks;

    /**
     * 用户任务数
     */
    @Schema(description = "用户任务数", example = "30")
    private Long userTasks;

    /**
     * 用户已完成任务数
     */
    @Schema(description = "用户已完成任务数", example = "25")
    private Long userCompletedTasks;

    /**
     * 时间段任务数
     */
    @Schema(description = "时间段任务数", example = "40")
    private Long periodTasks;

    /**
     * 设置已分配任务数
     */
    public void setAssignedTasks(Long assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    /**
     * 设置未分配任务数
     */
    public void setUnassignedTasks(Long unassignedTasks) {
        this.unassignedTasks = unassignedTasks;
    }

    /**
     * 设置用户任务数
     */
    public void setUserTasks(Long userTasks) {
        this.userTasks = userTasks;
    }

    /**
     * 设置用户已完成任务数
     */
    public void setUserCompletedTasks(Long userCompletedTasks) {
        this.userCompletedTasks = userCompletedTasks;
    }

    /**
     * 设置时间段任务数
     */
    public void setPeriodTasks(Long periodTasks) {
        this.periodTasks = periodTasks;
    }

    /**
     * 时间段已完成任务数
     */
    @Schema(description = "时间段已完成任务数", example = "35")
    private Long periodCompletedTasks;

    /**
     * 设置时间段已完成任务数
     */
    public void setPeriodCompletedTasks(long periodCompletedTasks) {
        this.periodCompletedTasks = periodCompletedTasks;
    }

    /**
     * 平均处理时长（毫秒）
     */
    @Schema(description = "平均处理时长（毫秒）", example = "1800000")
    private Long averageDurationInMillis;

    /**
     * 设置平均处理时长（毫秒）
     */
    public void setAverageDurationInMillis(long averageDurationInMillis) {
        this.averageDurationInMillis = averageDurationInMillis;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置平均处理时长（兼容性方法）
     * @param averageDuration 平均处理时长（毫秒）
     */
    public void setAverageDuration(long averageDuration) {
        this.averageDurationInMillis = averageDuration;
    }

    /**
     * 设置每个分配人的任务数（兼容性方法）
     * @param tasksPerAssignee 任务分布
     */
    public void setTasksPerAssignee(java.util.Map<String, Long> tasksPerAssignee) {
        this.assigneeDistribution = tasksPerAssignee;
    }

    /**
     * 设置每个流程定义的任务数（兼容性方法）
     * @param tasksPerProcessDefinition 任务分布
     */
    public void setTasksPerProcessDefinition(java.util.Map<String, Long> tasksPerProcessDefinition) {
        // 将Map存储到priorityDistribution作为兼容性处理
        this.priorityDistribution = tasksPerProcessDefinition;
    }
}
