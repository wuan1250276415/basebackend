package com.basebackend.logging.audit.storage;

import com.basebackend.logging.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 复合审计存储实现
 *
 * 支持多级存储策略：
 * - 主存储：高性能存储（本地文件）
 * - 次存储：冗余存储（Redis、数据库）
 * - 自动故障转移
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class CompositeAuditStorage implements AuditStorage {

    private final AuditStorage primary;
    private final List<AuditStorage> secondaries;
    private final ExecutorService executorService;

    public CompositeAuditStorage(AuditStorage primary, List<AuditStorage> secondaries) {
        this.primary = primary;
        this.secondaries = secondaries != null ? secondaries : new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public void save(AuditLogEntry entry) throws StorageException {
        try {
            primary.save(entry);
            replicateAsync(entry);
        } catch (StorageException e) {
            log.error("主存储保存失败，尝试备用存储", e);
            replicateToSecondaries(entry);
            throw e;
        }
    }

    @Override
    public void batchSave(List<AuditLogEntry> entries) throws StorageException {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        try {
            primary.batchSave(entries);
            replicateBatchAsync(entries);
        } catch (StorageException e) {
            log.error("主存储批量保存失败，尝试备用存储", e);
            replicateBatchToSecondaries(entries);
            throw e;
        }
    }

    @Override
    public AuditLogEntry findById(String id) throws StorageException {
        try {
            return primary.findById(id);
        } catch (StorageException e) {
            log.warn("主存储查询失败，尝试备用存储", e);
            return findInSecondaries(id);
        }
    }

    @Override
    public List<AuditLogEntry> findByTimeRange(long startTime, long endTime, int limit) throws StorageException {
        try {
            return primary.findByTimeRange(startTime, endTime, limit);
        } catch (StorageException e) {
            log.warn("主存储查询失败，尝试备用存储", e);
            return findInSecondaries(startTime, endTime, limit);
        }
    }

    @Override
    public List<AuditLogEntry> findByUserId(String userId, int limit) throws StorageException {
        try {
            return primary.findByUserId(userId, limit);
        } catch (StorageException e) {
            log.warn("主存储查询失败，尝试备用存储", e);
            return findInSecondaries(userId, limit);
        }
    }

    @Override
    public List<AuditLogEntry> findByEventType(String eventType, int limit) throws StorageException {
        try {
            return primary.findByEventType(eventType, limit);
        } catch (StorageException e) {
            log.warn("主存储查询失败，尝试备用存储", e);
            return findInSecondaries(eventType, limit);
        }
    }

    @Override
    public boolean verify() throws StorageException {
        return primary.verify();
    }

    @Override
    public int cleanup(int retentionDays) throws StorageException {
        int totalDeleted = 0;
        try {
            totalDeleted += primary.cleanup(retentionDays);
            for (AuditStorage storage : secondaries) {
                totalDeleted += storage.cleanup(retentionDays);
            }
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
        return totalDeleted;
    }

    @Override
    public StorageStats getStats() throws StorageException {
        return primary.getStats();
    }

    @Override
    public void close() {
        try {
            primary.close();
            for (AuditStorage storage : secondaries) {
                storage.close();
            }
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * 异步复制到备用存储
     */
    private void replicateAsync(AuditLogEntry entry) {
        if (secondaries.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                replicateToSecondaries(entry);
            } catch (Exception e) {
                log.error("异步复制审计日志失败", e);
            }
        }, executorService);
    }

    /**
     * 异步批量复制到备用存储
     */
    private void replicateBatchAsync(List<AuditLogEntry> entries) {
        if (secondaries.isEmpty() || entries.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                replicateBatchToSecondaries(entries);
            } catch (Exception e) {
                log.error("异步批量复制审计日志失败", e);
            }
        }, executorService);
    }

    /**
     * 复制到所有备用存储
     */
    private void replicateToSecondaries(AuditLogEntry entry) {
        for (AuditStorage storage : secondaries) {
            try {
                storage.save(entry);
                log.debug("成功复制审计日志到备用存储: {}", storage.getClass().getSimpleName());
            } catch (StorageException e) {
                log.error("复制到备用存储失败: {}", storage.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 批量复制到所有备用存储
     */
    private void replicateBatchToSecondaries(List<AuditLogEntry> entries) {
        for (AuditStorage storage : secondaries) {
            try {
                storage.batchSave(entries);
                log.debug("成功批量复制审计日志到备用存储: {}", storage.getClass().getSimpleName());
            } catch (StorageException e) {
                log.error("批量复制到备用存储失败: {}", storage.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 在备用存储中查询
     */
    private AuditLogEntry findInSecondaries(String id) {
        for (AuditStorage storage : secondaries) {
            try {
                AuditLogEntry entry = storage.findById(id);
                if (entry != null) {
                    log.info("从备用存储找到审计日志: {}", storage.getClass().getSimpleName());
                    return entry;
                }
            } catch (StorageException e) {
                log.warn("备用存储查询失败: {}", storage.getClass().getSimpleName(), e);
            }
        }
        return null;
    }

    private List<AuditLogEntry> findInSecondaries(long startTime, long endTime, int limit) {
        for (AuditStorage storage : secondaries) {
            try {
                List<AuditLogEntry> entries = storage.findByTimeRange(startTime, endTime, limit);
                if (!entries.isEmpty()) {
                    log.info("从备用存储找到审计日志: {}", storage.getClass().getSimpleName());
                    return entries;
                }
            } catch (StorageException e) {
                log.warn("备用存储查询失败: {}", storage.getClass().getSimpleName(), e);
            }
        }
        return new ArrayList<>();
    }

    private List<AuditLogEntry> findInSecondaries(String eventType, int limit) {
        for (AuditStorage storage : secondaries) {
            try {
                List<AuditLogEntry> entries = storage.findByEventType(eventType, limit);
                if (!entries.isEmpty()) {
                    log.info("从备用存储找到审计日志: {}", storage.getClass().getSimpleName());
                    return entries;
                }
            } catch (StorageException e) {
                log.warn("备用存储查询失败: {}", storage.getClass().getSimpleName(), e);
            }
        }
        return new ArrayList<>();
    }
}
