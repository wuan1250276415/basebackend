package com.basebackend.database.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.database.audit.entity.AuditLog;
import com.basebackend.database.audit.mapper.AuditLogMapper;
import com.basebackend.database.audit.query.AuditLogQuery;
import com.basebackend.database.audit.service.AuditLogArchiveService;
import com.basebackend.database.audit.service.AuditLogService;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final AuditLogArchiveService auditLogArchiveService;
    private final DatabaseEnhancedProperties properties;

    @Override
    @Async("auditLogExecutor")
    public void logAsync(AuditLog auditLog) {
        try {
            log(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log asynchronously", e);
        }
    }

    @Override
    public void log(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", auditLog, e);
            throw new RuntimeException("Failed to save audit log", e);
        }
    }

    @Override
    public Page<AuditLog> query(AuditLogQuery query) {
        Page<AuditLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getOperationType())) {
            wrapper.eq(AuditLog::getOperationType, query.getOperationType());
        }
        
        if (StringUtils.hasText(query.getTableName())) {
            wrapper.eq(AuditLog::getTableName, query.getTableName());
        }
        
        if (query.getOperatorId() != null) {
            wrapper.eq(AuditLog::getOperatorId, query.getOperatorId());
        }
        
        if (StringUtils.hasText(query.getOperatorName())) {
            wrapper.like(AuditLog::getOperatorName, query.getOperatorName());
        }
        
        if (StringUtils.hasText(query.getTenantId())) {
            wrapper.eq(AuditLog::getTenantId, query.getTenantId());
        }
        
        if (query.getStartTime() != null) {
            wrapper.ge(AuditLog::getOperateTime, query.getStartTime());
        }
        
        if (query.getEndTime() != null) {
            wrapper.le(AuditLog::getOperateTime, query.getEndTime());
        }
        
        wrapper.orderByDesc(AuditLog::getOperateTime);
        
        return auditLogMapper.selectPage(page, wrapper);
    }

    @Override
    public int archiveExpiredLogs(int retentionDays) {
        DatabaseEnhancedProperties.ArchiveProperties archiveProps = 
            properties.getAudit().getArchive();
        
        if (archiveProps.isEnabled()) {
            // 使用归档服务进行归档
            return auditLogArchiveService.archiveExpiredLogs(retentionDays);
        } else {
            // 直接删除过期日志
            return auditLogArchiveService.cleanExpiredLogs(retentionDays);
        }
    }
}
