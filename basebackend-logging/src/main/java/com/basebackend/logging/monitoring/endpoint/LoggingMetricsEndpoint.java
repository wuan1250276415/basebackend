package com.basebackend.logging.monitoring.endpoint;

import com.basebackend.logging.monitoring.collector.CustomMetricsCollector;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志指标端点
 *
 * 提供 /actuator/logging-metrics 端点，
 * 返回聚合的日志系统监控指标。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
@Endpoint(id = "logging-metrics")
public class LoggingMetricsEndpoint {

    private final MeterRegistry registry;
    private final CustomMetricsCollector metricsCollector;

    public LoggingMetricsEndpoint(MeterRegistry registry, 
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) CustomMetricsCollector metricsCollector) {
        this.registry = registry;
        this.metricsCollector = metricsCollector;
    }

    /**
     * 获取所有日志指标
     */
    @ReadOperation
    public Map<String, Object> loggingMetrics() {
        Map<String, Object> payload = new HashMap<>();

        // 基础指标
        payload.put("ingest.count", sum("logging.ingest.count"));
        payload.put("error.count", sum("logging.ingest.count", "error"));
        payload.put("success.count", sum("logging.ingest.count", "success"));
        payload.put("warn.count", sum("logging.ingest.count", "warn"));

        // 延迟指标
        payload.put("latency.mean", timerMean("logging.latency"));
        payload.put("latency.p50", timerPercentile("logging.latency", 0.50));
        payload.put("latency.p95", timerPercentile("logging.latency", 0.95));
        payload.put("latency.p99", timerPercentile("logging.latency", 0.99));

        // 吞吐量指标
        payload.put("throughput.bytes", summaryMean("logging.throughput.bytes"));
        payload.put("batch.size.avg", summaryMean("logging.batch.size"));

        // Gauge指标
        payload.put("queue.depth", gauge("logging.queue.depth"));
        payload.put("cache.hit.ratio", gauge("logging.cache.hit.ratio"));
        payload.put("compression.ratio", gauge("logging.compression.ratio"));
        payload.put("active.threads", gauge("logging.active.threads"));
        payload.put("memory.usage", gauge("logging.memory.usage"));

        // 业务指标
        payload.put("async.batch.count", gauge("logging.async.batch.count"));
        payload.put("gzip.compression.count", gauge("logging.gzip.compression.count"));
        payload.put("redis.cache.operations", gauge("logging.redis.cache.operations"));
        payload.put("masking.operations", gauge("logging.masking.operations"));
        payload.put("audit.events", gauge("logging.audit.events"));

        // 计算衍生指标
        payload.put("error.rate", metricsCollector.getCurrentErrorRate());
        payload.put("success.rate", metricsCollector.getCurrentSuccessRate());

        // 性能统计
        payload.put("performance.summary", Map.of(
            "avg_latency_ms", metricsCollector.getAverageLatencyMs(),
            "p95_latency_ms", metricsCollector.getP95LatencyMs(),
            "p99_latency_ms", metricsCollector.getP99LatencyMs(),
            "avg_batch_size", metricsCollector.getAverageBatchSize()
        ));

        // 系统健康状态
        payload.put("health", getHealthStatus());

        // 时间戳
        payload.put("timestamp", System.currentTimeMillis());

        log.debug("查询日志指标完成");
        return payload;
    }

    /**
     * 按类型获取指标
     */
    @ReadOperation
    public Map<String, Object> loggingMetricsByType(@org.springframework.boot.actuate.endpoint.annotation.Selector String type) {
        if (type == null || type.isEmpty()) {
            return loggingMetrics();
        }

        Map<String, Object> result = new HashMap<>();
        switch (type.toLowerCase()) {
            case "ingest":
                result.put("count", sum("logging.ingest.count"));
                result.put("error", sum("logging.ingest.count", "error"));
                result.put("success", sum("logging.ingest.count", "success"));
                result.put("warn", sum("logging.ingest.count", "warn"));
                result.put("error_rate", metricsCollector.getCurrentErrorRate());
                break;

            case "latency":
                result.put("mean", timerMean("logging.latency"));
                result.put("p50", timerPercentile("logging.latency", 0.50));
                result.put("p95", timerPercentile("logging.latency", 0.95));
                result.put("p99", timerPercentile("logging.latency", 0.99));
                break;

            case "performance":
                result.put("throughput", summaryMean("logging.throughput.bytes"));
                result.put("batch_size", summaryMean("logging.batch.size"));
                result.put("queue_depth", gauge("logging.queue.depth"));
                result.put("active_threads", gauge("logging.active.threads"));
                break;

            case "system":
                result.put("cache_hit_ratio", gauge("logging.cache.hit.ratio"));
                result.put("compression_ratio", gauge("logging.compression.ratio"));
                result.put("memory_usage", gauge("logging.memory.usage"));
                break;

            case "business":
                result.put("async_batch", gauge("logging.async.batch.count"));
                result.put("gzip_compression", gauge("logging.gzip.compression.count"));
                result.put("redis_operations", gauge("logging.redis.cache.operations"));
                result.put("masking", gauge("logging.masking.operations"));
                result.put("audit_events", gauge("logging.audit.events"));
                break;

            default:
                throw new IllegalArgumentException("未知的指标类型: " + type);
        }

        return result;
    }

    /**
     * 获取指标总和
     */
    private Double sum(String name) {
        Counter counter = Search.in(registry)
                .name(name)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    /**
     * 获取带标签的指标总和
     */
    private Double sum(String name, String tagValue) {
        Counter counter = Search.in(registry)
                .name(name)
                .tag("type", tagValue)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    /**
     * 获取计时器平均值（毫秒）
     */
    private Double timerMean(String name) {
        Timer timer = Search.in(registry)
                .name(name)
                .timer();
        return timer != null ? timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }

    /**
     * 获取计时器百分位数（毫秒）
     */
    private Double timerPercentile(String name, double percentile) {
        Timer timer = Search.in(registry)
                .name(name)
                .timer();
        return timer != null ? timer.percentile(percentile, java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }

    /**
     * 获取摘要平均值
     */
    private Double summaryMean(String name) {
        DistributionSummary summary = Search.in(registry)
                .name(name)
                .summary();
        return summary != null ? summary.mean() : 0.0;
    }

    /**
     * 获取仪表值
     */
    private Double gauge(String name) {
        Gauge gauge = Search.in(registry)
                .name(name)
                .gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    /**
     * 获取健康状态
     */
    private Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("details", Map.of(
            "error_rate", metricsCollector.getCurrentErrorRate(),
            "avg_latency_ms", metricsCollector.getAverageLatencyMs(),
            "queue_depth", gauge("logging.queue.depth")
        ));
        return health;
    }
}
