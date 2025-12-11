package com.basebackend.logging.audit.service;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.crypto.AuditSignatureService;
import com.basebackend.logging.audit.crypto.HashChainCalculator;
import com.basebackend.logging.audit.metrics.AuditMetrics;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.basebackend.logging.audit.storage.AuditStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 审计服务核心类
 *
 * 提供完整的审计功能：
 * - 异步批量写入
 * - 哈希链完整性保护
 * - 数字签名验证
 * - 自动刷盘机制
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class AuditService {

    private final AuditStorage storage;
    private final HashChainCalculator hashChainCalculator;
    private final AuditSignatureService signatureService;
    private final AuditMetrics metrics;

    private final BlockingQueue<AuditLogEntry> queue;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService ioExecutor;

    private final int queueCapacity;
    private final int batchSize;
    private final long flushIntervalMs;

    // P0优化：使用AtomicReference替代volatile+synchronized，提升线程安全性
    private final AtomicReference<String> lastHash = new AtomicReference<>(null);
    private final AtomicLong totalEntries = new AtomicLong(0);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    
    // P0优化：内存监控相关
    private final AtomicLong droppedEntries = new AtomicLong(0);
    private static final double MEMORY_THRESHOLD = 0.85; // 内存使用率阈值

    public AuditService(AuditStorage storage,
                        HashChainCalculator hashChainCalculator,
                        AuditSignatureService signatureService,
                        AuditMetrics metrics,
                        int queueCapacity,
                        int batchSize,
                        long flushIntervalMs) {
        this.storage = storage;
        this.hashChainCalculator = hashChainCalculator;
        this.signatureService = signatureService;
        this.metrics = metrics;

        this.queueCapacity = queueCapacity;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;

        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "audit-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.ioExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "audit-io");
            t.setDaemon(true);
            return t;
        });

        // 启动定时刷新任务
        startScheduler();

        log.info("审计服务初始化完成，队列容量: {}, 批量大小: {}, 刷新间隔: {}ms",
                queueCapacity, batchSize, flushIntervalMs);
    }

    /**
     * 记录审计事件
     */
    public void record(String userId, AuditEventType eventType, String resource,
                      String result, String clientIp, String userAgent, String operation,
                      String entityId, String sessionId, Map<String, Object> details) {
        long startTime = System.nanoTime();

        try {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .timestamp(Instant.now())
                    .userId(userId)
                    .eventType(eventType)
                    .resource(resource)
                    .result(result)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .operation(operation)
                    .entityId(entityId)
                    .sessionId(sessionId)
                    .details(details)
                    .build();

            // P0优化：使用AtomicReference的CAS操作替代synchronized
            String prevHash;
            String entryHash;
            do {
                prevHash = lastHash.get();
                entry.setPrevHash(prevHash);
                entryHash = hashChainCalculator.computeHash(entry, prevHash);
            } while (!lastHash.compareAndSet(prevHash, entryHash));
            entry.setEntryHash(entryHash);

            // 数字签名
            signatureService.sign(entry);

            // P0优化：增强的队列入队策略，包含内存监控和降级处理
            boolean success = tryEnqueue(entry);

            totalEntries.incrementAndGet();
            metrics.updateQueueSize(queue.size());

            // 记录成功指标
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            metrics.recordSuccess(elapsedMs);

            log.debug("审计事件已入队: {}", entry.getId());

            // 检查是否需要立即刷盘（高优先级事件）
            if (eventType.isHighRisk() && queue.size() >= 10) {
                flush();
            }

        } catch (Exception e) {
            log.error("记录审计事件失败", e);
            metrics.recordFailure("record-error");
            throw new RuntimeException("审计记录失败", e);
        }
    }

    /**
     * 批量记录审计事件
     */
    public void recordBatch(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (AuditLogEntry entry : entries) {
            record(entry.getUserId(), entry.getEventType(), entry.getResource(),
                    entry.getResult(), entry.getClientIp(), entry.getUserAgent(),
                    entry.getOperation(), entry.getEntityId(), entry.getSessionId(),
                    entry.getDetails());
        }
    }

    /**
     * 手动刷盘
     */
    public void flush() {
        List<AuditLogEntry> batch = drainQueue();

        if (batch.isEmpty()) {
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            storage.batchSave(batch);
            long elapsedMs = System.currentTimeMillis() - startTime;

            metrics.recordBatch(batch.size(), elapsedMs);

            log.info("审计批量刷盘完成，数量: {}, 耗时: {}ms", batch.size(), elapsedMs);
        } catch (Exception e) {
            log.error("审计批量刷盘失败", e);
            metrics.recordStorageError();

            // 重新入队（如果有空间）
            for (AuditLogEntry entry : batch) {
                queue.offer(entry);
            }
        }
    }

    /**
     * 定时刷盘任务
     */
    @Scheduled(fixedDelayString = "${basebackend.logging.audit.flush-interval:500}")
    private void scheduledFlush() {
        if (!isShuttingDown.get()) {
            try {
                flush();
            } catch (Exception e) {
                log.error("定时刷盘任务异常", e);
            }
        }
    }

    /**
     * 检查审计队列状态
     */
    public AuditQueueStatus getQueueStatus() {
        int currentSize = queue.size();
        int percentFull = (int) ((currentSize * 100L) / queueCapacity);

        return AuditQueueStatus.builder()
                .currentSize(currentSize)
                .queueCapacity(queueCapacity)
                .percentFull(percentFull)
                .totalEntries(totalEntries.get())
                .lastHash(lastHash.get())
                .droppedEntries(droppedEntries.get())
                .needsFlush(currentSize >= batchSize / 2)
                .build();
    }

    /**
     * 关闭审计服务
     */
    public void shutdown() {
        log.info("开始关闭审计服务...");

        isShuttingDown.set(true);

        // 停止定时任务
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("等待定时任务结束被中断", e);
            Thread.currentThread().interrupt();
        }

        // 最后一次刷盘
        flush();

        // 关闭存储
        storage.close();

        // 关闭 I/O 线程池
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("等待 I/O 线程结束被中断", e);
            Thread.currentThread().interrupt();
        }

        log.info("审计服务已关闭，总记录条目: {}", totalEntries.get());
    }

    /**
     * 清空队列
     */
    public void clearQueue() {
        queue.clear();
        metrics.updateQueueSize(0);
        log.info("审计队列已清空");
    }

    /**
     * 排空队列
     */
    private List<AuditLogEntry> drainQueue() {
        List<AuditLogEntry> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);

        metrics.updateQueueSize(queue.size());

        return batch;
    }
    
    /**
     * P0优化：增强的入队策略，包含内存监控和降级处理
     * 
     * @param entry 审计日志条目
     * @return 是否成功入队
     */
    private boolean tryEnqueue(AuditLogEntry entry) throws InterruptedException {
        // 检查内存使用率
        if (isMemoryPressureHigh()) {
            // 内存压力大时，尝试立即刷盘
            log.warn("内存压力过高，触发紧急刷盘");
            flush();
        }
        
        // 检查队列使用率
        int currentSize = queue.size();
        int percentFull = (int) ((currentSize * 100L) / queueCapacity);
        
        if (percentFull >= 95) {
            // 队列几乎满了，执行降级策略
            return handleQueueOverflow(entry, percentFull);
        }
        
        // 正常入队
        boolean success = queue.offer(entry, 5, TimeUnit.SECONDS);
        if (!success) {
            return handleQueueOverflow(entry, 100);
        }
        
        return true;
    }
    
    /**
     * P0优化：处理队列溢出的降级策略
     */
    private boolean handleQueueOverflow(AuditLogEntry entry, int percentFull) {
        // 策略1：尝试紧急刷盘
        flush();
        
        // 再次尝试入队
        boolean success = queue.offer(entry);
        if (success) {
            log.info("紧急刷盘后成功入队，队列使用率: {}%", percentFull);
            return true;
        }
        
        // 策略2：根据事件优先级决定是否丢弃
        if (entry.getEventType() != null && entry.getEventType().isHighRisk()) {
            // 高风险事件不能丢弃，阻塞等待
            try {
                success = queue.offer(entry, 30, TimeUnit.SECONDS);
                if (!success) {
                    log.error("高风险审计事件入队超时，事件ID: {}", entry.getId());
                    metrics.recordFailure("high-risk-queue-timeout");
                    droppedEntries.incrementAndGet();
                    return false;
                }
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("高风险审计事件入队被中断", e);
                return false;
            }
        }
        
        // 策略3：低优先级事件可以丢弃，但记录指标
        droppedEntries.incrementAndGet();
        metrics.recordFailure("queue-full-dropped");
        log.warn("审计队列已满，丢弃低优先级事件，事件ID: {}, 已丢弃总数: {}", 
                entry.getId(), droppedEntries.get());
        
        return false;
    }
    
    /**
     * P0优化：检查内存压力
     */
    private boolean isMemoryPressureHigh() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double) usedMemory / maxMemory;
        
        return memoryUsage > MEMORY_THRESHOLD;
    }
    
    /**
     * P0优化：获取丢弃的条目数
     */
    public long getDroppedEntries() {
        return droppedEntries.get();
    }

    /**
     * 启动定时任务
     */
    private void startScheduler() {
        // 定时刷盘
        scheduler.scheduleAtFixedRate(
                this::scheduledFlush,
                flushIntervalMs,
                flushIntervalMs,
                TimeUnit.MILLISECONDS
        );

        // 定时检查密钥轮换
        scheduler.scheduleAtFixedRate(
                this::checkKeyRotation,
                1,
                1,
                TimeUnit.HOURS
        );

        // 定时健康检查
        scheduler.scheduleAtFixedRate(
                this::healthCheck,
                30,
                30,
                TimeUnit.SECONDS
        );
    }

    /**
     * 检查密钥轮换
     */
    private void checkKeyRotation() {
        try {
            if (signatureService.needsKeyRotation()) {
                log.info("检测到需要密钥轮换");
                signatureService.rotateKey();
                log.info("密钥轮换完成");
            }
        } catch (Exception e) {
            log.error("密钥轮换失败", e);
        }
    }

    /**
     * 健康检查
     */
    private void healthCheck() {
        try {
            AuditQueueStatus status = getQueueStatus();
            boolean isHealthy = metrics.isHealthy();

            if (!isHealthy) {
                log.warn("审计系统健康检查失败: {}", status);
            }

            if (status.percentFull > 90) {
                log.warn("审计队列使用率过高: {}%", status.percentFull);
            }
        } catch (Exception e) {
            log.error("健康检查异常", e);
        }
    }

    /**
     * 审计队列状态
     */
    public static class AuditQueueStatus {
        private final int currentSize;
        private final int queueCapacity;
        private final int percentFull;
        private final long totalEntries;
        private final String lastHash;
        private final long droppedEntries;
        private final boolean needsFlush;

        private AuditQueueStatus(Builder builder) {
            this.currentSize = builder.currentSize;
            this.queueCapacity = builder.queueCapacity;
            this.percentFull = builder.percentFull;
            this.totalEntries = builder.totalEntries;
            this.lastHash = builder.lastHash;
            this.droppedEntries = builder.droppedEntries;
            this.needsFlush = builder.needsFlush;
        }

        public static Builder builder() {
            return new Builder();
        }
        
        // P0优化：添加getter方法以便外部访问状态
        public int getCurrentSize() { return currentSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public int getPercentFull() { return percentFull; }
        public long getTotalEntries() { return totalEntries; }
        public String getLastHash() { return lastHash; }
        public long getDroppedEntries() { return droppedEntries; }
        public boolean isNeedsFlush() { return needsFlush; }

        public static class Builder {
            private int currentSize;
            private int queueCapacity;
            private int percentFull;
            private long totalEntries;
            private String lastHash;
            private long droppedEntries;
            private boolean needsFlush;

            public Builder currentSize(int currentSize) {
                this.currentSize = currentSize;
                return this;
            }

            public Builder queueCapacity(int queueCapacity) {
                this.queueCapacity = queueCapacity;
                return this;
            }

            public Builder percentFull(int percentFull) {
                this.percentFull = percentFull;
                return this;
            }

            public Builder totalEntries(long totalEntries) {
                this.totalEntries = totalEntries;
                return this;
            }

            public Builder lastHash(String lastHash) {
                this.lastHash = lastHash;
                return this;
            }
            
            public Builder droppedEntries(long droppedEntries) {
                this.droppedEntries = droppedEntries;
                return this;
            }

            public Builder needsFlush(boolean needsFlush) {
                this.needsFlush = needsFlush;
                return this;
            }

            public AuditQueueStatus build() {
                return new AuditQueueStatus(this);
            }
        }

        @Override
        public String toString() {
            return String.format(
                "队列状态: %d/%d (%d%%), 总条目: %d, 丢弃: %d, 需要刷盘: %s",
                currentSize, queueCapacity, percentFull, totalEntries, droppedEntries, needsFlush
            );
        }
    }
}
