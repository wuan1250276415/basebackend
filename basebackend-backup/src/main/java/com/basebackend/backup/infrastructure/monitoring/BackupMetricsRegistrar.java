package com.basebackend.backup.infrastructure.monitoring;

import com.basebackend.backup.config.BackupProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 备份系统指标注册器
 * <p>
 * 收集和暴露备份系统的关键指标，包括：
 * <ul>
 *   <li>备份/恢复操作计数</li>
 *   <li>成功/失败率统计</li>
 *   <li>操作耗时分布</li>
 *   <li>活跃任务数量</li>
 *   <li>存储使用量</li>
 *   <li>重试统计</li>
 * </ul>
 *
 * @author BaseBackend
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "backup.metrics.enabled", havingValue = "true")
public class BackupMetricsRegistrar {

    private final MeterRegistry meterRegistry;
    private final BackupProperties backupProperties;

    // 计数器
    private Counter backupTotalCounter;
    private Counter backupSuccessCounter;
    private Counter backupFailureCounter;
    private Counter restoreTotalCounter;
    private Counter restoreSuccessCounter;
    private Counter restoreFailureCounter;

    // 定时器
    private Timer backupDurationTimer;
    private Timer restoreDurationTimer;

    // 当前活跃任务数
    private final AtomicInteger activeBackupTasks = new AtomicInteger(0);
    private final AtomicInteger activeRestoreTasks = new AtomicInteger(0);

    // 存储指标
    private final ConcurrentHashMap<String, AtomicLong> storageSizeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> storageCountMap = new ConcurrentHashMap<>();

    // 重试指标
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final AtomicLong successfulRetries = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (!backupProperties.getMetrics().isEnabled()) {
            log.info("备份监控指标已禁用");
            return;
        }

        String prefix = backupProperties.getMetrics().getPrefix();
        log.info("初始化备份监控指标, prefix: {}", prefix);

        // 创建计数器
        backupTotalCounter = Counter.builder(prefix + "_backup_total")
            .description("Total number of backup operations")
            .register(meterRegistry);

        backupSuccessCounter = Counter.builder(prefix + "_backup_success_total")
            .description("Total number of successful backup operations")
            .register(meterRegistry);

        backupFailureCounter = Counter.builder(prefix + "_backup_failure_total")
            .description("Total number of failed backup operations")
            .register(meterRegistry);

        restoreTotalCounter = Counter.builder(prefix + "_restore_total")
            .description("Total number of restore operations")
            .register(meterRegistry);

        restoreSuccessCounter = Counter.builder(prefix + "_restore_success_total")
            .description("Total number of successful restore operations")
            .register(meterRegistry);

        restoreFailureCounter = Counter.builder(prefix + "_restore_failure_total")
            .description("Total number of failed restore operations")
            .register(meterRegistry);

        // 创建定时器
        backupDurationTimer = Timer.builder(prefix + "_backup_duration_seconds")
            .description("Backup operation duration in seconds")
            .register(meterRegistry);

        restoreDurationTimer = Timer.builder(prefix + "_restore_duration_seconds")
            .description("Restore operation duration in seconds")
            .register(meterRegistry);

        // 创建Gauge（当前活跃任务数）
        Gauge.builder(prefix + "_active_backup_tasks", activeBackupTasks, AtomicInteger::get)
            .description("Number of currently running backup tasks")
            .register(meterRegistry);

        Gauge.builder(prefix + "_active_restore_tasks", activeRestoreTasks, AtomicInteger::get)
            .description("Number of currently running restore tasks")
            .register(meterRegistry);

        // 创建存储相关Gauge
        Gauge.builder(prefix + "_storage_usage_bytes", storageSizeMap,
                map -> map.values().stream().mapToLong(AtomicLong::get).sum())
            .description("Storage usage in bytes by type")
            .register(meterRegistry);

        Gauge.builder(prefix + "_storage_objects_total", storageCountMap,
                map -> map.values().stream().mapToInt(AtomicInteger::get).sum())
            .description("Total number of storage objects by type")
            .register(meterRegistry);

        // 重试指标
        Gauge.builder(prefix + "_retries_total", totalRetries, AtomicLong::get)
            .description("Total number of retry attempts")
            .register(meterRegistry);

        Gauge.builder(prefix + "_retries_success_total", successfulRetries, AtomicLong::get)
            .description("Total number of successful retry attempts")
            .register(meterRegistry);

        log.info("备份监控指标初始化完成");
    }

    /**
     * 记录备份开始
     */
    public void recordBackupStart() {
        activeBackupTasks.incrementAndGet();
        backupTotalCounter.increment();
    }

    /**
     * 记录备份成功
     */
    public void recordBackupSuccess(long durationMs) {
        activeBackupTasks.decrementAndGet();
        backupSuccessCounter.increment();
        backupDurationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录备份失败
     */
    public void recordBackupFailure(long durationMs) {
        activeBackupTasks.decrementAndGet();
        backupFailureCounter.increment();
        backupDurationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录恢复开始
     */
    public void recordRestoreStart() {
        activeRestoreTasks.incrementAndGet();
        restoreTotalCounter.increment();
    }

    /**
     * 记录恢复成功
     */
    public void recordRestoreSuccess(long durationMs) {
        activeRestoreTasks.decrementAndGet();
        restoreSuccessCounter.increment();
        restoreDurationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录恢复失败
     */
    public void recordRestoreFailure(long durationMs) {
        activeRestoreTasks.decrementAndGet();
        restoreFailureCounter.increment();
        restoreDurationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录存储使用量
     */
    public void recordStorageUsage(String storageType, long bytes, int objectCount) {
        storageSizeMap.computeIfAbsent(storageType, k -> new AtomicLong(0))
            .addAndGet(bytes);

        storageCountMap.computeIfAbsent(storageType, k -> new AtomicInteger(0))
            .addAndGet(objectCount);

        log.debug("更新存储使用量: type={}, bytes={}, objects={}",
            storageType, bytes, objectCount);
    }

    /**
     * 记录重试
     */
    public void recordRetry(boolean success) {
        totalRetries.incrementAndGet();
        if (success) {
            successfulRetries.incrementAndGet();
        }
    }

    /**
     * 获取当前指标快照
     */
    public MetricsSnapshot getSnapshot() {
        return MetricsSnapshot.builder()
            .backupTotal(Math.round(backupTotalCounter.count()))
            .backupSuccess(Math.round(backupSuccessCounter.count()))
            .backupFailure(Math.round(backupFailureCounter.count()))
            .restoreTotal(Math.round(restoreTotalCounter.count()))
            .restoreSuccess(Math.round(restoreSuccessCounter.count()))
            .restoreFailure(Math.round(restoreFailureCounter.count()))
            .activeBackupTasks(activeBackupTasks.get())
            .activeRestoreTasks(activeRestoreTasks.get())
            .totalRetries(totalRetries.get())
            .successfulRetries(successfulRetries.get())
            .storageSizeMap(new ConcurrentHashMap<>(storageSizeMap))
            .storageCountMap(new ConcurrentHashMap<>(storageCountMap))
            .build();
    }

    /**
     * 计算备份成功率
     */
    public double getBackupSuccessRate() {
        double total = backupTotalCounter.count();
        if (total == 0) {
            return 0.0;
        }
        return backupSuccessCounter.count() / total;
    }

    /**
     * 计算恢复成功率
     */
    public double getRestoreSuccessRate() {
        double total = restoreTotalCounter.count();
        if (total == 0) {
            return 0.0;
        }
        return restoreSuccessCounter.count() / total;
    }

    /**
     * 计算重试成功率
     */
    public double getRetrySuccessRate() {
        long total = totalRetries.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulRetries.get() / total;
    }

    /**
     * 重置所有指标（仅用于测试）
     */
    public void reset() {
        activeBackupTasks.set(0);
        activeRestoreTasks.set(0);
        totalRetries.set(0);
        successfulRetries.set(0);
        storageSizeMap.clear();
        storageCountMap.clear();

        log.warn("重置所有备份监控指标");
    }

    /**
     * 指标快照
     */
    public static class MetricsSnapshot {
        private final long backupTotal;
        private final long backupSuccess;
        private final long backupFailure;
        private final long restoreTotal;
        private final long restoreSuccess;
        private final long restoreFailure;
        private final int activeBackupTasks;
        private final int activeRestoreTasks;
        private final long totalRetries;
        private final long successfulRetries;
        private final ConcurrentHashMap<String, AtomicLong> storageSizeMap;
        private final ConcurrentHashMap<String, AtomicInteger> storageCountMap;

        public MetricsSnapshot(Builder builder) {
            this.backupTotal = builder.backupTotal;
            this.backupSuccess = builder.backupSuccess;
            this.backupFailure = builder.backupFailure;
            this.restoreTotal = builder.restoreTotal;
            this.restoreSuccess = builder.restoreSuccess;
            this.restoreFailure = builder.restoreFailure;
            this.activeBackupTasks = builder.activeBackupTasks;
            this.activeRestoreTasks = builder.activeRestoreTasks;
            this.totalRetries = builder.totalRetries;
            this.successfulRetries = builder.successfulRetries;
            this.storageSizeMap = builder.storageSizeMap;
            this.storageCountMap = builder.storageCountMap;
        }

        // Getters
        public long getBackupTotal() { return backupTotal; }
        public long getBackupSuccess() { return backupSuccess; }
        public long getBackupFailure() { return backupFailure; }
        public long getRestoreTotal() { return restoreTotal; }
        public long getRestoreSuccess() { return restoreSuccess; }
        public long getRestoreFailure() { return restoreFailure; }
        public int getActiveBackupTasks() { return activeBackupTasks; }
        public int getActiveRestoreTasks() { return activeRestoreTasks; }
        public long getTotalRetries() { return totalRetries; }
        public long getSuccessfulRetries() { return successfulRetries; }
        public ConcurrentHashMap<String, AtomicLong> getStorageSizeMap() { return storageSizeMap; }
        public ConcurrentHashMap<String, AtomicInteger> getStorageCountMap() { return storageCountMap; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long backupTotal;
            private long backupSuccess;
            private long backupFailure;
            private long restoreTotal;
            private long restoreSuccess;
            private long restoreFailure;
            private int activeBackupTasks;
            private int activeRestoreTasks;
            private long totalRetries;
            private long successfulRetries;
            private ConcurrentHashMap<String, AtomicLong> storageSizeMap;
            private ConcurrentHashMap<String, AtomicInteger> storageCountMap;

            public Builder backupTotal(long backupTotal) {
                this.backupTotal = backupTotal;
                return this;
            }

            public Builder backupSuccess(long backupSuccess) {
                this.backupSuccess = backupSuccess;
                return this;
            }

            public Builder backupFailure(long backupFailure) {
                this.backupFailure = backupFailure;
                return this;
            }

            public Builder restoreTotal(long restoreTotal) {
                this.restoreTotal = restoreTotal;
                return this;
            }

            public Builder restoreSuccess(long restoreSuccess) {
                this.restoreSuccess = restoreSuccess;
                return this;
            }

            public Builder restoreFailure(long restoreFailure) {
                this.restoreFailure = restoreFailure;
                return this;
            }

            public Builder activeBackupTasks(int activeBackupTasks) {
                this.activeBackupTasks = activeBackupTasks;
                return this;
            }

            public Builder activeRestoreTasks(int activeRestoreTasks) {
                this.activeRestoreTasks = activeRestoreTasks;
                return this;
            }

            public Builder totalRetries(long totalRetries) {
                this.totalRetries = totalRetries;
                return this;
            }

            public Builder successfulRetries(long successfulRetries) {
                this.successfulRetries = successfulRetries;
                return this;
            }

            public Builder storageSizeMap(ConcurrentHashMap<String, AtomicLong> storageSizeMap) {
                this.storageSizeMap = storageSizeMap;
                return this;
            }

            public Builder storageCountMap(ConcurrentHashMap<String, AtomicInteger> storageCountMap) {
                this.storageCountMap = storageCountMap;
                return this;
            }

            public MetricsSnapshot build() {
                return new MetricsSnapshot(this);
            }
        }
    }
}
