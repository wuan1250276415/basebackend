package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 恢复统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreStatistics {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 总恢复次数
     */
    private int totalCount;

    /**
     * 成功次数
     */
    private int successCount;

    /**
     * 失败次数
     */
    private int failedCount;

    /**
     * 成功率（百分比）
     */
    private double successRate;

    /**
     * 统计周期（天）
     */
    private int periodDays;

    /**
     * 是否检查成功率是否达标
     */
    public boolean isSuccessRateAcceptable(double threshold) {
        return successRate >= threshold;
    }
}
