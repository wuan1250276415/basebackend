package com.basebackend.logging.statistics;

import com.basebackend.logging.statistics.aggregator.StatisticsAggregator;
import com.basebackend.logging.statistics.analyzer.PatternAnalyzer;
import com.basebackend.logging.statistics.analyzer.TimeSeriesAnalyzer;
import com.basebackend.logging.statistics.calculator.StatisticsCalculator;
import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import com.basebackend.logging.statistics.predictor.TrendPredictor;
import com.basebackend.logging.statistics.report.ReportGenerator;
import com.basebackend.logging.statistics.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * StatisticsService 单元测试
 * P0优化：增加测试覆盖率
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsService 统计服务测试")
class StatisticsServiceTest {

    @Mock
    private StatisticsCalculator calculator;

    @Mock
    private TimeSeriesAnalyzer timeSeriesAnalyzer;

    @Mock
    private PatternAnalyzer patternAnalyzer;

    @Mock
    private TrendPredictor predictor;

    @Mock
    private StatisticsAggregator aggregator;

    @Mock
    private ReportGenerator reportGenerator;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(
                calculator,
                timeSeriesAnalyzer,
                patternAnalyzer,
                predictor,
                aggregator,
                reportGenerator
        );
    }

    private List<LogStatisticsEntry> createTestData(int count) {
        List<LogStatisticsEntry> data = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            LogStatisticsEntry entry = LogStatisticsEntry.basic(
                    now.minus(i, ChronoUnit.HOURS),
                    now.minus(i - 1, ChronoUnit.HOURS),
                    100.0 + i * 10
            );
            data.add(entry);
        }
        return data;
    }

    @Nested
    @DisplayName("完整统计分析测试")
    class CompleteAnalysisTests {

        @Test
        @DisplayName("基础统计分析 - 应返回分析结果")
        void shouldPerformBasicAnalysis() throws ExecutionException, InterruptedException {
            List<LogStatisticsEntry> data = createTestData(10);
            StatisticsService.StatisticsQueryOptions options = new StatisticsService.StatisticsQueryOptions();
            options.setIncludeBasicStats(true);
            options.setIncludeTimeSeries(false);
            options.setIncludeAggregations(false);
            options.setIncludePredictions(false);
            options.setIncludeTopN(false);
            options.setIncludePercentiles(false);
            options.setGenerateReport(false);

            when(calculator.calculateBasicStatistics(any())).thenReturn(data.get(0));

            CompletableFuture<StatisticsService.StatisticsAnalysisResult> future =
                    statisticsService.performCompleteAnalysis(data, options);
            StatisticsService.StatisticsAnalysisResult result = future.get();

            assertThat(result).isNotNull();
            assertThat(result.getDataCount()).isEqualTo(10);
            assertThat(result.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("空数据分析 - 应正常处理")
        void shouldHandleEmptyData() throws ExecutionException, InterruptedException {
            List<LogStatisticsEntry> data = new ArrayList<>();
            StatisticsService.StatisticsQueryOptions options = new StatisticsService.StatisticsQueryOptions();
            options.setIncludeBasicStats(false);
            options.setIncludeTimeSeries(false);
            options.setIncludeAggregations(false);

            CompletableFuture<StatisticsService.StatisticsAnalysisResult> future =
                    statisticsService.performCompleteAnalysis(data, options);
            StatisticsService.StatisticsAnalysisResult result = future.get();

            assertThat(result).isNotNull();
            assertThat(result.getDataCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("时间范围查询测试")
    class TimeRangeQueryTests {

        @Test
        @DisplayName("有效时间范围 - 应返回过滤后的数据")
        void shouldQueryValidTimeRange() {
            List<LogStatisticsEntry> data = createTestData(10);
            Instant startTime = Instant.now().minus(5, ChronoUnit.HOURS);
            Instant endTime = Instant.now();

            LogStatisticsEntry result = statisticsService.queryTimeRangeStatistics(
                    data, startTime, endTime);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("空数据 - 应返回基础条目")
        void shouldHandleEmptyDataForTimeRange() {
            Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant endTime = Instant.now();

            LogStatisticsEntry result = statisticsService.queryTimeRangeStatistics(
                    new ArrayList<>(), startTime, endTime);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("null数据 - 应返回基础条目")
        void shouldHandleNullDataForTimeRange() {
            Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant endTime = Instant.now();

            LogStatisticsEntry result = statisticsService.queryTimeRangeStatistics(
                    null, startTime, endTime);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("实时摘要测试")
    class RealtimeSummaryTests {

        @Test
        @DisplayName("有数据 - 应返回摘要")
        void shouldGetRealtimeSummary() {
            List<LogStatisticsEntry> data = createTestData(5);

            StatisticsService.RealtimeStatisticsSummary summary =
                    statisticsService.getRealtimeSummary(data);

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalCount()).isGreaterThan(0);
            assertThat(summary.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("空数据 - 应返回零值摘要")
        void shouldHandleEmptyDataForSummary() {
            StatisticsService.RealtimeStatisticsSummary summary =
                    statisticsService.getRealtimeSummary(new ArrayList<>());

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("null数据 - 应返回零值摘要")
        void shouldHandleNullDataForSummary() {
            StatisticsService.RealtimeStatisticsSummary summary =
                    statisticsService.getRealtimeSummary(null);

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("单条数据 - 增长率应为0")
        void shouldHandleSingleDataPoint() {
            List<LogStatisticsEntry> data = createTestData(1);

            StatisticsService.RealtimeStatisticsSummary summary =
                    statisticsService.getRealtimeSummary(data);

            assertThat(summary).isNotNull();
            assertThat(summary.getGrowthRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("查询选项测试")
    class QueryOptionsTests {

        @Test
        @DisplayName("默认选项 - 应有合理默认值")
        void shouldHaveDefaultOptions() {
            StatisticsService.StatisticsQueryOptions options =
                    new StatisticsService.StatisticsQueryOptions();

            assertThat(options.isIncludeBasicStats()).isTrue();
            assertThat(options.isIncludeTimeSeries()).isTrue();
            assertThat(options.isIncludeAggregations()).isTrue();
            assertThat(options.isIncludePredictions()).isFalse();
            assertThat(options.isIncludeTopN()).isTrue();
            assertThat(options.isIncludePercentiles()).isFalse();
            assertThat(options.isGenerateReport()).isFalse();
            assertThat(options.getPredictionSteps()).isEqualTo(1);
            assertThat(options.getTopN()).isEqualTo(10);
            assertThat(options.getPercentile()).isEqualTo(95.0);
        }

        @Test
        @DisplayName("自定义选项 - 应正确设置")
        void shouldSetCustomOptions() {
            StatisticsService.StatisticsQueryOptions options =
                    new StatisticsService.StatisticsQueryOptions();
            options.setIncludeBasicStats(false);
            options.setIncludePredictions(true);
            options.setPredictionSteps(5);
            options.setTopN(20);
            options.setPercentile(99.0);

            assertThat(options.isIncludeBasicStats()).isFalse();
            assertThat(options.isIncludePredictions()).isTrue();
            assertThat(options.getPredictionSteps()).isEqualTo(5);
            assertThat(options.getTopN()).isEqualTo(20);
            assertThat(options.getPercentile()).isEqualTo(99.0);
        }
    }

    @Nested
    @DisplayName("分析结果Builder测试")
    class AnalysisResultBuilderTests {

        @Test
        @DisplayName("Builder - 应正确构建结果")
        void shouldBuildAnalysisResult() {
            Instant now = Instant.now();
            StatisticsService.StatisticsAnalysisResult result =
                    StatisticsService.StatisticsAnalysisResult.builder()
                            .timestamp(now)
                            .dataCount(100)
                            .build();

            assertThat(result.getTimestamp()).isEqualTo(now);
            assertThat(result.getDataCount()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("实时摘要Builder测试")
    class RealtimeSummaryBuilderTests {

        @Test
        @DisplayName("Builder - 应正确构建摘要")
        void shouldBuildRealtimeSummary() {
            Instant now = Instant.now();
            StatisticsService.RealtimeStatisticsSummary summary =
                    StatisticsService.RealtimeStatisticsSummary.builder()
                            .totalCount(1000.0)
                            .averageCount(100.0)
                            .growthRate(0.15)
                            .anomalyCount(5)
                            .timestamp(now)
                            .build();

            assertThat(summary.getTotalCount()).isEqualTo(1000.0);
            assertThat(summary.getAverageCount()).isEqualTo(100.0);
            assertThat(summary.getGrowthRate()).isEqualTo(0.15);
            assertThat(summary.getAnomalyCount()).isEqualTo(5);
            assertThat(summary.getTimestamp()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("分析异常 - 应包装为StatisticsAnalysisException")
        void shouldWrapExceptionInStatisticsAnalysisException() {
            StatisticsService.StatisticsAnalysisException exception =
                    new StatisticsService.StatisticsAnalysisException(
                            "测试异常", new RuntimeException("原因"));

            assertThat(exception.getMessage()).contains("测试异常");
            assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        }
    }
}
