package com.basebackend.logging.statistics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * 日志统计条目
 *
 * 代表一个时间桶内的统计结果，包含所有计算的指标。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogStatisticsEntry {

    /**
     * 时间窗口开始时间
     */
    private Instant startTime;

    /**
     * 时间窗口结束时间
     */
    private Instant endTime;

    /**
     * 统计维度（级别、模块、用户、业务、地理、设备等）
     */
    private Map<String, String> dimensions;

    /**
     * 日志总数
     */
    private double count;

    /**
     * 平均值
     */
    private double mean;

    /**
     * 中位数
     */
    private double median;

    /**
     * 方差
     */
    private double variance;

    /**
     * 标准差
     */
    private double stdDev;

    /**
     * 百分位数（p50、p95、p99等）
     */
    private Map<String, Double> percentiles;

    /**
     * 最小值
     */
    private double min;

    /**
     * 最大值
     */
    private double max;

    /**
     * 增长率（与前一个时间桶对比）
     */
    private double growthRate;

    /**
     * 变化率
     */
    private double changeRate;

    /**
     * 季节性指数
     */
    private double seasonalityIndex;

    /**
     * 异常点数量
     */
    private int anomalyCount;

    /**
     * 异常率（0.0-1.0）
     */
    private double anomalyRate;

    /**
     * 异常类型
     */
    private String anomalyType;

    /**
     * 趋势序列（时间戳 -> 值）
     */
    private Map<String, Double> trendSeries;

    /**
     * 预测值
     */
    private Double predictedNext;

    /**
     * 获取统计摘要信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("时间范围: ").append(startTime).append(" ~ ").append(endTime);
        sb.append(", 总数: ").append((long) count);
        sb.append(", 平均值: ").append(String.format("%.2f", mean));
        sb.append(", P95: ").append(getPercentileValue("P95"));
        sb.append(", 增长率: ").append(String.format("%.2f%%", growthRate * 100));
        return sb.toString();
    }

    /**
     * 获取指定百分位数值
     */
    public double getPercentileValue(String percentile) {
        if (percentiles == null) {
            return 0.0;
        }
        return percentiles.getOrDefault(percentile, 0.0);
    }

    /**
     * 检查是否为高增长趋势
     */
    public boolean isHighGrowth() {
        return growthRate > 0.2; // 增长率超过20%
    }

    /**
     * 检查是否为高异常率
     */
    public boolean isHighAnomalyRate() {
        return anomalyRate > 0.05; // 异常率超过5%
    }

    /**
     * 获取趋势类型
     */
    public TrendType getTrendType() {
        if (isHighGrowth()) {
            return TrendType.GROWING;
        } else if (growthRate < -0.2) {
            return TrendType.DECLINING;
        } else if (Math.abs(growthRate) < 0.05) {
            return TrendType.STABLE;
        } else {
            return TrendType.VARIABLE;
        }
    }

    /**
     * 获取安全等级
     */
    public SecurityLevel getSecurityLevel() {
        if (anomalyRate > 0.1 || "RISING".equals(anomalyType)) {
            return SecurityLevel.HIGH;
        } else if (anomalyRate > 0.05 || "FALLING".equals(anomalyType)) {
            return SecurityLevel.MEDIUM;
        } else {
            return SecurityLevel.LOW;
        }
    }

    /**
     * 获取统计完整性评分（0-100）
     */
    public int getCompletenessScore() {
        int score = 0;
        if (count > 0) score += 20;
        if (mean > 0) score += 20;
        if (percentiles != null && !percentiles.isEmpty()) score += 20;
        if (variance >= 0) score += 20;
        if (trendSeries != null && !trendSeries.isEmpty()) score += 20;
        return score;
    }

    /**
     * 趋势类型枚举
     */
    public enum TrendType {
        GROWING,    // 增长
        DECLINING,  // 下降
        STABLE,     // 稳定
        VARIABLE    // 波动
    }

    /**
     * 安全等级枚举
     */
    public enum SecurityLevel {
        LOW,    // 低风险
        MEDIUM, // 中风险
        HIGH    // 高风险
    }

    /**
     * 创建基本统计条目
     */
    public static LogStatisticsEntry basic(Instant startTime, Instant endTime, double count) {
        return LogStatisticsEntry.builder()
                .startTime(startTime)
                .endTime(endTime)
                .count(count)
                .mean(count)
                .median(count)
                .min(count)
                .max(count)
                .variance(0.0)
                .stdDev(0.0)
                .growthRate(0.0)
                .changeRate(0.0)
                .seasonalityIndex(0.0)
                .anomalyCount(0)
                .anomalyRate(0.0)
                .dimensions(Collections.emptyMap())
                .percentiles(Collections.emptyMap())
                .trendSeries(Collections.emptyMap())
                .build();
    }

    /**
     * 验证统计条目的有效性
     */
    public boolean isValid() {
        return startTime != null &&
               endTime != null &&
               !endTime.isBefore(startTime) &&
               count >= 0 &&
               min >= 0 &&
               max >= 0 &&
               anomalyRate >= 0.0 &&
               anomalyRate <= 1.0;
    }

    /**
     * 合并两个统计条目
     */
    public LogStatisticsEntry merge(LogStatisticsEntry other) {
        if (other == null) {
            return this;
        }

        double totalCount = this.count + other.count;
        if (totalCount == 0) {
            return this;
        }

        double mergedMean = (this.mean * this.count + other.mean * other.count) / totalCount;

        return LogStatisticsEntry.builder()
                .startTime(this.startTime.isBefore(other.startTime) ? this.startTime : other.startTime)
                .endTime(this.endTime.isAfter(other.endTime) ? this.endTime : other.endTime)
                .count(totalCount)
                .mean(mergedMean)
                .median((this.median + other.median) / 2)
                .variance((this.variance + other.variance) / 2)
                .stdDev(Math.sqrt((this.stdDev * this.stdDev + other.stdDev * other.stdDev) / 2))
                .min(Math.min(this.min, other.min))
                .max(Math.max(this.max, other.max))
                .growthRate((this.growthRate + other.growthRate) / 2)
                .changeRate((this.changeRate + other.changeRate) / 2)
                .seasonalityIndex((this.seasonalityIndex + other.seasonalityIndex) / 2)
                .anomalyCount(this.anomalyCount + other.anomalyCount)
                .anomalyRate((this.anomalyRate + other.anomalyRate) / 2)
                .anomalyType(this.anomalyType != null ? this.anomalyType : other.anomalyType)
                .build();
    }
}
