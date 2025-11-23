package com.basebackend.logging.statistics.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模式分析器
 *
 * 提供日志模式识别和分析功能：
 * - 错误模式识别
 * - 访问模式分析
 * - 用户行为模式检测
 * - 频率分布分析
 * - Top-N 统计分析
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class PatternAnalyzer {

    // 常见错误模式
    private static final Pattern NULL_POINTER_PATTERN = Pattern.compile("(?i)(null.*pointer|npe|nullpoint)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i)(sql.*injection|select.*union|drop.*table)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(?i)(timeout|connection.*refused|timed.*out)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OUT_OF_MEMORY_PATTERN = Pattern.compile("(?i)(out.*of.*memory|oom|heap.*space)",
            Pattern.CASE_INSENSITIVE);

    /**
     * 识别错误模式
     *
     * @param logs 日志列表
     * @return 错误模式分析结果
     */
    public ErrorPatternResult analyzeErrorPatterns(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            return ErrorPatternResult.builder()
                    .totalCount(0)
                    .patterns(Collections.emptyMap())
                    .topErrors(Collections.emptyList())
                    .build();
        }

        Map<ErrorType, Integer> patternCounts = new HashMap<>();
        Map<String, Integer> errorFrequency = new HashMap<>();
        int totalErrors = 0;

        for (String log : logs) {
            if (log == null) continue;

            ErrorType detectedType = detectErrorType(log);
            if (detectedType != ErrorType.NONE) {
                patternCounts.put(detectedType,
                        patternCounts.getOrDefault(detectedType, 0) + 1);
                totalErrors++;
            }

            // 提取错误信息用于频率分析
            String errorKey = extractErrorKey(log);
            if (errorKey != null) {
                errorFrequency.put(errorKey,
                        errorFrequency.getOrDefault(errorKey, 0) + 1);
            }
        }

        // 获取 Top 10 错误
        List<ErrorFrequency> topErrors = errorFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> ErrorFrequency.builder()
                        .errorPattern(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        log.debug("错误模式分析完成: 总数={}, 模式数={}, Top错误数={}",
                totalErrors, patternCounts.size(), topErrors.size());

        return ErrorPatternResult.builder()
                .totalCount(totalErrors)
                .patterns(patternCounts)
                .topErrors(topErrors)
                .build();
    }

    /**
     * 分析访问模式
     *
     * @param accessLogs 访问日志列表
     * @return 访问模式分析结果
     */
    public AccessPatternResult analyzeAccessPatterns(List<AccessLog> accessLogs) {
        if (accessLogs == null || accessLogs.isEmpty()) {
            return AccessPatternResult.builder()
                    .totalRequests(0)
                    .uniqueUsers(0)
                    .hourlyDistribution(Collections.emptyMap())
                    .endpointStats(Collections.emptyMap())
                    .userActivity(Collections.emptyMap())
                    .build();
        }

        // 统计每小时分布
        Map<Integer, Integer> hourlyDistribution = accessLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).getHour(),
                        Collectors.reducing(0, e -> 1, Integer::sum)));

        // 端点统计
        Map<String, EndpointStats> endpointStats = accessLogs.stream()
                .collect(Collectors.groupingBy(
                        AccessLog::getEndpoint,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                this::calculateEndpointStats)));

        // 用户活跃度
        Map<String, Integer> userActivity = accessLogs.stream()
                .filter(log -> log.getUserId() != null)
                .collect(Collectors.groupingBy(
                        AccessLog::getUserId,
                        Collectors.reducing(0, e -> 1, Integer::sum)));

        log.debug("访问模式分析完成: 请求数={}, 用户数={}, 端点数={}",
                accessLogs.size(), userActivity.size(), endpointStats.size());

        return AccessPatternResult.builder()
                .totalRequests(accessLogs.size())
                .uniqueUsers(userActivity.size())
                .hourlyDistribution(hourlyDistribution)
                .endpointStats(endpointStats)
                .userActivity(userActivity)
                .build();
    }

    /**
     * 检测频率分布
     *
     * @param values 数值列表
     * @param bucketCount 分桶数量
     * @return 频率分布结果
     */
    public FrequencyDistributionResult analyzeFrequencyDistribution(
            List<Double> values, int bucketCount) {
        if (values == null || values.isEmpty() || bucketCount <= 0) {
            return FrequencyDistributionResult.builder()
                    .buckets(Collections.emptyMap())
                    .build();
        }

        double min = values.stream().min(Double::compare).orElse(0.0);
        double max = values.stream().max(Double::compare).orElse(1.0);
        double range = max - min;
        double bucketSize = range / bucketCount;

        Map<String, Integer> buckets = new HashMap<>();
        for (int i = 0; i < bucketCount; i++) {
            double bucketStart = min + i * bucketSize;
            double bucketEnd = i == bucketCount - 1 ? max : min + (i + 1) * bucketSize;
            String bucketKey = String.format("%.2f-%.2f", bucketStart, bucketEnd);
            buckets.put(bucketKey, 0);
        }

        for (double value : values) {
            int bucketIndex = (int) Math.min(
                    bucketCount - 1,
                    Math.max(0, (value - min) / bucketSize));
            double bucketStart = min + bucketIndex * bucketSize;
            double bucketEnd = bucketIndex == bucketCount - 1 ?
                    max : min + (bucketIndex + 1) * bucketSize;
            String bucketKey = String.format("%.2f-%.2f", bucketStart, bucketEnd);

            buckets.put(bucketKey, buckets.getOrDefault(bucketKey, 0) + 1);
        }

        log.debug("频率分布分析完成: {} 个分桶", buckets.size());
        return FrequencyDistributionResult.builder()
                .buckets(buckets)
                .min(min)
                .max(max)
                .bucketSize(bucketSize)
                .build();
    }

    /**
     * 检测周期性模式
     *
     * @param timestamps 时间戳列表
     * @return 周期性模式结果
     */
    public CyclicPatternResult detectCyclicPatterns(List<Long> timestamps) {
        if (timestamps == null || timestamps.size() < 3) {
            return CyclicPatternResult.builder()
                    .detected(false)
                    .build();
        }

        List<Long> sortedTimestamps = timestamps.stream()
                .sorted()
                .collect(Collectors.toList());

        // 计算时间间隔
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < sortedTimestamps.size(); i++) {
            intervals.add(sortedTimestamps.get(i) - sortedTimestamps.get(i - 1));
        }

        // 寻找最频繁的间隔
        Map<Long, Integer> intervalFrequency = intervals.stream()
                .collect(Collectors.groupingBy(
                        interval -> interval,
                        Collectors.reducing(0, e -> 1, Integer::sum)));

        long mostFrequentInterval = intervalFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0L);

        int frequency = intervalFrequency.getOrDefault(mostFrequentInterval, 0);
        double confidence = (double) frequency / intervals.size();

        boolean detected = confidence > 0.3; // 30%以上认为是周期性

        log.debug("周期性模式检测完成: detected={}, interval={}, confidence={}%",
                detected, mostFrequentInterval, confidence * 100);

        return CyclicPatternResult.builder()
                .detected(detected)
                .interval(mostFrequentInterval)
                .frequency(frequency)
                .confidence(confidence)
                .totalEvents(timestamps.size())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    private ErrorType detectErrorType(String log) {
        if (log == null) return ErrorType.NONE;

        Matcher matcher = NULL_POINTER_PATTERN.matcher(log);
        if (matcher.find()) return ErrorType.NULL_POINTER;

        matcher = SQL_INJECTION_PATTERN.matcher(log);
        if (matcher.find()) return ErrorType.SQL_INJECTION;

        matcher = TIMEOUT_PATTERN.matcher(log);
        if (matcher.find()) return ErrorType.TIMEOUT;

        matcher = OUT_OF_MEMORY_PATTERN.matcher(log);
        if (matcher.find()) return ErrorType.OUT_OF_MEMORY;

        return ErrorType.OTHER;
    }

    private String extractErrorKey(String log) {
        // 简化：错误类型 + 关键信息
        if (log.contains("Exception")) {
            int start = log.indexOf("Exception");
            int end = Math.min(log.length(), start + 50);
            return log.substring(start, end);
        }
        return log.substring(0, Math.min(50, log.length()));
    }

    private EndpointStats calculateEndpointStats(List<AccessLog> logs) {
        long successCount = logs.stream()
                .filter(log -> log.getStatusCode() >= 200 && log.getStatusCode() < 300)
                .count();

        long errorCount = logs.size() - successCount;
        double avgResponseTime = logs.stream()
                .mapToLong(AccessLog::getResponseTime)
                .average()
                .orElse(0.0);

        return EndpointStats.builder()
                .requestCount(logs.size())
                .successCount(successCount)
                .errorCount(errorCount)
                .errorRate(logs.isEmpty() ? 0.0 : (double) errorCount / logs.size())
                .avgResponseTime(avgResponseTime)
                .build();
    }

    // ==================== 数据模型 ====================

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        NONE,              // 无错误
        NULL_POINTER,      // 空指针
        SQL_INJECTION,     // SQL注入
        TIMEOUT,           // 超时
        OUT_OF_MEMORY,     // 内存溢出
        OTHER              // 其他
    }

    /**
     * 访问日志数据模型
     */
    public static class AccessLog {
        private String userId;
        private String endpoint;
        private int statusCode;
        private long responseTime;
        private java.time.Instant timestamp;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

        public java.time.Instant getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.Instant timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 错误模式分析结果
     */
    public static class ErrorPatternResult {
        private int totalCount;
        private Map<ErrorType, Integer> patterns;
        private List<ErrorFrequency> topErrors;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ErrorPatternResult result = new ErrorPatternResult();

            public Builder totalCount(int count) {
                result.totalCount = count;
                return this;
            }

            public Builder patterns(Map<ErrorType, Integer> patterns) {
                result.patterns = patterns;
                return this;
            }

            public Builder topErrors(List<ErrorFrequency> topErrors) {
                result.topErrors = topErrors;
                return this;
            }

            public ErrorPatternResult build() {
                return result;
            }
        }
    }

    /**
     * 访问模式分析结果
     */
    public static class AccessPatternResult {
        private int totalRequests;
        private int uniqueUsers;
        private Map<Integer, Integer> hourlyDistribution; // 小时 -> 请求数
        private Map<String, EndpointStats> endpointStats;
        private Map<String, Integer> userActivity;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AccessPatternResult result = new AccessPatternResult();

            public Builder totalRequests(int requests) {
                result.totalRequests = requests;
                return this;
            }

            public Builder uniqueUsers(int users) {
                result.uniqueUsers = users;
                return this;
            }

            public Builder hourlyDistribution(Map<Integer, Integer> distribution) {
                result.hourlyDistribution = distribution;
                return this;
            }

            public Builder endpointStats(Map<String, EndpointStats> stats) {
                result.endpointStats = stats;
                return this;
            }

            public Builder userActivity(Map<String, Integer> activity) {
                result.userActivity = activity;
                return this;
            }

            public AccessPatternResult build() {
                return result;
            }
        }
    }

    /**
     * 端点统计
     */
    public static class EndpointStats {
        private long requestCount;
        private long successCount;
        private long errorCount;
        private double errorRate;
        private double avgResponseTime;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private EndpointStats stats = new EndpointStats();

            public Builder requestCount(long count) {
                stats.requestCount = count;
                return this;
            }

            public Builder successCount(long count) {
                stats.successCount = count;
                return this;
            }

            public Builder errorCount(long count) {
                stats.errorCount = count;
                return this;
            }

            public Builder errorRate(double rate) {
                stats.errorRate = rate;
                return this;
            }

            public Builder avgResponseTime(double time) {
                stats.avgResponseTime = time;
                return this;
            }

            public EndpointStats build() {
                return stats;
            }
        }
    }

    /**
     * 错误频率
     */
    public static class ErrorFrequency {
        private String errorPattern;
        private int count;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ErrorFrequency frequency = new ErrorFrequency();

            public Builder errorPattern(String pattern) {
                frequency.errorPattern = pattern;
                return this;
            }

            public Builder count(int count) {
                frequency.count = count;
                return this;
            }

            public ErrorFrequency build() {
                return frequency;
            }
        }
    }

    /**
     * 频率分布结果
     */
    public static class FrequencyDistributionResult {
        private Map<String, Integer> buckets;
        private double min;
        private double max;
        private double bucketSize;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FrequencyDistributionResult result = new FrequencyDistributionResult();

            public Builder buckets(Map<String, Integer> buckets) {
                result.buckets = buckets;
                return this;
            }

            public Builder min(double min) {
                result.min = min;
                return this;
            }

            public Builder max(double max) {
                result.max = max;
                return this;
            }

            public Builder bucketSize(double size) {
                result.bucketSize = size;
                return this;
            }

            public FrequencyDistributionResult build() {
                return result;
            }
        }
    }

    /**
     * 周期性模式结果
     */
    public static class CyclicPatternResult {
        private boolean detected;
        private long interval;    // 间隔 (毫秒)
        private int frequency;    // 频率
        private double confidence; // 置信度
        private int totalEvents;  // 总事件数

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CyclicPatternResult result = new CyclicPatternResult();

            public Builder detected(boolean detected) {
                result.detected = detected;
                return this;
            }

            public Builder interval(long interval) {
                result.interval = interval;
                return this;
            }

            public Builder frequency(int frequency) {
                result.frequency = frequency;
                return this;
            }

            public Builder confidence(double confidence) {
                result.confidence = confidence;
                return this;
            }

            public Builder totalEvents(int events) {
                result.totalEvents = events;
                return this;
            }

            public CyclicPatternResult build() {
                return result;
            }
        }
    }
}
