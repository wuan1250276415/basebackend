package com.basebackend.logging.audit.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 审计系统指标监控
 *
 * 基于 Micrometer 的指标收集和监控。
 * 提供完整的审计操作性能和质量监控。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class AuditMetrics {

    private final MeterRegistry registry;

    /** 审计事件计数器 */
    private final Counter totalEvents;
    private final Counter successEvents;
    private final Counter failureEvents;
    private final Counter criticalEvents;

    /** 存储相关指标 */
    private final Counter storageErrors;
    private final Counter storageRetries;
    private final Counter storageTimeouts;

    /** 性能指标 */
    private final Timer operationTimer;
    private final DistributionSummary latency;
    private final DistributionSummary throughput;
    private final DistributionSummary batchSize;

    /** 当前队列状态 */
    private final AtomicLong currentQueueSize = new AtomicLong(0);
    private final AtomicLong maxQueueSize = new AtomicLong(0);

    /** 哈希链指标 */
    private final Counter hashChainErrors;
    private final Counter signatureErrors;

    /** 持久化指标 */
    private final LongAdder totalBytesWritten = new LongAdder();

    public AuditMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 事件计数器
        this.totalEvents = Counter.builder("audit.events.total")
                .description("总审计事件数")
                .register(registry);
        this.successEvents = Counter.builder("audit.events.success")
                .description("成功审计事件数")
                .register(registry);
        this.failureEvents = Counter.builder("audit.events.failure")
                .description("失败审计事件数")
                .register(registry);
        this.criticalEvents = Counter.builder("audit.events.critical")
                .description("严重级别审计事件数")
                .register(registry);

        // 存储指标
        this.storageErrors = Counter.builder("audit.storage.errors")
                .description("存储错误次数")
                .register(registry);
        this.storageRetries = Counter.builder("audit.storage.retries")
                .description("存储重试次数")
                .register(registry);
        this.storageTimeouts = Counter.builder("audit.storage.timeouts")
                .description("存储超时次数")
                .register(registry);

        // 性能指标
        this.operationTimer = Timer.builder("audit.operation.duration")
                .description("审计操作耗时")
                .register(registry);
        this.latency = DistributionSummary.builder("audit.latency")
                .baseUnit("milliseconds")
                .description("审计操作延迟")
                .register(registry);
        this.throughput = DistributionSummary.builder("audit.throughput")
                .baseUnit("events/second")
                .description("审计吞吐量")
                .register(registry);
        this.batchSize = DistributionSummary.builder("audit.batch.size")
                .baseUnit("events")
                .description("批量写入大小")
                .register(registry);

        // 哈希链指标
        this.hashChainErrors = Counter.builder("audit.hash.errors")
                .description("哈希链计算错误次数")
                .register(registry);
        this.signatureErrors = Counter.builder("audit.signature.errors")
                .description("数字签名错误次数")
                .register(registry);

        // 队列状态指标
        Gauge.builder("audit.queue.current.size", currentQueueSize, obj -> obj.get())
                .description("当前队列大小")
                .register(registry);

        Gauge.builder("audit.queue.max.size", maxQueueSize, obj -> obj.get())
                .description("最大队列大小")
                .register(registry);

        Gauge.builder("audit.bytes.written", totalBytesWritten, obj -> obj.sum())
                .description("写入的总字节数")
                .register(registry);
    }

    /**
     * 记录成功操作
     */
    public void recordSuccess(long elapsedMs) {
        totalEvents.increment();
        successEvents.increment();
        latency.record(elapsedMs);
    }

    /**
     * 记录失败操作
     */
    public void recordFailure(String errorType) {
        totalEvents.increment();
        failureEvents.increment();
    }

    /**
     * 记录严重事件
     */
    public void recordCritical() {
        criticalEvents.increment();
    }

    /**
     * 记录存储错误
     */
    public void recordStorageError() {
        storageErrors.increment();
    }

    /**
     * 记录存储重试
     */
    public void recordStorageRetry() {
        storageRetries.increment();
    }

    /**
     * 记录存储超时
     */
    public void recordStorageTimeout() {
        storageTimeouts.increment();
    }

    /**
     * 记录批量操作
     */
    public void recordBatch(int size, long elapsedMs) {
        batchSize.record(size);
        throughput.record((double) size / (elapsedMs / 1000.0));
        operationTimer.record(elapsedMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 更新队列大小
     */
    public void updateQueueSize(int currentSize) {
        long current = currentQueueSize.get();
        long max = maxQueueSize.get();

        currentQueueSize.set(currentSize);

        if (currentSize > max) {
            maxQueueSize.set(currentSize);
        }
    }

    /**
     * 记录哈希链错误
     */
    public void recordHashChainError() {
        hashChainErrors.increment();
    }

    /**
     * 记录签名错误
     */
    public void recordSignatureError() {
        signatureErrors.increment();
    }

    /**
     * 记录写入字节数
     */
    public void recordBytesWritten(long bytes) {
        totalBytesWritten.add(bytes);
    }

    /**
     * 获取错误率
     */
    public double getErrorRate() {
        long success = (long) successEvents.count();
        long failure = (long) failureEvents.count();
        long total = success + failure;
        return total > 0 ? (double) failure / total : 0.0;
    }

    /**
     * 获取关键统计信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("审计指标摘要:\n");
        sb.append("  总事件数: ").append((long) totalEvents.count()).append("\n");
        sb.append("  成功事件: ").append((long) successEvents.count()).append("\n");
        sb.append("  失败事件: ").append((long) failureEvents.count()).append("\n");
        sb.append("  严重事件: ").append((long) criticalEvents.count()).append("\n");
        sb.append("  错误率: ").append(String.format("%.2f%%", getErrorRate() * 100)).append("\n");
        sb.append("  存储错误: ").append((long) storageErrors.count()).append("\n");
        sb.append("  队列当前大小: ").append(currentQueueSize.get()).append("\n");
        sb.append("  队列最大大小: ").append(maxQueueSize.get()).append("\n");
        sb.append("  写入字节数: ").append(totalBytesWritten.sum()).append("\n");

        return sb.toString();
    }

    /**
     * 检查系统健康状态
     */
    public boolean isHealthy() {
        long errors = (long) storageErrors.count();
        double errorRate = getErrorRate();
        long queueSize = currentQueueSize.get();

        // 健康检查条件
        boolean hasNoStorageErrors = errors < 10;
        boolean hasLowErrorRate = errorRate < 0.05; // 5%
        boolean queueNotFull = queueSize < 10000;

        return hasNoStorageErrors && hasLowErrorRate && queueNotFull;
    }
}
