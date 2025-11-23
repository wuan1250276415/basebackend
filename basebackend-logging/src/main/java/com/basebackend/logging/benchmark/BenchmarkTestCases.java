package com.basebackend.logging.benchmark;

import com.basebackend.logging.statistics.aggregator.StatisticsAggregator;
import com.basebackend.logging.statistics.calculator.StatisticsCalculator;
import com.basebackend.logging.statistics.model.LogStatisticsEntry;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基准测试用例集合
 *
 * 包含日志系统各组件的性能测试用例：
 * - 异步批量写入器测试
 * - 统计计算器测试
 * - 统计分析引擎测试
 * - 缓存系统测试
 * - 模式分析器测试
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class BenchmarkTestCases {

    /**
     * 模拟日志写入测试
     */
    public static class LogWritingTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final List<String> testLogs;

        public LogWritingTest(int logCount) {
            this.testLogs = generateTestLogs(logCount);
        }

        @Override
        public String getName() {
            return "日志写入测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            // 模拟日志写入
            String log = testLogs.get(requestId % testLogs.size());
            // 模拟写入耗时 (1-5ms)
            Thread.sleep(1, 500000);
        }
    }

    /**
     * 统计计算器测试
     */
    public static class StatisticsCalculationTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final StatisticsCalculator calculator;
        private final List<List<Double>> testDataSets;

        public StatisticsCalculationTest(StatisticsCalculator calculator, int dataSetSize, int datasets) {
            this.calculator = calculator;
            this.testDataSets = generateTestDataSets(dataSetSize, datasets);
        }

        @Override
        public String getName() {
            return "统计计算器测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            List<Double> data = testDataSets.get(requestId % testDataSets.size());
            calculator.calculateBasicStatistics(data);
            calculator.calculatePercentiles(data, new double[]{50, 95, 99});
            calculator.detectAnomalies(data, 100.0, 10.0, 3.0);
        }
    }

    /**
     * 统计分析引擎测试
     */
    public static class StatisticsAnalysisTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final StatisticsAggregator aggregator;
        private final List<LogStatisticsEntry> testData;

        public StatisticsAnalysisTest(StatisticsAggregator aggregator, int dataSize) {
            this.aggregator = aggregator;
            this.testData = generateTestStatistics(dataSize);
        }

        @Override
        public String getName() {
            return "统计分析引擎测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            aggregator.aggregateByTimeDimension(testData, StatisticsAggregator.TimeDimension.DAY);
            aggregator.analyzeTopN(testData, StatisticsAggregator.MetricField.COUNT, 10, false);
            aggregator.aggregatePercentile(testData, 95.0);
        }
    }

    /**
     * 缓存系统测试
     */
    public static class CacheSystemTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final Map<String, String> cache;
        private final List<String> keys;

        public CacheSystemTest(int keyCount) {
            this.cache = new ConcurrentCache();
            this.keys = generateTestKeys(keyCount);
            // 预热缓存
            keys.forEach(key -> cache.put(key, "test-value-" + key));
        }

        @Override
        public String getName() {
            return "缓存系统测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            String key = keys.get(requestId % keys.size());
            String value = cache.get(key);
            if (value != null) {
                // 模拟缓存命中处理
                Thread.sleep(0, 50000); // 50微秒
            } else {
                // 模拟缓存未命中
                Thread.sleep(1, 0); // 1毫秒
                cache.put(key, "test-value-" + key);
            }
        }
    }

    /**
     * 模式分析器测试
     */
    public static class PatternAnalysisTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final List<String> testLogs;
        private final Random random = new Random(42);

        public PatternAnalysisTest(int logCount) {
            this.testLogs = generateTestLogsWithPatterns(logCount);
        }

        @Override
        public String getName() {
            return "模式分析器测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            String log = testLogs.get(requestId % testLogs.size());

            // 模拟模式匹配
            if (log.contains("Exception")) {
                // 模拟异常处理
                Thread.sleep(2, 0);
            } else if (log.contains("ERROR")) {
                // 模拟错误处理
                Thread.sleep(1, 500000);
            } else {
                // 模拟普通日志处理
                Thread.sleep(0, 500000);
            }
        }
    }

    /**
     * 趋势预测测试
     */
    public static class TrendPredictionTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final Map<Long, Double> historicalData;

        public TrendPredictionTest(int dataPoints) {
            this.historicalData = generateTimeSeriesData(dataPoints);
        }

        @Override
        public String getName() {
            return "趋势预测测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            // 模拟趋势预测计算
            double sum = 0;
            for (Double value : historicalData.values()) {
                sum += value;
                // 模拟计算复杂度
                if (requestId % 10 == 0) {
                    Thread.sleep(0, 100000);
                }
            }
            double avg = sum / historicalData.size();
            // 模拟预测输出
        }
    }

    /**
     * 报告生成测试
     */
    public static class ReportGenerationTest implements PerformanceBenchmark.BenchmarkTestCase {
        private final List<LogStatisticsEntry> testData;

        public ReportGenerationTest(int dataSize) {
            this.testData = generateTestStatistics(dataSize);
        }

        @Override
        public String getName() {
            return "报告生成测试";
        }

        @Override
        public void execute(int requestId) throws Exception {
            // 模拟报告生成
            StringBuilder report = new StringBuilder();
            report.append("# 性能测试报告\n\n");
            report.append("## 摘要信息\n\n");
            report.append("- 总数据量: ").append(testData.size()).append("\n");
            report.append("- 平均值: ").append(testData.stream()
                    .mapToDouble(LogStatisticsEntry::getMean).average().orElse(0.0)).append("\n");
            report.append("- 总数: ").append(testData.stream()
                    .mapToDouble(LogStatisticsEntry::getCount).sum()).append("\n\n");

            // 模拟报告内容生成
            for (int i = 0; i < Math.min(100, testData.size()); i++) {
                LogStatisticsEntry entry = testData.get(i);
                report.append(String.format("| %s | %.2f | %.2f |\n",
                        entry.getStartTime(), entry.getCount(), entry.getMean()));
            }

            // 模拟报告写入耗时
            Thread.sleep(2, 0);
        }
    }

    // ==================== 私有辅助方法 ====================

    private static List<String> generateTestLogs(int count) {
        List<String> logs = new ArrayList<>();
        String[] levels = {"INFO", "DEBUG", "WARN", "ERROR"};
        String[] messages = {
            "User login successful",
            "Database query executed",
            "Cache miss detected",
            "Service timeout occurred",
            "Memory usage high",
            "Network connection established",
            "File uploaded successfully",
            "Invalid request received",
            "Processing completed",
            "System started"
        };

        for (int i = 0; i < count; i++) {
            String level = levels[i % levels.length];
            String message = messages[i % messages.length];
            logs.add(String.format("[%s] %s - Request #%d", level, message, i));
        }

        return logs;
    }

    private static List<List<Double>> generateTestDataSets(int dataSize, int datasets) {
        List<List<Double>> dataSets = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < datasets; i++) {
            List<Double> data = new ArrayList<>();
            for (int j = 0; j < dataSize; j++) {
                // 生成随机数，模拟实际数据
                double value = random.nextGaussian() * 50 + 100;
                data.add(value);
            }
            dataSets.add(data);
        }

        return dataSets;
    }

    private static List<LogStatisticsEntry> generateTestStatistics(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> LogStatisticsEntry.builder()
                        .startTime(Instant.now().minusSeconds(count - i))
                        .endTime(Instant.now().minusSeconds(count - i - 1))
                        .count(1000.0 + Math.random() * 500)
                        .mean(100.0 + Math.random() * 50)
                        .min(80.0 + Math.random() * 20)
                        .max(120.0 + Math.random() * 30)
                        .variance(Math.random() * 10)
                        .stdDev(Math.random() * 3)
                        .growthRate((Math.random() - 0.5) * 0.1)
                        .anomalyCount((int) (Math.random() * 5))
                        .anomalyRate(Math.random() * 0.1)
//                        .trendType(LogStatisticsEntry.TrendType.STABLE)
                        .build())
                .collect(Collectors.toList());
    }

    private static List<String> generateTestKeys(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "cache-key-" + i)
                .collect(Collectors.toList());
    }

    private static List<String> generateTestLogsWithPatterns(int count) {
        List<String> logs = new ArrayList<>();
        String[] patterns = {
            "INFO: Application started successfully",
            "DEBUG: Processing request #%d",
            "WARN: High memory usage detected: %d%%",
            "ERROR: NullPointerException occurred",
            "ERROR: Database connection failed",
            "ERROR: SQL injection attempt detected",
            "WARN: Request timeout: %dms",
            "ERROR: OutOfMemoryError: Java heap space",
            "INFO: User authentication successful",
            "DEBUG: Cache hit for key: %s"
        };

        for (int i = 0; i < count; i++) {
            int patternIndex = i % patterns.length;
            if (patternIndex >= 3) {
                // 模拟异常日志
                logs.add(String.format(patterns[patternIndex], i));
            } else {
                logs.add(String.format(patterns[patternIndex], i, i * 10));
            }
        }

        return logs;
    }

    private static Map<Long, Double> generateTimeSeriesData(int dataPoints) {
        Map<Long, Double> data = new HashMap<>();
        long currentTime = System.currentTimeMillis() - (dataPoints * 1000);

        for (int i = 0; i < dataPoints; i++) {
            long timestamp = currentTime + (i * 1000);
            // 生成带有趋势的时间序列数据
            double value = 100 + (i * 0.5) + (Math.random() - 0.5) * 20;
            data.put(timestamp, value);
        }

        return data;
    }

    // ==================== 简单缓存实现 ====================

    private static class ConcurrentCache extends ConcurrentHashMap<String, String> {
        // 简化的缓存实现
    }
}
