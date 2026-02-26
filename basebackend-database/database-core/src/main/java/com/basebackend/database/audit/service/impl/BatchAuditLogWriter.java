package com.basebackend.database.audit.service.impl;

import com.basebackend.database.audit.entity.AuditLog;
import com.basebackend.database.audit.mapper.AuditLogMapper;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审计日志批量写入器
 * 
 * 功能特性：
 * 1. 异步批量写入，减少数据库压力
 * 2. 可配置的批量大小和刷新间隔
 * 3. 优雅关闭，确保数据不丢失
 * 4. 性能统计和监控
 * 
 * 性能优化：
 * - 使用 BlockingQueue 缓冲日志
 * - 批量 INSERT 减少数据库往返
 * - 定时刷新确保日志及时持久化
 */
@Slf4j
@Component
public class BatchAuditLogWriter {

    private final AuditLogMapper auditLogMapper;
    private final DatabaseEnhancedProperties properties;

    // 日志缓冲队列
    private final BlockingQueue<AuditLog> logQueue;

    // 批量大小
    private static final int DEFAULT_BATCH_SIZE = 100;

    // 队列容量
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;

    // 运行状态
    private final AtomicBoolean running = new AtomicBoolean(true);

    // 性能统计
    private static final AtomicLong TOTAL_QUEUED = new AtomicLong(0);
    private static final AtomicLong TOTAL_WRITTEN = new AtomicLong(0);
    private static final AtomicLong TOTAL_BATCHES = new AtomicLong(0);
    private static final AtomicLong QUEUE_OVERFLOW_COUNT = new AtomicLong(0);
    private static final AtomicLong WRITE_ERRORS = new AtomicLong(0);

    public BatchAuditLogWriter(AuditLogMapper auditLogMapper,
            DatabaseEnhancedProperties properties) {
        this.auditLogMapper = auditLogMapper;
        this.properties = properties;

        int queueCapacity = properties.getAudit().getThreadPool().getQueueCapacity();
        this.logQueue = new LinkedBlockingQueue<>(
                queueCapacity > 0 ? queueCapacity : DEFAULT_QUEUE_CAPACITY);

        log.info("BatchAuditLogWriter initialized with queue capacity: {}",
                queueCapacity > 0 ? queueCapacity : DEFAULT_QUEUE_CAPACITY);
    }

    /**
     * 添加审计日志到队列
     * 
     * @param auditLog 审计日志
     * @return true 如果成功加入队列
     */
    public boolean enqueue(AuditLog auditLog) {
        if (!running.get()) {
            log.warn("BatchAuditLogWriter is shutting down, rejecting new log");
            return false;
        }

        boolean offered = logQueue.offer(auditLog);
        if (offered) {
            TOTAL_QUEUED.incrementAndGet();
        } else {
            QUEUE_OVERFLOW_COUNT.incrementAndGet();
            log.warn("Audit log queue is full, log dropped. Consider increasing queue capacity.");
        }
        return offered;
    }

    /**
     * 定时刷新队列中的日志到数据库
     * 每5秒执行一次
     */
    @Scheduled(fixedDelay = 5000)
    public void flushLogs() {
        if (!properties.getAudit().isEnabled() || !properties.getAudit().isAsync()) {
            return;
        }

        flush();
    }

    /**
     * 执行批量写入
     */
    public void flush() {
        if (logQueue.isEmpty()) {
            return;
        }

        List<AuditLog> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        int drained = logQueue.drainTo(batch, DEFAULT_BATCH_SIZE);

        if (drained == 0) {
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            // 为没有 ID 的日志生成 ID（使用雪花算法）
            for (AuditLog auditLog : batch) {
                if (auditLog.getId() == null) {
                    auditLog.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
                }
            }

            // 批量插入
            auditLogMapper.insertBatch(batch);

            long duration = System.currentTimeMillis() - startTime;
            TOTAL_WRITTEN.addAndGet(drained);
            TOTAL_BATCHES.incrementAndGet();

            log.debug("Batch audit log write completed: count={}, duration={}ms, queueSize={}",
                    drained, duration, logQueue.size());

        } catch (Exception e) {
            WRITE_ERRORS.incrementAndGet();
            log.error("Failed to write audit log batch: count={}, error={}", drained, e.getMessage(), e);

            // 写入失败时，尝试逐条写入
            for (AuditLog auditLog : batch) {
                try {
                    auditLogMapper.insert(auditLog);
                    TOTAL_WRITTEN.incrementAndGet();
                } catch (Exception ex) {
                    log.error("Failed to write single audit log: {}", ex.getMessage());
                }
            }
        }
    }

    /**
     * 优雅关闭，确保所有日志都被写入
     */
    @PreDestroy
    public void shutdown() {
        log.info("BatchAuditLogWriter shutting down, flushing remaining logs...");
        running.set(false);

        // 刷新所有剩余日志
        while (!logQueue.isEmpty()) {
            flush();
        }

        log.info("BatchAuditLogWriter shutdown complete. Stats: queued={}, written={}, batches={}, errors={}",
                TOTAL_QUEUED.get(), TOTAL_WRITTEN.get(), TOTAL_BATCHES.get(), WRITE_ERRORS.get());
    }

    /**
     * 获取当前队列大小
     */
    public int getQueueSize() {
        return logQueue.size();
    }

    /**
     * 获取性能统计
     */
    public static java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalQueued", TOTAL_QUEUED.get());
        stats.put("totalWritten", TOTAL_WRITTEN.get());
        stats.put("totalBatches", TOTAL_BATCHES.get());
        stats.put("queueOverflowCount", QUEUE_OVERFLOW_COUNT.get());
        stats.put("writeErrors", WRITE_ERRORS.get());
        stats.put("avgBatchSize", TOTAL_BATCHES.get() > 0 ? (double) TOTAL_WRITTEN.get() / TOTAL_BATCHES.get() : 0);
        return stats;
    }
}
