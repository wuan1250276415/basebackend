package com.basebackend.logging.statistics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

/**
 * 统计分析系统配置属性
 *
 * 从 application.yml 中读取统计分析系统相关配置。
 * 支持实时分析、历史查询、缓存、预测等参数。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Data
@Validated
@ConfigurationProperties(prefix = "basebackend.logging.statistics")
public class StatisticsProperties {

    /**
     * 是否启用统计分析功能
     */
    private boolean enabled = true;

    /**
     * 实时查询窗口（默认 5 分钟）
     */
    @NotNull
    private Duration realtimeWindow = Duration.ofMinutes(5);

    /**
     * 历史查询默认窗口（默认 24 小时）
     */
    @NotNull
    private Duration historicalWindow = Duration.ofHours(24);

    /**
     * 最大查询窗口（默认 30 天）
     */
    @NotNull
    private Duration maxWindow = Duration.ofDays(30);

    /**
     * LRU 缓存大小
     */
    @Min(100)
    private int cacheSize = 512;

    /**
     * 缓存 TTL（默认 10 分钟）
     */
    @NotNull
    private Duration cacheTtl = Duration.ofMinutes(10);

    /**
     * 是否预计算热门报告
     */
    private boolean preloadHotReports = true;

    /**
     * 默认百分位数精度
     */
    @Min(10)
    private int percentileScale = 100;

    /**
     * 趋势斜率阈值
     */
    @Min(0L)
    private double trendSlopeThreshold = 0.01;

    /**
     * 异常检测 Z-score 阈值
     */
    @Min(1L)
    private double anomalyZThreshold = 3.0;

    /**
     * 时间序列最大存储长度
     */
    @Min(1000)
    private int maxSeriesLength = 10000;

    /**
     * Top N 排名限制
     */
    @Min(5)
    private int topN = 20;

    /**
     * 报告生成配置
     */
    private ReportConfig report = new ReportConfig();

    /**
     * 数据源配置
     */
    private DataSourceConfig dataSource = new DataSourceConfig();

    /**
     * 分析配置
     */
    private AnalysisConfig analysis = new AnalysisConfig();

    /**
     * 性能配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 报告配置
     */
    @Data
    public static class ReportConfig {
        /**
         * 是否启用 PDF 报告
         */
        private boolean enablePdf = true;

        /**
         * 是否启用 Excel 报告
         */
        private boolean enableExcel = true;

        /**
         * 是否启用 HTML 报告
         */
        private boolean enableHtml = true;

        /**
         * 是否启用 JSON 报告
         */
        private boolean enableJson = true;

        /**
         * 报告模板路径
         */
        private String templatePath = "templates/reports/";

        /**
         * 报告输出目录
         */
        private String outputDir = "reports/";

        /**
         * 报告生成超时时间
         */
        @NotNull
        private Duration generationTimeout = Duration.ofMinutes(5);
    }

    /**
     * 数据源配置
     */
    @Data
    public static class DataSourceConfig {
        /**
         * 默认数据源名称
         */
        @NotBlank
        private String defaultSource = "elasticsearch";

        /**
         * 最大查询记录数
         */
        @Min(100)
        private int maxQueryRecords = 100000;

        /**
         * 查询超时时间
         */
        @NotNull
        private Duration queryTimeout = Duration.ofSeconds(30);

        /**
         * 批处理大小
         */
        @Min(100)
        private int batchSize = 1000;

        /**
         * 是否启用缓存查询结果
         */
        private boolean enableQueryCache = true;
    }

    /**
     * 分析配置
     */
    @Data
    public static class AnalysisConfig {
        /**
         * 趋势分析最小数据点
         */
        @Min(3)
        private int minTrendDataPoints = 10;

        /**
         * 周期性检测周期
         */
        private int seasonalityPeriod = 24; // 24小时

        /**
         * 异常检测窗口大小
         */
        @Min(10)
        private int anomalyDetectionWindow = 100;

        /**
         * 预测模型类型
         */
        @NotBlank
        private String predictionModel = "exponential_smoothing"; // linear, moving_average, exponential_smoothing

        /**
         * 预测步数
         */
        @Min(1)
        private int predictionSteps = 1;

        /**
         * 置信度（0.0-1.0）
         */
        @Min(0L)
        @NotNull
        private double confidenceLevel = 0.95;
    }

    /**
     * 性能配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 并行处理线程数
         */
        @Min(1)
        private int parallelThreads = Runtime.getRuntime().availableProcessors();

        /**
         * 是否启用异步处理
         */
        private boolean enableAsync = true;

        /**
         * 异步处理超时时间
         */
        @NotNull
        private Duration asyncTimeout = Duration.ofMinutes(10);

        /**
         * 是否启用内存优化
         */
        private boolean enableMemoryOptimization = true;

        /**
         * 内存使用阈值（百分比）
         */
        @Min(50)
        @NotNull
        private double memoryThreshold = 80.0;

        /**
         * 是否启用预聚合
         */
        private boolean enablePreAggregation = true;

        /**
         * 预聚合时间窗口
         */
        @NotNull
        private Duration preAggregationWindow = Duration.ofHours(1);
    }

    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (enabled) {
            if (realtimeWindow.compareTo(Duration.ZERO) <= 0) {
                throw new IllegalArgumentException("实时查询窗口必须大于0");
            }

            if (historicalWindow.compareTo(Duration.ZERO) <= 0) {
                throw new IllegalArgumentException("历史查询窗口必须大于0");
            }

            if (historicalWindow.compareTo(maxWindow) > 0) {
                throw new IllegalArgumentException("历史查询窗口不能超过最大查询窗口");
            }

            if (cacheTtl.compareTo(Duration.ZERO) <= 0) {
                throw new IllegalArgumentException("缓存TTL必须大于0");
            }

            if (trendSlopeThreshold < 0) {
                throw new IllegalArgumentException("趋势斜率阈值不能为负数");
            }

            if (anomalyZThreshold < 1.0) {
                throw new IllegalArgumentException("异常检测Z-score阈值必须大于等于1.0");
            }

            if (percentileScale < 10 || percentileScale > 1000) {
                throw new IllegalArgumentException("百分位数精度必须在10-1000之间");
            }

            // 验证报告配置
            if (report != null && report.getGenerationTimeout() != null) {
                if (report.getGenerationTimeout().compareTo(Duration.ofMinutes(1)) < 0) {
                    throw new IllegalArgumentException("报告生成超时时间不能少于1分钟");
                }
            }

            // 验证分析配置
            if (analysis != null) {
                if (analysis.getMinTrendDataPoints() < 3) {
                    throw new IllegalArgumentException("趋势分析最小数据点不能少于3");
                }

                if (analysis.getConfidenceLevel() < 0.5 || analysis.getConfidenceLevel() > 0.999) {
                    throw new IllegalArgumentException("置信度必须在0.5-0.999之间");
                }
            }

            // 验证性能配置
            if (performance != null) {
                if (performance.getParallelThreads() < 1) {
                    throw new IllegalArgumentException("并行处理线程数不能少于1");
                }

                if (performance.getMemoryThreshold() < 50 || performance.getMemoryThreshold() > 95) {
                    throw new IllegalArgumentException("内存使用阈值必须在50-95之间");
                }
            }
        }
    }
}
