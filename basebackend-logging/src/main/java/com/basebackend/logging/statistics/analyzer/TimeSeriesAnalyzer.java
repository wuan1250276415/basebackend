package com.basebackend.logging.statistics.analyzer;

import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间序列分析器
 *
 * 提供时间序列数据的深度分析功能：
 * - 趋势识别和分类
 * - 季节性模式检测
 * - 周期性分析
 * - 时间窗口统计
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class TimeSeriesAnalyzer {

    /**
     * 分析时间序列趋势
     *
     * @param timeSeries 时间序列 (timestamp -> value)
     * @return 趋势分析结果
     */
    public TrendAnalysisResult analyzeTrend(Map<Instant, Double> timeSeries) {
        if (timeSeries == null || timeSeries.size() < 2) {
            return createEmptyTrendAnalysis();
        }

        List<Map.Entry<Instant, Double>> sortedEntries = timeSeries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        // 计算趋势斜率
        double slope = calculateSlope(sortedEntries);

        // 计算变化率
        List<Double> values = sortedEntries.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        double changeRate = calculateChangeRate(values);

        // 确定趋势类型
        LogStatisticsEntry.TrendType trendType = classifyTrend(slope, changeRate);

        // 检测季节性
        double seasonalityIndex = detectSeasonality(values);

        TrendAnalysisResult result = TrendAnalysisResult.builder()
                .slope(slope)
                .changeRate(changeRate)
                .trendType(trendType)
                .seasonalityIndex(seasonalityIndex)
                .volatility(calculateVolatility(values))
                .build();

        log.debug("时间序列趋势分析完成: type={}, slope={}, changeRate={}%",
                trendType, slope, changeRate * 100);

        return result;
    }

    /**
     * 检测季节性模式
     *
     * @param values 值序列
     * @param period 周期长度 (如 24 表示24小时周期)
     * @return 季节性指标 (0-1, 1表示强季节性)
     */
    public double detectSeasonality(List<Double> values, int period) {
        if (values == null || values.size() < period * 2) {
            return 0.0;
        }

        // 按周期分组计算平均值
        Map<Integer, List<Double>> cycleGroups = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            int cyclePos = i % period;
            cycleGroups.computeIfAbsent(cyclePos, k -> new ArrayList<>())
                    .add(values.get(i));
        }

        // 计算每个周期位置的均值
        List<Double> cycleMeans = cycleGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0))
                .collect(Collectors.toList());

        // 计算总体均值
        double overallMean = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // 计算季节性指数
        double variance = cycleMeans.stream()
                .mapToDouble(v -> Math.pow(v - overallMean, 2))
                .average()
                .orElse(0.0);

        double seasonalityIndex = Math.min(1.0, Math.sqrt(variance) / Math.abs(overallMean));

        log.debug("季节性检测完成: period={}, index={}", period, seasonalityIndex);
        return seasonalityIndex;
    }

    /**
     * 计算时间窗口统计
     *
     * @param timeSeries 时间序列
     * @param windowSize 窗口大小
     * @return 窗口统计结果列表
     */
    public List<LogStatisticsEntry> calculateWindowStatistics(
            Map<Instant, Double> timeSeries, Duration windowSize) {
        if (timeSeries == null || timeSeries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<Instant, Double>> sortedEntries = timeSeries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        List<LogStatisticsEntry> windows = new ArrayList<>();
        Instant startTime = sortedEntries.get(0).getKey();
        Instant windowEnd = startTime.plus(windowSize);

        List<Double> windowValues = new ArrayList<>();

        for (Map.Entry<Instant, Double> entry : sortedEntries) {
            Instant currentTime = entry.getKey();

            if (!currentTime.isBefore(windowEnd)) {
                // 完成当前窗口
                if (!windowValues.isEmpty()) {
                    LogStatisticsEntry windowStat = createWindowStatistics(
                            startTime, windowEnd, windowValues);
                    windows.add(windowStat);
                }

                // 开始新窗口
                startTime = windowEnd;
                windowEnd = startTime.plus(windowSize);
                windowValues.clear();
            }

            windowValues.add(entry.getValue());
        }

        // 添加最后一个窗口
        if (!windowValues.isEmpty()) {
            LogStatisticsEntry windowStat = createWindowStatistics(
                    startTime, windowEnd, windowValues);
            windows.add(windowStat);
        }

        log.debug("时间窗口统计完成: {} 个窗口", windows.size());
        return windows;
    }

    /**
     * 预测下一时刻的值
     *
     * @param timeSeries 历史时间序列
     * @param method     预测方法 (LINEAR, MOVING_AVERAGE, EXPONENTIAL_SMOOTHING)
     * @return 预测值
     */
    public double predictNextValue(Map<Instant, Double> timeSeries, PredictionMethod method) {
        if (timeSeries == null || timeSeries.size() < 2) {
            return 0.0;
        }

        List<Double> values = timeSeries.values().stream()
                .sorted()
                .collect(Collectors.toList());

        double prediction = switch (method) {
            case LINEAR -> predictLinear(values);
            case MOVING_AVERAGE -> predictMovingAverage(values, 3);
            case EXPONENTIAL_SMOOTHING -> predictExponentialSmoothing(values, 0.3);
        };

        log.debug("值预测完成: method={}, prediction={}", method, prediction);
        return prediction;
    }

    /**
     * 分析时间序列的稳定性
     *
     * @param values 值序列
     * @return 稳定性指标 (0-1, 1表示高稳定性)
     */
    public double analyzeStability(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }

        // 计算变异系数 (CV = stdDev / mean)
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0) {
            return 0.0;
        }

        double stdDev = Math.sqrt(values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0));

        double cv = stdDev / Math.abs(mean);

        // 转换为稳定性指标 (CV越小, 稳定性越高)
        double stability = Math.max(0, 1 - Math.min(1, cv));

        log.debug("稳定性分析完成: stability={}", stability);
        return stability;
    }

    // ==================== 私有辅助方法 ====================

    private TrendAnalysisResult createEmptyTrendAnalysis() {
        return TrendAnalysisResult.builder()
                .slope(0.0)
                .changeRate(0.0)
                .trendType(LogStatisticsEntry.TrendType.STABLE)
                .seasonalityIndex(0.0)
                .volatility(0.0)
                .build();
    }

    private double calculateSlope(List<Map.Entry<Instant, Double>> sortedEntries) {
        int n = sortedEntries.size();
        if (n < 2)
            return 0.0;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i; // 使用索引作为x值
            double y = sortedEntries.get(i).getValue();

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }

    private double calculateChangeRate(List<Double> values) {
        if (values.size() < 2)
            return 0.0;

        double first = values.get(0);
        double last = values.get(values.size() - 1);

        if (first == 0)
            return 0.0;
        return (last - first) / Math.abs(first);
    }

    private LogStatisticsEntry.TrendType classifyTrend(double slope, double changeRate) {
        if (slope > 0.01 || changeRate > 0.2) {
            return LogStatisticsEntry.TrendType.GROWING;
        } else if (slope < -0.01 || changeRate < -0.2) {
            return LogStatisticsEntry.TrendType.DECLINING;
        } else if (Math.abs(changeRate) < 0.05) {
            return LogStatisticsEntry.TrendType.STABLE;
        } else {
            return LogStatisticsEntry.TrendType.VARIABLE;
        }
    }

    private double detectSeasonality(List<Double> values) {
        // 简化的季节性检测
        if (values.size() < 24) {
            return 0.0;
        }
        return detectSeasonality(values, 24); // 假设24小时周期
    }

    private double calculateVolatility(List<Double> values) {
        if (values.size() < 2)
            return 0.0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    private LogStatisticsEntry createWindowStatistics(
            Instant start, Instant end, List<Double> values) {
        double count = values.size();
        double mean = values.stream().mapToDouble(Double::doubleValue).sum() / count;
        double min = values.stream().min(Double::compare).orElse(0.0);
        double max = values.stream().max(Double::compare).orElse(0.0);

        return LogStatisticsEntry.builder()
                .startTime(start)
                .endTime(end)
                .count(count)
                .mean(mean)
                .min(min)
                .max(max)
                .build();
    }

    private double predictLinear(List<Double> values) {
        if (values.size() < 2)
            return values.isEmpty() ? 0.0 : values.get(values.size() - 1);

        // 简单线性回归预测
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        return intercept + slope * n; // 预测下一个点
    }

    private double predictMovingAverage(List<Double> values, int windowSize) {
        if (values.isEmpty())
            return 0.0;
        if (values.size() < windowSize) {
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        int start = values.size() - windowSize;
        return values.subList(start, values.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double predictExponentialSmoothing(List<Double> values, double alpha) {
        if (values.isEmpty())
            return 0.0;
        if (values.size() == 1)
            return values.get(0);

        double smoothed = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            smoothed = alpha * values.get(i) + (1 - alpha) * smoothed;
        }

        return smoothed;
    }

    /**
     * 预测方法枚举
     */
    public enum PredictionMethod {
        LINEAR, // 线性回归
        MOVING_AVERAGE, // 移动平均
        EXPONENTIAL_SMOOTHING // 指数平滑
    }

    /**
     * 趋势分析结果
     */
    public static class TrendAnalysisResult {
        private double slope; // 趋势斜率
        private double changeRate; // 变化率
        private LogStatisticsEntry.TrendType trendType; // 趋势类型
        private double seasonalityIndex; // 季节性指数
        private double volatility; // 波动性

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private TrendAnalysisResult result = new TrendAnalysisResult();

            public Builder slope(double slope) {
                result.slope = slope;
                return this;
            }

            public Builder changeRate(double rate) {
                result.changeRate = rate;
                return this;
            }

            public Builder trendType(LogStatisticsEntry.TrendType type) {
                result.trendType = type;
                return this;
            }

            public Builder seasonalityIndex(double index) {
                result.seasonalityIndex = index;
                return this;
            }

            public Builder volatility(double volatility) {
                result.volatility = volatility;
                return this;
            }

            public TrendAnalysisResult build() {
                return result;
            }
        }
    }
}
