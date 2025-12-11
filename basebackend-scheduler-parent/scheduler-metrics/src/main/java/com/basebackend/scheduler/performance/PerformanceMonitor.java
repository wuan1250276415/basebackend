package com.basebackend.scheduler.performance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

/**
 * 性能监控器
 *
 * <p>提供全方位的系统性能监控：
 * <ul>
 *   <li>应用性能指标（响应时间、吞吐量）</li>
 *   <li>数据库性能指标（查询时间、连接数）</li>
 *   <li>缓存性能指标（命中率、响应时间）</li>
 *   <li>系统资源监控（CPU、内存、线程）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */

@Component
public class PerformanceMonitor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceMonitor.class);

    private final MeterRegistry meterRegistry;
    private final Cache<String, Timer> responseTimeMetrics;
    private final Cache<String, Counter> requestCountMetrics;
    private final Map<String, Long> customMetrics;

    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.responseTimeMetrics = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofMinutes(10))
                .recordStats()
                .build();

        this.requestCountMetrics = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofMinutes(10))
                .recordStats()
                .build();

        this.customMetrics = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        log.info("性能监控器初始化完成");
    }

    /**
     * 记录响应时间
     */
    public void recordResponseTime(String endpoint, long durationMs) {
        Timer timer = responseTimeMetrics.get(endpoint, this::createTimer);
        timer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录请求计数
     */
    public void incrementRequestCount(String endpoint) {
        Counter counter = requestCountMetrics.get(endpoint, this::createCounter);
        counter.increment();
    }

    /**
     * 记录自定义指标
     */
    public void recordCustomMetric(String metricName, long value) {
        customMetrics.merge(metricName, value, Long::sum);
        log.debug("记录自定义指标，name={}, value={}", metricName, value);
    }

    /**
     * 获取响应时间统计
     */
    public Map<String, Object> getResponseTimeStats(String endpoint) {
        Timer timer = responseTimeMetrics.asMap().get(endpoint);
        if (timer == null) {
            return Map.of("count", 0L, "totalTime", 0L, "avgTime", 0.0);
        }

        return Map.of(
            "count", timer.count(),
            "totalTime", timer.totalTime(TimeUnit.MILLISECONDS),
            "avgTime", timer.mean(TimeUnit.MILLISECONDS),
            "maxTime", timer.max(TimeUnit.MILLISECONDS)
        );
    }

    /**
     * 获取所有响应时间统计
     */
    public Map<String, Map<String, Object>> getAllResponseTimeStats() {
        Map<String, Map<String, Object>> stats = new ConcurrentHashMap<>();
        responseTimeMetrics.asMap().forEach((endpoint, timer) -> {
            stats.put(endpoint, getResponseTimeStats(endpoint));
        });
        return stats;
    }

    /**
     * 获取请求计数统计
     */
    public Map<String, Long> getRequestCountStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        requestCountMetrics.asMap().forEach((endpoint, counter) -> {
            stats.put(endpoint, (long) counter.count());
        });
        return stats;
    }

    /**
     * 获取自定义指标
     */
    public Map<String, Long> getCustomMetrics() {
        return new ConcurrentHashMap<>(customMetrics);
    }

    /**
     * 重置所有指标
     */
    public void resetAllMetrics() {
        responseTimeMetrics.invalidateAll();
        requestCountMetrics.invalidateAll();
        customMetrics.clear();
        log.info("所有性能指标已重置");
    }

    private Timer createTimer(String endpoint) {
        return Timer.builder("app.response.time")
                .description("Response time for " + endpoint)
                .tag("endpoint", endpoint)
                .register(meterRegistry);
    }

    private Counter createCounter(String endpoint) {
        return Counter.builder("app.request.count")
                .description("Request count for " + endpoint)
                .tag("endpoint", endpoint)
                .register(meterRegistry);
    }
}
