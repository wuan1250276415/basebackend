package com.basebackend.logging.statistics.service;

import com.basebackend.logging.statistics.aggregator.StatisticsAggregator;
import com.basebackend.logging.statistics.analyzer.PatternAnalyzer;
import com.basebackend.logging.statistics.analyzer.TimeSeriesAnalyzer;
import com.basebackend.logging.statistics.calculator.StatisticsCalculator;
import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import com.basebackend.logging.statistics.predictor.TrendPredictor;
import com.basebackend.logging.statistics.report.ReportGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 统计服务
 *
 * 提供统计分析的完整服务接口：
 * - 统计分析查询
 * - 趋势分析
 * - 模式识别
 * - 预测分析
 * - 报告生成
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class StatisticsService {

    private final StatisticsCalculator calculator;
    private final TimeSeriesAnalyzer timeSeriesAnalyzer;
    private final PatternAnalyzer patternAnalyzer;
    private final TrendPredictor predictor;
    private final StatisticsAggregator aggregator;
    private final ReportGenerator reportGenerator;

    public StatisticsService(StatisticsCalculator calculator,
                           TimeSeriesAnalyzer timeSeriesAnalyzer,
                           PatternAnalyzer patternAnalyzer,
                           TrendPredictor predictor,
                           StatisticsAggregator aggregator,
                           ReportGenerator reportGenerator) {
        this.calculator = calculator;
        this.timeSeriesAnalyzer = timeSeriesAnalyzer;
        this.patternAnalyzer = patternAnalyzer;
        this.predictor = predictor;
        this.aggregator = aggregator;
        this.reportGenerator = reportGenerator;
    }

    /**
     * 执行完整统计分析
     *
     * @param data 原始数据
     * @param queryOptions 查询选项
     * @return 完整分析结果
     */
    public CompletableFuture<StatisticsAnalysisResult> performCompleteAnalysis(
            List<LogStatisticsEntry> data,
            StatisticsQueryOptions queryOptions) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始完整统计分析, 数据量: {}", data.size());

            try {
                StatisticsAnalysisResult result = StatisticsAnalysisResult.builder()
                        .timestamp(Instant.now())
                        .dataCount(data.size())
                        .build();

                // 1. 基础统计分析
                if (queryOptions.isIncludeBasicStats()) {
                    log.debug("执行基础统计分析");
                    result.setBasicStatistics(calculator.calculateBasicStatistics(
                            data.stream()
                                    .map(LogStatisticsEntry::getCount)
                                    .collect(Collectors.toList())));
                }

                // 2. 时间序列分析
                if (queryOptions.isIncludeTimeSeries()) {
                    log.debug("执行时间序列分析");
                    Map<Instant, Double> timeSeries = buildTimeSeries(data);
                    result.setTrendAnalysis(timeSeriesAnalyzer.analyzeTrend(timeSeries));
                }

                // 3. 聚合分析
                if (queryOptions.isIncludeAggregations()) {
                    log.debug("执行聚合分析");
                    result.setTimeAggregation(aggregator.aggregateByTimeDimension(
                            data, StatisticsAggregator.TimeDimension.DAY));
                    result.setBusinessAggregation(aggregator.aggregateByBusinessDimension(
                            data, "level"));
                }

                // 4. 预测分析
                if (queryOptions.isIncludePredictions()) {
                    log.debug("执行预测分析");
                    Map<Long, Double> historicalData = buildHistoricalData(data);
                    result.setPrediction(predictor.predictComposite(historicalData, queryOptions.getPredictionSteps()));
                }

                // 5. Top-N 分析
                if (queryOptions.isIncludeTopN()) {
                    log.debug("执行 Top-N 分析");
                    result.setTopNAnalysis(aggregator.analyzeTopN(
                            data,
                            StatisticsAggregator.MetricField.COUNT,
                            queryOptions.getTopN(),
                            false));
                }

                // 6. 百分比聚合
                if (queryOptions.isIncludePercentiles()) {
                    log.debug("执行百分比聚合");
                    result.setPercentileAnalysis(aggregator.aggregatePercentile(
                            data, queryOptions.getPercentile()));
                }

                // 7. 报告生成
                if (queryOptions.isGenerateReport()) {
                    log.debug("生成报告");
                    ReportGenerator.ReportConfig reportConfig = new ReportGenerator.ReportConfig();
                    reportConfig.setTitle("统计分析报告");
                    result.setReport(reportGenerator.generateMultiFormatReports(data, reportConfig));
                }

                log.info("完整统计分析完成");
                return result;

            } catch (Exception e) {
                log.error("统计分析失败", e);
                throw new StatisticsAnalysisException("统计分析失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 查询特定时间范围的统计
     *
     * @param data 数据列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    public LogStatisticsEntry queryTimeRangeStatistics(
            List<LogStatisticsEntry> data,
            Instant startTime,
            Instant endTime) {
        if (data == null || data.isEmpty()) {
            return LogStatisticsEntry.basic(startTime, endTime, 0.0);
        }

        List<LogStatisticsEntry> filteredData = data.stream()
                .filter(entry -> {
                    Instant entryTime = entry.getStartTime();
                    return !entryTime.isBefore(startTime) && !entryTime.isAfter(endTime);
                })
                .collect(Collectors.toList());

        if (filteredData.isEmpty()) {
            return LogStatisticsEntry.basic(startTime, endTime, 0.0);
        }

        LogStatisticsEntry aggregated = filteredData.get(0);
        for (int i = 1; i < filteredData.size(); i++) {
            aggregated = aggregated.merge(filteredData.get(i));
        }

        log.debug("时间范围查询完成: {}-{}, count={}",
                startTime, endTime, filteredData.size());

        return aggregated;
    }

    /**
     * 获取实时统计摘要
     *
     * @param data 最新数据
     * @return 实时摘要
     */
    public RealtimeStatisticsSummary getRealtimeSummary(List<LogStatisticsEntry> data) {
        if (data == null || data.isEmpty()) {
            return RealtimeStatisticsSummary.builder()
                    .totalCount(0)
                    .timestamp(Instant.now())
                    .build();
        }

        double totalCount = data.stream()
                .mapToDouble(LogStatisticsEntry::getCount)
                .sum();

        double avgCount = totalCount / data.size();

        // 计算增长率
        double growthRate = data.size() >= 2 ?
                calculateGrowthRate(data) : 0.0;

        // 检测异常
        long anomalyCount = data.stream()
                .mapToLong(LogStatisticsEntry::getAnomalyCount)
                .sum();

        // 识别趋势
        LogStatisticsEntry.TrendType trendType = data.get(data.size() - 1).getTrendType();

        log.debug("实时摘要生成完成: count={}, growth={}%",
                totalCount, growthRate * 100);

        return RealtimeStatisticsSummary.builder()
                .totalCount(totalCount)
                .averageCount(avgCount)
                .growthRate(growthRate)
                .anomalyCount(anomalyCount)
                .trendType(trendType)
                .timestamp(Instant.now())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    private Map<Instant, Double> buildTimeSeries(List<LogStatisticsEntry> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        LogStatisticsEntry::getStartTime,
                        LogStatisticsEntry::getCount,
                        (v1, v2) -> v1 + v2,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, Double> buildHistoricalData(List<LogStatisticsEntry> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        entry -> entry.getStartTime().toEpochMilli(),
                        LogStatisticsEntry::getCount,
                        (v1, v2) -> v1 + v2,
                        LinkedHashMap::new
                ));
    }

    private double calculateGrowthRate(List<LogStatisticsEntry> data) {
        if (data.size() < 2) return 0.0;

        double recent = data.get(data.size() - 1).getCount();
        double previous = data.get(data.size() - 2).getCount();

        if (previous == 0) return 0.0;
        return (recent - previous) / previous;
    }

    // ==================== 数据模型 ====================

    /**
     * 统计查询选项
     */
    public static class StatisticsQueryOptions {
        private boolean includeBasicStats = true;
        private boolean includeTimeSeries = true;
        private boolean includeAggregations = true;
        private boolean includePredictions = false;
        private boolean includeTopN = true;
        private boolean includePercentiles = false;
        private boolean generateReport = false;
        private int predictionSteps = 1;
        private int topN = 10;
        private double percentile = 95.0;

        // Getters and setters
        public boolean isIncludeBasicStats() { return includeBasicStats; }
        public void setIncludeBasicStats(boolean includeBasicStats) { this.includeBasicStats = includeBasicStats; }

        public boolean isIncludeTimeSeries() { return includeTimeSeries; }
        public void setIncludeTimeSeries(boolean includeTimeSeries) { this.includeTimeSeries = includeTimeSeries; }

        public boolean isIncludeAggregations() { return includeAggregations; }
        public void setIncludeAggregations(boolean includeAggregations) { this.includeAggregations = includeAggregations; }

        public boolean isIncludePredictions() { return includePredictions; }
        public void setIncludePredictions(boolean includePredictions) { this.includePredictions = includePredictions; }

        public boolean isIncludeTopN() { return includeTopN; }
        public void setIncludeTopN(boolean includeTopN) { this.includeTopN = includeTopN; }

        public boolean isIncludePercentiles() { return includePercentiles; }
        public void setIncludePercentiles(boolean includePercentiles) { this.includePercentiles = includePercentiles; }

        public boolean isGenerateReport() { return generateReport; }
        public void setGenerateReport(boolean generateReport) { this.generateReport = generateReport; }

        public int getPredictionSteps() { return predictionSteps; }
        public void setPredictionSteps(int predictionSteps) { this.predictionSteps = predictionSteps; }

        public int getTopN() { return topN; }
        public void setTopN(int topN) { this.topN = topN; }

        public double getPercentile() { return percentile; }
        public void setPercentile(double percentile) { this.percentile = percentile; }
    }

    /**
     * 完整分析结果
     */
    @lombok.Data
    public static class StatisticsAnalysisResult {
        private Instant timestamp;
        private int dataCount;
        private LogStatisticsEntry basicStatistics;
        private TimeSeriesAnalyzer.TrendAnalysisResult trendAnalysis;
        private StatisticsAggregator.AggregationResult timeAggregation;
        private StatisticsAggregator.AggregationResult businessAggregation;
        private TrendPredictor.CompositePredictionResult prediction;
        private StatisticsAggregator.TopNResult topNAnalysis;
        private StatisticsAggregator.PercentileAggregationResult percentileAnalysis;
        private ReportGenerator.MultiFormatReportResult report;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private StatisticsAnalysisResult result = new StatisticsAnalysisResult();

            public Builder timestamp(Instant timestamp) {
                result.timestamp = timestamp;
                return this;
            }

            public Builder dataCount(int count) {
                result.dataCount = count;
                return this;
            }

            public Builder basicStatistics(LogStatisticsEntry stats) {
                result.basicStatistics = stats;
                return this;
            }

            public Builder trendAnalysis(TimeSeriesAnalyzer.TrendAnalysisResult analysis) {
                result.trendAnalysis = analysis;
                return this;
            }

            public Builder timeAggregation(StatisticsAggregator.AggregationResult aggregation) {
                result.timeAggregation = aggregation;
                return this;
            }

            public Builder businessAggregation(StatisticsAggregator.AggregationResult aggregation) {
                result.businessAggregation = aggregation;
                return this;
            }

            public Builder prediction(TrendPredictor.CompositePredictionResult prediction) {
                result.prediction = prediction;
                return this;
            }

            public Builder topNAnalysis(StatisticsAggregator.TopNResult analysis) {
                result.topNAnalysis = analysis;
                return this;
            }

            public Builder percentileAnalysis(StatisticsAggregator.PercentileAggregationResult analysis) {
                result.percentileAnalysis = analysis;
                return this;
            }

            public Builder report(ReportGenerator.MultiFormatReportResult report) {
                result.report = report;
                return this;
            }

            public StatisticsAnalysisResult build() {
                return result;
            }
        }
    }

    /**
     * 实时统计摘要
     * 
     * P0优化：添加getter方法以便外部访问
     */
    public static class RealtimeStatisticsSummary {
        private double totalCount;
        private double averageCount;
        private double growthRate;
        private long anomalyCount;
        private LogStatisticsEntry.TrendType trendType;
        private Instant timestamp;
        
        // P0优化：添加getter方法
        public double getTotalCount() { return totalCount; }
        public double getAverageCount() { return averageCount; }
        public double getGrowthRate() { return growthRate; }
        public long getAnomalyCount() { return anomalyCount; }
        public LogStatisticsEntry.TrendType getTrendType() { return trendType; }
        public Instant getTimestamp() { return timestamp; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RealtimeStatisticsSummary summary = new RealtimeStatisticsSummary();

            public Builder totalCount(double count) {
                summary.totalCount = count;
                return this;
            }

            public Builder averageCount(double count) {
                summary.averageCount = count;
                return this;
            }

            public Builder growthRate(double rate) {
                summary.growthRate = rate;
                return this;
            }

            public Builder anomalyCount(long count) {
                summary.anomalyCount = count;
                return this;
            }

            public Builder trendType(LogStatisticsEntry.TrendType type) {
                summary.trendType = type;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                summary.timestamp = timestamp;
                return this;
            }

            public RealtimeStatisticsSummary build() {
                return summary;
            }
        }
    }

    /**
     * 统计分析异常
     */
    public static class StatisticsAnalysisException extends RuntimeException {
        public StatisticsAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
