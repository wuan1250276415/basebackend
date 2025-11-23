package com.basebackend.logging.monitoring.collector;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义指标采集器
 *
 * 采集日志系统的核心业务指标，包括：
 * - 日志流量统计
 * - 错误率计算
 * - 性能指标（延迟、吞吐量）
 * - 系统资源指标（队列、缓存、压缩率）
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class CustomMetricsCollector {

    private final MeterRegistry registry;

    /** 日志采集计数器 */
    private final Counter logIngestCounter;
    private final Counter logSuccessCounter;
    private final Counter logErrorCounter;
    private final Counter logWarnCounter;

    /** 延迟统计计时器 */
    private final Timer logLatencyTimer;

    /** 吞吐量统计 */
    private final DistributionSummary throughputBytes;
    private final DistributionSummary batchSizeSummary;

    /** 系统资源指标 */
    private final AtomicLong queueDepthGauge = new AtomicLong(0);
    private final AtomicLong cacheHitGauge = new AtomicLong(0);
    private final AtomicLong compressionRatioGauge = new AtomicLong(100);
    private final AtomicLong activeThreadsGauge = new AtomicLong(0);
    private final AtomicLong memoryUsageGauge = new AtomicLong(0);

    /** 业务指标 */
    private final AtomicLong asyncBatchCount = new AtomicLong(0);
    private final AtomicLong gzipCompressionCount = new AtomicLong(0);
    private final AtomicLong redisCacheOperations = new AtomicLong(0);
    private final AtomicLong maskingOperations = new AtomicLong(0);
    private final AtomicLong auditEvents = new AtomicLong(0);

    public CustomMetricsCollector(MeterRegistry registry) {
        this.registry = registry;

        // 初始化日志流量指标
        this.logIngestCounter = Counter.builder("logging.ingest.count")
                .description("总日志采集数")
                .tag("type", "total")
                .register(registry);

        this.logSuccessCounter = Counter.builder("logging.ingest.count")
                .description("成功日志数")
                .tag("type", "success")
                .register(registry);

        this.logErrorCounter = Counter.builder("logging.ingest.count")
                .description("错误日志数")
                .tag("type", "error")
                .register(registry);

        this.logWarnCounter = Counter.builder("logging.ingest.count")
                .description("警告日志数")
                .tag("type", "warn")
                .register(registry);

        // 初始化延迟计时器
        this.logLatencyTimer = Timer.builder("logging.latency")
                .description("日志处理延迟")
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(10), Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(500))
                .register(registry);

        // 初始化吞吐量指标
        this.throughputBytes = DistributionSummary.builder("logging.throughput.bytes")
                .description("字节吞吐量")
                .baseUnit("bytes")
                .publishPercentileHistogram(true)
                .register(registry);

        this.batchSizeSummary = DistributionSummary.builder("logging.batch.size")
                .description("批量处理大小")
                .baseUnit("events")
                .register(registry);

        // 注册Gauge指标
        registerGauges();
    }

    /**
     * 注册所有Gauge指标
     */
    private void registerGauges() {
        // 队列深度
        Gauge.builder("logging.queue.depth", queueDepthGauge, AtomicLong::get)
                .description("当前队列深度")
                .register(registry);

        // 缓存命中率
        Gauge.builder("logging.cache.hit.ratio", cacheHitGauge, AtomicLong::get)
                .description("缓存命中率（百分比）")
                .register(registry);

        // 压缩比
        Gauge.builder("logging.compression.ratio", compressionRatioGauge, AtomicLong::get)
                .description("压缩比（百分比）")
                .register(registry);

        // 活跃线程数
        Gauge.builder("logging.active.threads", activeThreadsGauge, AtomicLong::get)
                .description("活跃线程数")
                .register(registry);

        // 内存使用
        Gauge.builder("logging.memory.usage", memoryUsageGauge, AtomicLong::get)
                .description("内存使用（百分比）")
                .register(registry);

        // 异步批量操作
        Gauge.builder("logging.async.batch.count", asyncBatchCount, AtomicLong::get)
                .description("异步批量操作计数")
                .register(registry);

        // GZIP压缩操作
        Gauge.builder("logging.gzip.compression.count", gzipCompressionCount, AtomicLong::get)
                .description("GZIP压缩操作计数")
                .register(registry);

        // Redis缓存操作
        Gauge.builder("logging.redis.cache.operations", redisCacheOperations, AtomicLong::get)
                .description("Redis缓存操作计数")
                .register(registry);

        // 脱敏操作
        Gauge.builder("logging.masking.operations", maskingOperations, AtomicLong::get)
                .description("数据脱敏操作计数")
                .register(registry);

        // 审计事件
        Gauge.builder("logging.audit.events", auditEvents, AtomicLong::get)
                .description("审计事件计数")
                .register(registry);
    }

    /**
     * 记录日志采集
     */
    public void recordLogIngest(long count) {
        logIngestCounter.increment(count);
    }

    /**
     * 记录成功日志
     */
    public void recordLogSuccess(long count) {
        logSuccessCounter.increment(count);
    }

    /**
     * 记录错误日志
     */
    public void recordLogError(long count) {
        logErrorCounter.increment(count);
    }

    /**
     * 记录警告日志
     */
    public void recordLogWarn(long count) {
        logWarnCounter.increment(count);
    }

    /**
     * 记录延迟
     */
    public void recordLatency(Duration duration) {
        logLatencyTimer.record(duration);
    }

    /**
     * 记录吞吐量
     */
    public void recordThroughputBytes(double bytes) {
        throughputBytes.record(bytes);
    }

    /**
     * 记录批量大小
     */
    public void recordBatchSize(int size) {
        batchSizeSummary.record(size);
    }

    /**
     * 更新队列深度
     */
    public void updateQueueDepth(long depth) {
        queueDepthGauge.set(depth);
    }

    /**
     * 更新缓存命中率
     */
    public void updateCacheHitRatio(long percentage) {
        cacheHitGauge.set(percentage);
    }

    /**
     * 更新压缩比
     */
    public void updateCompressionRatio(long percentage) {
        compressionRatioGauge.set(percentage);
    }

    /**
     * 更新活跃线程数
     */
    public void updateActiveThreads(long count) {
        activeThreadsGauge.set(count);
    }

    /**
     * 更新内存使用率
     */
    public void updateMemoryUsage(long percentage) {
        memoryUsageGauge.set(percentage);
    }

    /**
     * 记录异步批量操作
     */
    public void incrementAsyncBatchCount() {
        asyncBatchCount.incrementAndGet();
    }

    /**
     * 记录GZIP压缩操作
     */
    public void incrementGzipCompressionCount() {
        gzipCompressionCount.incrementAndGet();
    }

    /**
     * 记录Redis缓存操作
     */
    public void incrementRedisCacheOperations() {
        redisCacheOperations.incrementAndGet();
    }

    /**
     * 记录脱敏操作
     */
    public void incrementMaskingOperations() {
        maskingOperations.incrementAndGet();
    }

    /**
     * 记录审计事件
     */
    public void incrementAuditEvents() {
        auditEvents.incrementAndGet();
    }

    /**
     * 获取当前错误率
     */
    public double getCurrentErrorRate() {
        double total = logSuccessCounter.count() + logErrorCounter.count();
        if (total == 0) {
            return 0.0;
        }
        return logErrorCounter.count() / total;
    }

    /**
     * 获取当前成功率
     */
    public double getCurrentSuccessRate() {
        double total = logSuccessCounter.count() + logErrorCounter.count();
        if (total == 0) {
            return 0.0;
        }
        return logSuccessCounter.count() / total;
    }

    /**
     * 获取平均延迟（毫秒）
     */
    public double getAverageLatencyMs() {
        return logLatencyTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 获取P95延迟（毫秒）
     */
    public double getP95LatencyMs() {
        return logLatencyTimer.percentile(0.95, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 获取P99延迟（毫秒）
     */
    public double getP99LatencyMs() {
        return logLatencyTimer.percentile(0.99, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 获取平均批量大小
     */
    public double getAverageBatchSize() {
        return batchSizeSummary.mean();
    }

    /**
     * 导出所有指标为JSON
     */
    public String exportToJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"metrics\": {\n");
        sb.append("    \"ingest\": {\n");
        sb.append("      \"total\": ").append((long) logIngestCounter.count()).append(",\n");
        sb.append("      \"success\": ").append((long) logSuccessCounter.count()).append(",\n");
        sb.append("      \"error\": ").append((long) logErrorCounter.count()).append(",\n");
        sb.append("      \"warn\": ").append((long) logWarnCounter.count()).append("\n");
        sb.append("    },\n");
        sb.append("    \"performance\": {\n");
        sb.append("      \"avg_latency_ms\": ").append(getAverageLatencyMs()).append(",\n");
        sb.append("      \"p95_latency_ms\": ").append(getP95LatencyMs()).append(",\n");
        sb.append("      \"p99_latency_ms\": ").append(getP99LatencyMs()).append(",\n");
        sb.append("      \"avg_batch_size\": ").append(getAverageBatchSize()).append("\n");
        sb.append("    },\n");
        sb.append("    \"system\": {\n");
        sb.append("      \"queue_depth\": ").append(queueDepthGauge.get()).append(",\n");
        sb.append("      \"cache_hit_ratio\": ").append(cacheHitGauge.get()).append(",\n");
        sb.append("      \"compression_ratio\": ").append(compressionRatioGauge.get()).append(",\n");
        sb.append("      \"active_threads\": ").append(activeThreadsGauge.get()).append(",\n");
        sb.append("      \"memory_usage\": ").append(memoryUsageGauge.get()).append("\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }
}
