package com.basebackend.logging.statistics.calculator;

import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析计算器
 *
 * 提供完整的统计分析功能，包括：
 * - 基础统计指标计算（均值、中位数、方差、标准差）
 * - 百分位数计算（P50、P95、P99等）
 * - 异常值检测（Z-Score）
 * - 多维度统计分析
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class StatisticsCalculator {

    /**
     * 计算基础统计指标
     *
     * @param values 数值列表
     * @return 统计结果
     */
    public LogStatisticsEntry calculateBasicStatistics(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return LogStatisticsEntry.basic(Instant.now(), Instant.now(), 0.0);
        }

        int count = values.size();
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / count;

        // 计算中位数
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        double median = calculateMedian(sorted);

        // 计算方差和标准差
        double variance = calculateVariance(values, mean);
        double stdDev = Math.sqrt(variance);

        // 计算最小值和最大值
        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);

        LogStatisticsEntry entry = LogStatisticsEntry.builder()
                .count(count)
                .mean(mean)
                .median(median)
                .variance(variance)
                .stdDev(stdDev)
                .min(min)
                .max(max)
                .build();

        log.debug("基础统计计算完成: count={}, mean={}, median={}, stdDev={}",
                count, mean, median, stdDev);

        return entry;
    }

    /**
     * 计算百分位数
     *
     * @param values 数值列表
     * @param percentiles 要计算的百分位数列表 (如 50, 95, 99)
     * @return 百分位数结果映射
     */
    public Map<String, Double> calculatePercentiles(List<Double> values, double[] percentiles) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        Map<String, Double> result = new HashMap<>();

        for (double p : percentiles) {
            double value = calculatePercentile(sorted, p);
            result.put("P" + (int) p, value);
        }

        log.debug("百分位数计算完成: {}", result);
        return result;
    }

    /**
     * 检测异常值（基于 Z-Score）
     *
     * @param values 数值列表
     * @param mean 平均值
     * @param stdDev 标准差
     * @param zThreshold Z-Score 阈值 (默认 3.0)
     * @return 异常值检测结果
     */
    public AnomalyDetectionResult detectAnomalies(List<Double> values, double mean,
                                                  double stdDev, double zThreshold) {
        if (values == null || values.isEmpty() || stdDev == 0) {
            return AnomalyDetectionResult.builder()
                    .anomalyCount(0)
                    .anomalyRate(0.0)
                    .anomalies(Collections.emptyList())
                    .build();
        }

        List<AnomalyPoint> anomalies = new ArrayList<>();
        int anomalyCount = 0;

        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            double zScore = Math.abs((value - mean) / stdDev);

            if (zScore > zThreshold) {
                anomalies.add(AnomalyPoint.builder()
                        .index(i)
                        .value(value)
                        .zScore(zScore)
                        .severity(zScore > zThreshold * 1.5 ? "HIGH" : "MEDIUM")
                        .build());
                anomalyCount++;
            }
        }

        double anomalyRate = (double) anomalyCount / values.size();

        log.debug("异常值检测完成: total={}, anomalies={}, rate={}%",
                values.size(), anomalyCount, anomalyRate * 100);

        return AnomalyDetectionResult.builder()
                .anomalyCount(anomalyCount)
                .anomalyRate(anomalyRate)
                .anomalies(anomalies)
                .build();
    }

    /**
     * 计算多维度统计聚合
     *
     * @param dataMap 数据映射 (维度 -> 值列表)
     * @return 聚合结果
     */
    public Map<String, LogStatisticsEntry> calculateMultiDimensionalStats(
            Map<String, List<Double>> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, LogStatisticsEntry> results = new HashMap<>();

        for (Map.Entry<String, List<Double>> entry : dataMap.entrySet()) {
            String dimension = entry.getKey();
            List<Double> values = entry.getValue();

            LogStatisticsEntry stats = calculateBasicStatistics(values);
            results.put(dimension, stats);
        }

        log.debug("多维度统计聚合完成: {} 个维度", results.size());
        return results;
    }

    /**
     * 计算中位数
     */
    private double calculateMedian(List<Double> sortedValues) {
        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }

    /**
     * 计算方差
     */
    private double calculateVariance(List<Double> values, double mean) {
        return values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
    }

    /**
     * 计算指定百分位数
     */
    private double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }

        int size = sortedValues.size();
        double index = (percentile / 100.0) * (size - 1);

        if (index == Math.floor(index)) {
            return sortedValues.get((int) index);
        } else {
            int lower = (int) Math.floor(index);
            int upper = (int) Math.ceil(index);
            double weight = index - lower;
            return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
        }
    }

    /**
     * 异常点
     */
    public static class AnomalyPoint {
        private int index;
        private double value;
        private double zScore;
        private String severity; // LOW, MEDIUM, HIGH

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AnomalyPoint point = new AnomalyPoint();

            public Builder index(int index) {
                point.index = index;
                return this;
            }

            public Builder value(double value) {
                point.value = value;
                return this;
            }

            public Builder zScore(double zScore) {
                point.zScore = zScore;
                return this;
            }

            public Builder severity(String severity) {
                point.severity = severity;
                return this;
            }

            public AnomalyPoint build() {
                return point;
            }
        }
    }

    /**
     * 异常检测结果
     */
    public static class AnomalyDetectionResult {
        private int anomalyCount;
        private double anomalyRate;
        private List<AnomalyPoint> anomalies;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AnomalyDetectionResult result = new AnomalyDetectionResult();

            public Builder anomalyCount(int count) {
                result.anomalyCount = count;
                return this;
            }

            public Builder anomalyRate(double rate) {
                result.anomalyRate = rate;
                return this;
            }

            public Builder anomalies(List<AnomalyPoint> anomalies) {
                result.anomalies = anomalies;
                return this;
            }

            public AnomalyDetectionResult build() {
                return result;
            }
        }
    }
}
