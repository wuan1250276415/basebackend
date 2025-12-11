package com.basebackend.featuretoggle.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 特性开关指标收集器
 * <p>
 * 使用Micrometer收集特性开关的各种指标，包括：
 * <ul>
 *   <li>特性开关调用次数</li>
 *   <li>特性开关启用/禁用次数</li>
 *   <li>特性开关响应时间</li>
 *   <li>特性开关命中率</li>
 *   <li>AB测试分组分配统计</li>
 *   <li>缓存命中率</li>
 *   <li>异常统计</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class FeatureToggleMetrics {

    // 手动添加 Logger 以解决 Lombok 注解处理问题
    private static final Logger log = LoggerFactory.getLogger(FeatureToggleMetrics.class);

    // 指标前缀
    private static final String METRICS_PREFIX = "feature_toggle";

    // 特性开关调用指标
    private final Counter totalCalls;
    private final Counter enabledCalls;
    private final Counter disabledCalls;
    private final Timer responseTime;

    // 缓存指标
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final AtomicLong currentCacheSize = new AtomicLong(0);

    // AB测试指标
    private final ConcurrentHashMap<String, Counter> abTestGroupCounters = new ConcurrentHashMap<>();

    // 异常指标
    private final Counter totalExceptions;
    private final ConcurrentHashMap<String, Counter> exceptionTypeCounters = new ConcurrentHashMap<>();

    // 按特性名称的指标
    private final ConcurrentHashMap<String, FeatureMetrics> featureMetrics = new ConcurrentHashMap<>();

    public FeatureToggleMetrics(MeterRegistry meterRegistry) {
        // 初始化总体指标
        this.totalCalls = Counter.builder(METRICS_PREFIX + ".total_calls")
                .description("Total number of feature toggle checks")
                .register(meterRegistry);

        this.enabledCalls = Counter.builder(METRICS_PREFIX + ".enabled_calls")
                .description("Number of times feature toggles returned enabled")
                .register(meterRegistry);

        this.disabledCalls = Counter.builder(METRICS_PREFIX + ".disabled_calls")
                .description("Number of times feature toggles returned disabled")
                .register(meterRegistry);

        this.responseTime = Timer.builder(METRICS_PREFIX + ".response_time")
                .description("Response time for feature toggle checks")
                .register(meterRegistry);

        // 缓存指标
        this.cacheHits = Counter.builder(METRICS_PREFIX + ".cache_hits")
                .description("Number of cache hits")
                .register(meterRegistry);

        this.cacheMisses = Counter.builder(METRICS_PREFIX + ".cache_misses")
                .description("Number of cache misses")
                .register(meterRegistry);

        // 注册缓存大小Gauge
        io.micrometer.core.instrument.Gauge.builder(
                        METRICS_PREFIX + ".cache_size",
                        this.currentCacheSize,
                        java.util.concurrent.atomic.AtomicLong::doubleValue)
                .description("Current cache size")
                .register(meterRegistry);

        // 异常指标
        this.totalExceptions = Counter.builder(METRICS_PREFIX + ".exceptions")
                .description("Total number of exceptions")
                .register(meterRegistry);

        log.info("Initialized FeatureToggleMetrics");
    }

    /**
     * 记录特性开关调用
     *
     * @param featureName 特性名称
     * @param enabled 是否启用
     * @param responseTimeMs 响应时间（毫秒）
     */
    public void recordCall(String featureName, boolean enabled, long responseTimeMs) {
        // 记录总体指标
        totalCalls.increment();
        if (enabled) {
            enabledCalls.increment();
        } else {
            disabledCalls.increment();
        }
        responseTime.record(responseTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        // 记录特性级指标
        getFeatureMetrics(featureName).recordCall(enabled, responseTimeMs);

        log.trace("Recorded metric for feature '{}': enabled={}, responseTime={}ms",
                featureName, enabled, responseTimeMs);
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    /**
     * 更新缓存大小
     *
     * @param size 缓存大小
     */
    public void updateCacheSize(long size) {
        currentCacheSize.set(size);
    }

    /**
     * 记录AB测试分组分配
     *
     * @param featureName 特性名称
     * @param groupName 分组名称
     */
    public void recordABTestAssignment(String featureName, String groupName) {
        String key = featureName + "." + groupName;
        abTestGroupCounters.computeIfAbsent(key, k ->
                Counter.builder(METRICS_PREFIX + ".ab_test_groups")
                        .description("AB test group assignments")
                        .tag("feature", featureName)
                        .tag("group", groupName)
                        .register(io.micrometer.core.instrument.Metrics.globalRegistry)
        ).increment();

        log.trace("Recorded AB test assignment: feature={}, group={}", featureName, groupName);
    }

    /**
     * 记录异常
     *
     * @param featureName 特性名称
     * @param exceptionType 异常类型
     * @param exceptionMessage 异常消息
     */
    public void recordException(String featureName, String exceptionType, String exceptionMessage) {
        // 记录总体异常
        totalExceptions.increment();

        // 记录异常类型
        exceptionTypeCounters.computeIfAbsent(exceptionType, k ->
                Counter.builder(METRICS_PREFIX + ".exceptions_by_type")
                        .description("Exceptions by type")
                        .tag("type", exceptionType)
                        .register(io.micrometer.core.instrument.Metrics.globalRegistry)
        ).increment();

        // 记录特性级异常
        getFeatureMetrics(featureName).recordException(exceptionType);

        log.debug("Recorded exception for feature '{}': type={}, message={}",
                featureName, exceptionType, exceptionMessage);
    }

    /**
     * 记录配置刷新
     *
     * @param featureName 特性名称
     * @param success 是否成功
     */
    public void recordConfigRefresh(String featureName, boolean success) {
        getFeatureMetrics(featureName).recordConfigRefresh(success);
        log.debug("Recorded config refresh for feature '{}': success={}", featureName, success);
    }

    /**
     * 获取特性级指标
     */
    private FeatureMetrics getFeatureMetrics(String featureName) {
        return featureMetrics.computeIfAbsent(featureName, FeatureMetrics::new);
    }

    /**
     * 获取指标统计
     *
     * @return 指标统计信息
     */
    public MetricsStatistics getStatistics() {
        return new MetricsStatistics(
                (long) totalCalls.count(),
                (long) enabledCalls.count(),
                (long) disabledCalls.count(),
                (long) cacheHits.count(),
                (long) cacheMisses.count(),
                currentCacheSize.get(),
                abTestGroupCounters.size(),
                featureMetrics.size()
        );
    }

    /**
     * 指标统计信息
     */
    public static class MetricsStatistics {
        private final long totalCalls;
        private final long enabledCalls;
        private final long disabledCalls;
        private final long cacheHits;
        private final long cacheMisses;
        private final long cacheSize;
        private final long abTestGroups;
        private final long featuresTracked;

        public MetricsStatistics(long totalCalls, long enabledCalls, long disabledCalls,
                                long cacheHits, long cacheMisses, long cacheSize,
                                long abTestGroups, long featuresTracked) {
            this.totalCalls = totalCalls;
            this.enabledCalls = enabledCalls;
            this.disabledCalls = disabledCalls;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cacheSize = cacheSize;
            this.abTestGroups = abTestGroups;
            this.featuresTracked = featuresTracked;
        }

        public double getEnabledRate() {
            return totalCalls > 0 ? (double) enabledCalls / totalCalls : 0.0;
        }

        public double getCacheHitRate() {
            long totalCacheRequests = cacheHits + cacheMisses;
            return totalCacheRequests > 0 ? (double) cacheHits / totalCacheRequests : 0.0;
        }

        @Override
        public String toString() {
            return String.format("MetricsStatistics{totalCalls=%d, enabledRate=%.2f%%, " +
                            "cacheHitRate=%.2f%%, cacheSize=%d, abTestGroups=%d, featuresTracked=%d}",
                    totalCalls, getEnabledRate() * 100, getCacheHitRate() * 100,
                    cacheSize, abTestGroups, featuresTracked);
        }
    }

    /**
     * 特性级指标
     */
    private static class FeatureMetrics {
        private final String featureName;
        private final Counter calls;
        private final Counter enabledCalls;
        private final Counter disabledCalls;
        private final Timer responseTime;
        private final Counter exceptions;
        private final Counter configRefreshes;
        private final AtomicLong lastRefreshTime = new AtomicLong(0);

        public FeatureMetrics(String featureName) {
            this.featureName = featureName;

            this.calls = Counter.builder(METRICS_PREFIX + ".feature_calls")
                    .description("Feature toggle calls by feature")
                    .tag("feature", featureName)
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry);

            this.enabledCalls = Counter.builder(METRICS_PREFIX + ".feature_enabled_calls")
                    .description("Feature toggle enabled calls by feature")
                    .tag("feature", featureName)
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry);

            this.disabledCalls = Counter.builder(METRICS_PREFIX + ".feature_disabled_calls")
                    .description("Feature toggle disabled calls by feature")
                    .tag("feature", featureName)
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry);

            this.responseTime = Timer.builder(METRICS_PREFIX + ".feature_response_time")
                    .description("Feature toggle response time by feature")
                    .tag("feature", featureName)
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry);

            this.exceptions = Counter.builder(METRICS_PREFIX + ".feature_exceptions")
                    .description("Feature toggle exceptions by feature")
                    .tag("feature", featureName)
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry);

            this.configRefreshes = Counter.builder(METRICS_PREFIX + ".config_refreshes")
                    .description("Config refreshes by feature")
                    .tag("feature", featureName)
                    .register(io.micrometer.core.instrument.Metrics.globalRegistry);
        }

        public void recordCall(boolean enabled, long responseTimeMs) {
            calls.increment();
            if (enabled) {
                enabledCalls.increment();
            } else {
                disabledCalls.increment();
            }
            responseTime.record(responseTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        public void recordException(String exceptionType) {
            exceptions.increment();
        }

        public void recordConfigRefresh(boolean success) {
            configRefreshes.increment();
            lastRefreshTime.set(System.currentTimeMillis());
        }
    }
}
