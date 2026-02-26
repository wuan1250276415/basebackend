package com.basebackend.observability.metrics;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义指标定义
 * 提供业务相关的指标采集能力
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomMetrics {

    private final MeterRegistry meterRegistry;

    // API 调用计数器
    private Counter apiCallCounter;

    // API 错误计数器
    private Counter apiErrorCounter;

    // API 响应时间分布
    private Timer apiResponseTimer;

    // 当前活跃请求数
    private AtomicLong activeRequests = new AtomicLong(0);

    // 业务操作计数器
    private Counter businessOperationCounter;

    /**
     * 记录 API 调用
     */
    public void recordApiCall(String method, String uri, String status) {
        Counter.builder("api_calls_total")
                .description("Total API calls")
                .tag("method", method)
                .tag("uri", sanitizeUri(uri))
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 平均相应时间
     */
    public void apiRequestTime(String method, String uri, String status) {
        Timer timer = Timer.builder("api_response_time")
                .publishPercentiles(0.5, 0.9, 0.99)
                .tag("method", method)
                .tag("uri", sanitizeUri(uri))
                .tag("status", status)
                .register(meterRegistry);
    }

    /**
     * 记录 API 响应时间
     */
    public void recordApiResponseTime(String method, String uri, long durationMs) {
        Timer.builder("api_response_time_seconds")
                .description("API response time in seconds")
                .tag("method", method)
                .tag("uri", sanitizeUri(uri))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * 记录 API 错误
     */
    public void recordApiError(String method, String uri, String errorType) {
        Counter.builder("api_errors_total")
                .description("Total API errors")
                .tag("method", method)
                .tag("uri", sanitizeUri(uri))
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 增加活跃请求数
     */
    public void incrementActiveRequests() {
        long current = activeRequests.incrementAndGet();
        Gauge.builder("api_active_requests", activeRequests, AtomicLong::get)
                .description("Number of active API requests")
                .register(meterRegistry);
    }

    /**
     * 减少活跃请求数
     */
    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }

    /**
     * 记录业务操作
     */
    public void recordBusinessOperation(String operationType, String status) {
        Counter.builder("business_operations_total")
                .description("Total business operations")
                .tag("operation_type", operationType)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录缓存命中/未命中
     */
    public void recordCacheHit(String cacheName, boolean hit) {
        Counter.builder("cache_access_total")
                .description("Cache access count")
                .tag("cache", cacheName)
                .tag("result", hit ? "hit" : "miss")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录数据库操作
     */
    public void recordDatabaseOperation(String operation, String table, long durationMs) {
        Timer.builder("database_operation_time_seconds")
                .description("Database operation time in seconds")
                .tag("operation", operation)
                .tag("table", table)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * 记录队列大小
     */
    public void recordQueueSize(String queueName, int size) {
        Gauge.builder("queue_size", () -> size)
                .description("Queue size")
                .tag("queue", queueName)
                .register(meterRegistry);
    }

    /**
     * 记录自定义计数器
     */
    public void incrementCounter(String name, String... tags) {
        Counter.Builder builder = Counter.builder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        builder.register(meterRegistry).increment();
    }

    /**
     * 记录自定义指标值
     */
    public void recordGauge(String name, Number value, String... tags) {
        Gauge.Builder<?> builder = Gauge.builder(name, value, Number::doubleValue);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        builder.register(meterRegistry);
    }

    /**
     * 清理 URI，移除路径参数
     * 例如：/api/users/123 -> /api/users/{id}
     */
    private String sanitizeUri(String uri) {
        if (uri == null) {
            return "unknown";
        }

        // 简单的路径参数替换（可以根据实际情况优化）
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "/{uuid}");
    }
}
