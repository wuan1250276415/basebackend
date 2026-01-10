package com.basebackend.logging.statistics.config;

import com.basebackend.logging.statistics.aggregator.StatisticsAggregator;
import com.basebackend.logging.statistics.analyzer.PatternAnalyzer;
import com.basebackend.logging.statistics.analyzer.TimeSeriesAnalyzer;
import com.basebackend.logging.statistics.cache.StatisticsCache;
import com.basebackend.logging.statistics.calculator.StatisticsCalculator;
import com.basebackend.logging.statistics.predictor.TrendPredictor;
import com.basebackend.logging.statistics.report.ReportGenerator;
import com.basebackend.logging.statistics.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统计分析系统自动配置
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Configuration
@EnableConfigurationProperties(StatisticsProperties.class)
@ConditionalOnProperty(name = "basebackend.logging.statistics.enabled", havingValue = "true", matchIfMissing = true)
public class StatisticsAutoConfiguration {

    /**
     * 配置统计缓存
     */
    @Bean(name = "loggingStatisticsCache")
    public StatisticsCache statisticsCache(StatisticsProperties properties) {
        return new StatisticsCache(properties.getCacheSize(), properties.getCacheTtl());
    }

    /**
     * 配置对象映射器
     */
    @Bean(name = "loggingObjectMapper")
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 配置统计计算器
     */
    @Bean(name = "loggingStatisticsCalculator")
    public StatisticsCalculator statisticsCalculator() {
        return new StatisticsCalculator();
    }

    /**
     * 配置时间序列分析器
     */
    @Bean(name = "loggingTimeSeriesAnalyzer")
    public TimeSeriesAnalyzer timeSeriesAnalyzer() {
        return new TimeSeriesAnalyzer();
    }

    /**
     * 配置模式分析器
     */
    @Bean(name = "loggingPatternAnalyzer")
    public PatternAnalyzer patternAnalyzer() {
        return new PatternAnalyzer();
    }

    /**
     * 配置趋势预测器
     */
    @Bean(name = "loggingTrendPredictor")
    public TrendPredictor trendPredictor() {
        return new TrendPredictor();
    }

    /**
     * 配置统计聚合器
     */
    @Bean(name = "loggingStatisticsAggregator")
    public StatisticsAggregator statisticsAggregator() {
        return new StatisticsAggregator();
    }

    /**
     * 配置报告生成器
     */
    @Bean(name = "loggingReportGenerator")
    public ReportGenerator reportGenerator(ObjectMapper objectMapper,
            StatisticsAggregator aggregator,
            PatternAnalyzer patternAnalyzer) {
        return new ReportGenerator(objectMapper, aggregator, patternAnalyzer);
    }

    /**
     * 配置统计服务
     */
    @Bean(name = "loggingStatisticsService")
    public StatisticsService statisticsService(StatisticsCalculator calculator,
            TimeSeriesAnalyzer timeSeriesAnalyzer,
            PatternAnalyzer patternAnalyzer,
            TrendPredictor predictor,
            StatisticsAggregator aggregator,
            ReportGenerator reportGenerator) {
        return new StatisticsService(calculator, timeSeriesAnalyzer, patternAnalyzer,
                predictor, aggregator, reportGenerator);
    }
}
