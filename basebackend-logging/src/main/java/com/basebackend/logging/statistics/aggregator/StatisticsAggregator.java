package com.basebackend.logging.statistics.aggregator;

import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计聚合器
 *
 * 提供多维度数据聚合功能：
 * - 时间维度聚合 (小时、日、周、月)
 * - 业务维度聚合 (模块、用户、级别)
 * - Top-N 分析
 * - 百分比聚合
 * - 分组统计
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class StatisticsAggregator {

    /**
     * 按时间维度聚合数据
     *
     * @param entries       统计条目列表
     * @param timeDimension 时间维度 (HOUR, DAY, WEEK, MONTH)
     * @return 聚合结果
     */
    public AggregationResult aggregateByTimeDimension(
            List<LogStatisticsEntry> entries, TimeDimension timeDimension) {
        if (entries == null || entries.isEmpty()) {
            return createEmptyAggregation("时间维度");
        }

        Map<String, List<LogStatisticsEntry>> groupedData = entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    Instant time = entry.getStartTime();
                    return getTimeKey(time, timeDimension);
                }));

        Map<String, LogStatisticsEntry> aggregatedResults = new HashMap<>();

        for (Map.Entry<String, List<LogStatisticsEntry>> group : groupedData.entrySet()) {
            String timeKey = group.getKey();
            List<LogStatisticsEntry> groupEntries = group.getValue();

            LogStatisticsEntry aggregated = mergeEntries(groupEntries);
            aggregatedResults.put(timeKey, aggregated);
        }

        log.debug("时间维度聚合完成: dimension={}, groups={}",
                timeDimension, aggregatedResults.size());

        return AggregationResult.builder()
                .dimension("时间")
                .keyField(timeDimension.name())
                .groups(aggregatedResults)
                .build();
    }

    /**
     * 按业务维度聚合数据
     *
     * @param entries        统计条目列表
     * @param dimensionField 维度字段名 (如 "level", "module", "userId")
     * @return 聚合结果
     */
    public AggregationResult aggregateByBusinessDimension(
            List<LogStatisticsEntry> entries, String dimensionField) {
        if (entries == null || entries.isEmpty()) {
            return createEmptyAggregation("业务维度");
        }

        Map<String, List<LogStatisticsEntry>> groupedData = entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    Map<String, String> dimensions = entry.getDimensions();
                    return dimensions != null ? dimensions.getOrDefault(dimensionField, "UNKNOWN") : "UNKNOWN";
                }));

        Map<String, LogStatisticsEntry> aggregatedResults = new HashMap<>();

        for (Map.Entry<String, List<LogStatisticsEntry>> group : groupedData.entrySet()) {
            String dimensionValue = group.getKey();
            List<LogStatisticsEntry> groupEntries = group.getValue();

            LogStatisticsEntry aggregated = mergeEntries(groupEntries);
            aggregatedResults.put(dimensionValue, aggregated);
        }

        log.debug("业务维度聚合完成: field={}, groups={}",
                dimensionField, aggregatedResults.size());

        return AggregationResult.builder()
                .dimension("业务")
                .keyField(dimensionField)
                .groups(aggregatedResults)
                .build();
    }

    /**
     * Top-N 分析
     *
     * @param entries     统计条目列表
     * @param metricField 度量字段 (COUNT, MEAN, ERROR_RATE等)
     * @param topN        Top N 的数量
     * @param ascending   是否升序排列
     * @return Top-N 结果
     */
    public TopNResult analyzeTopN(
            List<LogStatisticsEntry> entries,
            MetricField metricField,
            int topN,
            boolean ascending) {
        if (entries == null || entries.isEmpty()) {
            return TopNResult.builder()
                    .metricField(metricField)
                    .results(Collections.emptyList())
                    .build();
        }

        List<TopNItem> items = entries.stream()
                .map(entry -> {
                    double value = getMetricValue(entry, metricField);
                    String dimension = extractDimensionKey(entry);
                    return TopNItem.builder()
                            .dimension(dimension)
                            .value(value)
                            .entry(entry)
                            .build();
                })
                .sorted((a, b) -> {
                    int cmp = Double.compare(a.getValue(), b.getValue());
                    return ascending ? cmp : -cmp;
                })
                .limit(topN)
                .collect(Collectors.toList());

        log.debug("Top-N 分析完成: metric={}, top={}, ascending={}",
                metricField, topN, ascending);

        return TopNResult.builder()
                .metricField(metricField)
                .results(items)
                .build();
    }

    /**
     * 计算百分比聚合
     *
     * @param entries    统计条目列表
     * @param percentile 百分位数 (0-100)
     * @return 百分比聚合结果
     */
    public PercentileAggregationResult aggregatePercentile(
            List<LogStatisticsEntry> entries, double percentile) {
        if (entries == null || entries.isEmpty()) {
            return PercentileAggregationResult.builder()
                    .percentile(percentile)
                    .value(0.0)
                    .count(0)
                    .build();
        }

        List<Double> values = entries.stream()
                .mapToDouble(LogStatisticsEntry::getCount)
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        double percentileValue = calculatePercentile(values, percentile);
        double averageValue = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        log.debug("百分比聚合完成: percentile={}%, value={}",
                percentile, percentileValue);

        return PercentileAggregationResult.builder()
                .percentile(percentile)
                .value(percentileValue)
                .average(averageValue)
                .count(values.size())
                .build();
    }

    /**
     * 多维度交叉聚合
     *
     * @param entries    统计条目列表
     * @param dimensions 维度列表
     * @return 交叉聚合结果
     */
    public CrossAggregationResult aggregateByMultipleDimensions(
            List<LogStatisticsEntry> entries, List<String> dimensions) {
        if (entries == null || entries.isEmpty() || dimensions == null || dimensions.isEmpty()) {
            return CrossAggregationResult.builder()
                    .dimensions(dimensions)
                    .groups(Collections.emptyMap())
                    .build();
        }

        // 创建复合维度键
        Map<String, List<LogStatisticsEntry>> groupedData = entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    Map<String, String> entryDimensions = entry.getDimensions();
                    List<String> values = dimensions.stream()
                            .map(dim -> entryDimensions != null ? entryDimensions.getOrDefault(dim, "UNKNOWN")
                                    : "UNKNOWN")
                            .collect(Collectors.toList());
                    return String.join("|", values);
                }));

        Map<String, LogStatisticsEntry> aggregatedResults = new HashMap<>();
        Map<String, Map<String, String>> groupMetadata = new HashMap<>();

        for (Map.Entry<String, List<LogStatisticsEntry>> group : groupedData.entrySet()) {
            String compositeKey = group.getKey();
            List<LogStatisticsEntry> groupEntries = group.getValue();

            LogStatisticsEntry aggregated = mergeEntries(groupEntries);
            aggregatedResults.put(compositeKey, aggregated);

            // 提取维度值
            String[] dimensionValues = compositeKey.split("\\|");
            Map<String, String> dimensionMap = new HashMap<>();
            for (int i = 0; i < dimensions.size() && i < dimensionValues.length; i++) {
                dimensionMap.put(dimensions.get(i), dimensionValues[i]);
            }
            groupMetadata.put(compositeKey, dimensionMap);
        }

        log.debug("多维度交叉聚合完成: dimensions={}, groups={}",
                dimensions, aggregatedResults.size());

        return CrossAggregationResult.builder()
                .dimensions(dimensions)
                .groups(aggregatedResults)
                .groupMetadata(groupMetadata)
                .build();
    }

    /**
     * 计算聚合统计摘要
     *
     * @param result 聚合结果
     * @return 统计摘要
     */
    public AggregationSummary calculateAggregationSummary(AggregationResult result) {
        if (result == null || result.getGroups().isEmpty()) {
            return AggregationSummary.builder()
                    .totalGroups(0)
                    .totalCount(0)
                    .build();
        }

        Map<String, LogStatisticsEntry> groups = result.getGroups();
        double totalCount = groups.values().stream()
                .mapToDouble(LogStatisticsEntry::getCount)
                .sum();

        double totalMean = groups.values().stream()
                .mapToDouble(LogStatisticsEntry::getMean)
                .average()
                .orElse(0.0);

        int groupCount = groups.size();
        String topGroup = groups.entrySet().stream()
                .max(Map.Entry.comparingByValue(
                        Comparator.comparingDouble(LogStatisticsEntry::getCount)))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        log.debug("聚合摘要计算完成: groups={}, totalCount={}, topGroup={}",
                groupCount, totalCount, topGroup);

        return AggregationSummary.builder()
                .totalGroups(groupCount)
                .totalCount(totalCount)
                .averageValue(totalMean)
                .topGroup(topGroup)
                .dimension(result.getDimension())
                .keyField(result.getKeyField())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    private AggregationResult createEmptyAggregation(String type) {
        return AggregationResult.builder()
                .dimension(type)
                .keyField("N/A")
                .groups(Collections.emptyMap())
                .build();
    }

    private String getTimeKey(Instant time, TimeDimension dimension) {
        // 简化的时间键生成
        // 在实际应用中应使用更精确的时间格式化
        return switch (dimension) {
            case HOUR -> String.format("%d-%02d-%02d %02d:00",
                    time.getEpochSecond() / 86400, time.getEpochSecond() % 24, 0, 0);
            case DAY -> String.format("%d-%02d-%02d",
                    time.getEpochSecond() / 86400, time.getEpochSecond() % 24, 0);
            case WEEK -> String.format("%d-W%02d",
                    time.getEpochSecond() / (86400 * 7), (time.getEpochSecond() % (86400 * 7)) / 7);
            case MONTH -> String.format("%d-%02d",
                    time.getEpochSecond() / (86400 * 30), (time.getEpochSecond() % (86400 * 30)) / 30);
        };
    }

    private LogStatisticsEntry mergeEntries(List<LogStatisticsEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return LogStatisticsEntry.basic(Instant.now(), Instant.now(), 0.0);
        }

        if (entries.size() == 1) {
            return entries.get(0);
        }

        LogStatisticsEntry first = entries.get(0);
        LogStatisticsEntry merged = first;

        for (int i = 1; i < entries.size(); i++) {
            merged = merged.merge(entries.get(i));
        }

        return merged;
    }

    private String extractDimensionKey(LogStatisticsEntry entry) {
        Map<String, String> dimensions = entry.getDimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            return "ALL";
        }

        // 返回第一个维度作为键
        return dimensions.values().iterator().next();
    }

    private double getMetricValue(LogStatisticsEntry entry, MetricField field) {
        return switch (field) {
            case COUNT -> entry.getCount();
            case MEAN -> entry.getMean();
            case MEDIAN -> entry.getMedian();
            case VARIANCE -> entry.getVariance();
            case STD_DEV -> entry.getStdDev();
            case MIN -> entry.getMin();
            case MAX -> entry.getMax();
            case GROWTH_RATE -> entry.getGrowthRate();
            case ANOMALY_RATE -> entry.getAnomalyRate();
        };
    }

    private double calculatePercentile(List<Double> values, double percentile) {
        if (values.isEmpty())
            return 0.0;

        int size = values.size();
        double index = (percentile / 100.0) * (size - 1);

        if (index == Math.floor(index)) {
            return values.get((int) index);
        } else {
            int lower = (int) Math.floor(index);
            int upper = (int) Math.ceil(index);
            double weight = index - lower;
            return values.get(lower) * (1 - weight) + values.get(upper) * weight;
        }
    }

    // ==================== 数据模型和枚举 ====================

    /**
     * 时间维度枚举
     */
    public enum TimeDimension {
        HOUR, // 小时
        DAY, // 天
        WEEK, // 周
        MONTH // 月
    }

    /**
     * 度量字段枚举
     */
    public enum MetricField {
        COUNT, // 数量
        MEAN, // 平均值
        MEDIAN, // 中位数
        VARIANCE, // 方差
        STD_DEV, // 标准差
        MIN, // 最小值
        MAX, // 最大值
        GROWTH_RATE, // 增长率
        ANOMALY_RATE // 异常率
    }

    /**
     * 聚合结果
     */
    @lombok.Data
    public static class AggregationResult {
        private String dimension; // 维度名称
        private String keyField; // 键字段
        private Map<String, LogStatisticsEntry> groups; // 分组结果

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AggregationResult result = new AggregationResult();

            public Builder dimension(String dimension) {
                result.dimension = dimension;
                return this;
            }

            public Builder keyField(String keyField) {
                result.keyField = keyField;
                return this;
            }

            public Builder groups(Map<String, LogStatisticsEntry> groups) {
                result.groups = groups;
                return this;
            }

            public AggregationResult build() {
                return result;
            }
        }
    }

    /**
     * Top-N 结果
     */
    @lombok.Data
    public static class TopNResult {
        private MetricField metricField;
        private List<TopNItem> results;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private TopNResult result = new TopNResult();

            public Builder metricField(MetricField metricField) {
                result.metricField = metricField;
                return this;
            }

            public Builder results(List<TopNItem> results) {
                result.results = results;
                return this;
            }

            public TopNResult build() {
                return result;
            }
        }
    }

    /**
     * Top-N 项目
     */
    @lombok.Data
    public static class TopNItem {
        private String dimension;
        private double value;
        private LogStatisticsEntry entry;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private TopNItem item = new TopNItem();

            public Builder dimension(String dimension) {
                item.dimension = dimension;
                return this;
            }

            public Builder value(double value) {
                item.value = value;
                return this;
            }

            public Builder entry(LogStatisticsEntry entry) {
                item.entry = entry;
                return this;
            }

            public TopNItem build() {
                return item;
            }
        }
    }

    /**
     * 百分比聚合结果
     */
    @lombok.Data
    public static class PercentileAggregationResult {
        private double percentile;
        private double value;
        private double average;
        private int count;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PercentileAggregationResult result = new PercentileAggregationResult();

            public Builder percentile(double percentile) {
                result.percentile = percentile;
                return this;
            }

            public Builder value(double value) {
                result.value = value;
                return this;
            }

            public Builder average(double average) {
                result.average = average;
                return this;
            }

            public Builder count(int count) {
                result.count = count;
                return this;
            }

            public PercentileAggregationResult build() {
                return result;
            }
        }
    }

    /**
     * 交叉聚合结果
     */
    public static class CrossAggregationResult {
        private List<String> dimensions;
        private Map<String, LogStatisticsEntry> groups;
        private Map<String, Map<String, String>> groupMetadata;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CrossAggregationResult result = new CrossAggregationResult();

            public Builder dimensions(List<String> dimensions) {
                result.dimensions = dimensions;
                return this;
            }

            public Builder groups(Map<String, LogStatisticsEntry> groups) {
                result.groups = groups;
                return this;
            }

            public Builder groupMetadata(Map<String, Map<String, String>> metadata) {
                result.groupMetadata = metadata;
                return this;
            }

            public CrossAggregationResult build() {
                return result;
            }
        }
    }

    /**
     * 聚合摘要
     */
    public static class AggregationSummary {
        private int totalGroups;
        private double totalCount;
        private double averageValue;
        private String topGroup;
        private String dimension;
        private String keyField;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AggregationSummary summary = new AggregationSummary();

            public Builder totalGroups(int groups) {
                summary.totalGroups = groups;
                return this;
            }

            public Builder totalCount(double count) {
                summary.totalCount = count;
                return this;
            }

            public Builder averageValue(double value) {
                summary.averageValue = value;
                return this;
            }

            public Builder topGroup(String group) {
                summary.topGroup = group;
                return this;
            }

            public Builder dimension(String dimension) {
                summary.dimension = dimension;
                return this;
            }

            public Builder keyField(String keyField) {
                summary.keyField = keyField;
                return this;
            }

            public AggregationSummary build() {
                return summary;
            }
        }
    }
}
