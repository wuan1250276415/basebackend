package com.basebackend.logging.monitoring.health;

import com.basebackend.logging.monitoring.collector.CustomMetricsCollector;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控健康检查指示器
 *
 * 提供监控系统的健康状态检查，
 * 集成到 Spring Boot Actuator。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class MonitoringHealthIndicator implements HealthIndicator {

    private final MeterRegistry registry;
    private final CustomMetricsCollector metricsCollector;

    public MonitoringHealthIndicator(MeterRegistry registry, 
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) CustomMetricsCollector metricsCollector) {
        this.registry = registry;
        this.metricsCollector = metricsCollector;
    }

    public MonitoringHealthIndicator(MeterRegistry registry) {
        this.registry = registry;
        this.metricsCollector = null;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        try {
            // 检查核心指标是否存在
            boolean hasCoreMetrics = hasCoreMetrics();
            details.put("metrics.present", hasCoreMetrics);

            // 获取关键指标
            double errorRate = metricsCollector != null
                    ? metricsCollector.getCurrentErrorRate()
                    : sum("logging.ingest.count", "error") / Math.max(sum("logging.ingest.count"), 1);

            double avgLatencyMs = metricsCollector != null
                    ? metricsCollector.getAverageLatencyMs()
                    : timerMean("logging.latency");

            long queueDepth = gauge("logging.queue.depth").longValue();

            details.put("error.rate", errorRate);
            details.put("avg.latency.ms", avgLatencyMs);
            details.put("queue.depth", queueDepth);

            // 获取详细指标
            details.putAll(getDetailedMetrics());

            // 判断健康状态
            Health.Builder builder;
            if (errorRate > 0.1 || avgLatencyMs > 1000 || queueDepth > 10000) {
                builder = Health.down();
                builder.withDetail("reason", "指标异常");
                log.warn("监控系统健康检查异常: 错误率={}, 延迟={}ms, 队列深度={}",
                        errorRate, avgLatencyMs, queueDepth);
            } else if (errorRate > 0.05 || avgLatencyMs > 500 || queueDepth > 5000) {
                builder = Health.up();
                builder.withDetail("reason", "指标警告");
                log.warn("监控系统健康检查警告: 错误率={}, 延迟={}ms, 队列深度={}",
                        errorRate, avgLatencyMs, queueDepth);
            } else {
                builder = Health.up();
                builder.withDetail("reason", "所有指标正常");
                log.debug("监控系统健康检查通过");
            }

            return builder.withDetails(details).build();

        } catch (Exception e) {
            log.error("监控健康检查异常", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetails(details)
                    .build();
        }
    }

    /**
     * 检查是否包含核心指标
     */
    private boolean hasCoreMetrics() {
        Counter counter = Search.in(registry)
                .name("logging.ingest.count")
                .counter();
        return counter != null;
    }

    /**
     * 获取详细指标
     */
    private Map<String, Object> getDetailedMetrics() {
        Map<String, Object> details = new HashMap<>();

        // 吞吐量指标
        details.put("throughput.bytes", summaryMean("logging.throughput.bytes"));

        // 缓存指标
        details.put("cache.hit.ratio", gauge("logging.cache.hit.ratio"));

        // 压缩指标
        details.put("compression.ratio", gauge("logging.compression.ratio"));

        // 线程指标
        details.put("active.threads", gauge("logging.active.threads"));

        // 内存指标
        details.put("memory.usage", gauge("logging.memory.usage"));

        // 业务指标
        details.put("async.batch.count", gauge("logging.async.batch.count"));
        details.put("gzip.compression.count", gauge("logging.gzip.compression.count"));
        details.put("redis.cache.operations", gauge("logging.redis.cache.operations"));
        details.put("masking.operations", gauge("logging.masking.operations"));
        details.put("audit.events", gauge("logging.audit.events"));

        // 百分位数指标
        details.put("latency.p50.ms", timerPercentile("logging.latency", 0.50));
        details.put("latency.p95.ms", timerPercentile("logging.latency", 0.95));
        details.put("latency.p99.ms", timerPercentile("logging.latency", 0.99));

        // 计数指标
        details.put("ingest.total", sum("logging.ingest.count"));
        details.put("ingest.success", sum("logging.ingest.count", "success"));
        details.put("ingest.error", sum("logging.ingest.count", "error"));
        details.put("ingest.warn", sum("logging.ingest.count", "warn"));

        return details;
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
}
