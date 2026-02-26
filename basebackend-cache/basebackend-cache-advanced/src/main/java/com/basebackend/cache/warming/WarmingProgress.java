package com.basebackend.cache.warming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存预热进度报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarmingProgress {

    /**
     * 总任务数
     */
    private int totalTasks;

    /**
     * 已完成任务数
     */
    private int completedTasks;

    /**
     * 成功任务数
     */
    private int successTasks;

    /**
     * 失败任务数
     */
    private int failedTasks;

    /**
     * 总数据条目数
     */
    private int totalItems;

    /**
     * 已加载条目数
     */
    private int loadedItems;

    /**
     * 失败条目数
     */
    private int failedItems;

    /**
     * 开始时间（毫秒）
     */
    private long startTime;

    /**
     * 结束时间（毫秒）
     */
    private long endTime;

    /**
     * 任务详情列表
     */
    @Builder.Default
    private List<CacheWarmingTask> tasks = new ArrayList<>();

    /**
     * 获取完成百分比
     */
    public double getCompletionPercentage() {
        if (totalTasks > 0) {
            return (double) completedTasks / totalTasks * 100;
        }
        return 0.0;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (completedTasks > 0) {
            return (double) successTasks / completedTasks * 100;
        }
        return 0.0;
    }

    /**
     * 获取数据加载成功率
     */
    public double getItemSuccessRate() {
        if (totalItems > 0) {
            return (double) loadedItems / totalItems * 100;
        }
        return 0.0;
    }

    /**
     * 获取总耗时（毫秒）
     */
    public long getTotalExecutionTime() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }

    /**
     * 判断是否全部完成
     */
    public boolean isCompleted() {
        return completedTasks >= totalTasks;
    }

    /**
     * 判断是否全部成功
     */
    public boolean isAllSuccess() {
        return isCompleted() && successTasks == totalTasks;
    }
}
