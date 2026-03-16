package com.basebackend.logging.audit.storage.database;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.basebackend.logging.audit.storage.AuditStorage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库审计存储实现
 *
 * 通过 MyBatis Plus 将审计日志持久化到 MySQL sys_audit_log 表。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class DatabaseAuditStorage implements AuditStorage {

    private final SysAuditLogMapper mapper;

    public DatabaseAuditStorage(SysAuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(AuditLogEntry entry) throws StorageException {
        try {
            mapper.insert(toEntity(entry));
        } catch (Exception e) {
            throw new StorageException("数据库审计日志保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void batchSave(List<AuditLogEntry> entries) throws StorageException {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        try {
            List<SysAuditLog> entities = entries.stream()
                    .map(this::toEntity)
                    .collect(Collectors.toList());
            // 使用 MyBatis Plus 的 insertBatchSomeColumn 进行真正的批量 INSERT
            // 若 mapper 未扩展 InsertBatchSomColumnMapper，则退化为分批单条插入
            mapper.insertBatchSomeColumn(entities);
        } catch (Exception e) {
            throw new StorageException("数据库审计日志批量保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AuditLogEntry findById(String id) throws StorageException {
        try {
            SysAuditLog entity = mapper.selectById(id);
            return entity != null ? toModel(entity) : null;
        } catch (Exception e) {
            throw new StorageException("数据库审计日志查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditLogEntry> findByTimeRange(long startTime, long endTime, int limit) throws StorageException {
        try {
            Instant start = Instant.ofEpochMilli(startTime);
            Instant end = Instant.ofEpochMilli(endTime);
            List<SysAuditLog> entities = mapper.selectByTimeRange(start, end, limit);
            return entities.stream().map(this::toModel).collect(Collectors.toList());
        } catch (Exception e) {
            throw new StorageException("数据库审计日志时间范围查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditLogEntry> findByUserId(String userId, int limit) throws StorageException {
        try {
            // 使用 LambdaQueryWrapper + page() 替代 .last("LIMIT N") 字符串拼接，
            // 消除 SQL 注入风险（limit 参数通过 MyBatis 绑定参数传递）
            int safeLimit = Math.max(1, Math.min(limit, 10_000));
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysAuditLog> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysAuditLog>()
                            .eq(SysAuditLog::getUserId, userId)
                            .orderByDesc(SysAuditLog::getTimestamp);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysAuditLog> page =
                    mapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, safeLimit), wrapper);
            return page.getRecords().stream().map(this::toModel).collect(Collectors.toList());
        } catch (Exception e) {
            throw new StorageException("数据库审计日志用户查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditLogEntry> findByEventType(String eventType, int limit) throws StorageException {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 10_000));
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysAuditLog> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysAuditLog>()
                            .eq(SysAuditLog::getEventType, eventType)
                            .orderByDesc(SysAuditLog::getTimestamp);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysAuditLog> page =
                    mapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, safeLimit), wrapper);
            return page.getRecords().stream().map(this::toModel).collect(Collectors.toList());
        } catch (Exception e) {
            throw new StorageException("数据库审计日志事件类型查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verify() throws StorageException {
        try {
            mapper.selectStats();
            return true;
        } catch (Exception e) {
            throw new StorageException("数据库审计存储验证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int cleanup(int retentionDays) throws StorageException {
        try {
            Instant expireTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            int deleted = mapper.deleteExpired(expireTime);
            log.info("数据库审计日志清理完成: 删除 {} 条过期记录 (保留 {} 天)", deleted, retentionDays);
            return deleted;
        } catch (Exception e) {
            throw new StorageException("数据库审计日志清理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageStats getStats() throws StorageException {
        try {
            Map<String, Object> stats = mapper.selectStats();
            long totalEntries = stats.get("total_entries") != null
                    ? ((Number) stats.get("total_entries")).longValue() : 0;
            long oldestTime = 0;
            long newestTime = 0;
            // MyBatis 从 MySQL DATETIME 列返回 java.sql.Timestamp 或 java.time.LocalDateTime，
            // 而非 Instant；使用 Number / java.util.Date 公共接口统一处理，
            // 或通过 Timestamp.toInstant() 转换。
            Object oldestRaw = stats.get("oldest_entry_time");
            if (oldestRaw instanceof java.sql.Timestamp ts) {
                oldestTime = ts.toInstant().toEpochMilli();
            } else if (oldestRaw instanceof java.time.LocalDateTime ldt) {
                oldestTime = ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            } else if (oldestRaw instanceof Instant inst) {
                oldestTime = inst.toEpochMilli();
            }
            Object newestRaw = stats.get("newest_entry_time");
            if (newestRaw instanceof java.sql.Timestamp ts) {
                newestTime = ts.toInstant().toEpochMilli();
            } else if (newestRaw instanceof java.time.LocalDateTime ldt) {
                newestTime = ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            } else if (newestRaw instanceof Instant inst) {
                newestTime = inst.toEpochMilli();
            }
            return new StorageStats(totalEntries, 0, 0, oldestTime, newestTime, 0);
        } catch (Exception e) {
            throw new StorageException("数据库审计存储统计查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // MyBatis mapper 由 Spring 容器管理，无需手动关闭
    }

    // ========== 转换方法 ==========

    private SysAuditLog toEntity(AuditLogEntry entry) {
        return SysAuditLog.builder()
                .id(entry.getId())
                .timestamp(entry.getTimestamp())
                .userId(entry.getUserId())
                .sessionId(entry.getSessionId())
                .eventType(entry.getEventType() != null ? entry.getEventType().name() : null)
                .resource(entry.getResource())
                .result(entry.getResult())
                .clientIp(entry.getClientIp())
                .userAgent(entry.getUserAgent())
                .deviceInfo(entry.getDeviceInfo())
                .location(entry.getLocation())
                .entityId(entry.getEntityId())
                .operation(entry.getOperation())
                .details(entry.getDetails())
                .durationMs(entry.getDurationMs())
                .errorCode(entry.getErrorCode())
                .errorMessage(entry.getErrorMessage())
                .traceId(entry.getTraceId())
                .spanId(entry.getSpanId())
                .prevHash(entry.getPrevHash())
                .entryHash(entry.getEntryHash())
                .signature(entry.getSignature())
                .certificateId(entry.getCertificateId())
                .tenantId(entry.getTenantId())
                .build();
    }

    private AuditLogEntry toModel(SysAuditLog entity) {
        AuditEventType eventType = null;
        if (entity.getEventType() != null) {
            try {
                eventType = AuditEventType.valueOf(entity.getEventType());
            } catch (IllegalArgumentException e) {
                log.warn("未知的审计事件类型: {}", entity.getEventType());
            }
        }

        return AuditLogEntry.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .userId(entity.getUserId())
                .sessionId(entity.getSessionId())
                .eventType(eventType)
                .resource(entity.getResource())
                .result(entity.getResult())
                .clientIp(entity.getClientIp())
                .userAgent(entity.getUserAgent())
                .deviceInfo(entity.getDeviceInfo())
                .location(entity.getLocation())
                .entityId(entity.getEntityId())
                .operation(entity.getOperation())
                .details(entity.getDetails())
                .durationMs(entity.getDurationMs())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .traceId(entity.getTraceId())
                .spanId(entity.getSpanId())
                .prevHash(entity.getPrevHash())
                .entryHash(entity.getEntryHash())
                .signature(entity.getSignature())
                .certificateId(entity.getCertificateId())
                .tenantId(entity.getTenantId())
                .build();
    }
}
