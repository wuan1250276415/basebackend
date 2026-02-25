package com.basebackend.database.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.database.audit.entity.AuditLog;
import com.basebackend.database.audit.entity.AuditLogArchive;
import com.basebackend.database.audit.mapper.AuditLogArchiveMapper;
import com.basebackend.database.audit.mapper.AuditLogMapper;
import com.basebackend.database.audit.service.AuditLogArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志归档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogArchiveServiceImpl implements AuditLogArchiveService {

    private final AuditLogMapper auditLogMapper;
    private final AuditLogArchiveMapper auditLogArchiveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveExpiredLogs(int retentionDays) {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("Starting to archive audit logs older than {} (retention: {} days)", 
                expirationDate, retentionDays);
        
        // 查询需要归档的日志
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuditLog::getOperateTime, expirationDate);
        
        List<AuditLog> expiredLogs = auditLogMapper.selectList(wrapper);
        
        if (expiredLogs.isEmpty()) {
            log.info("No expired audit logs found for archiving");
            return 0;
        }
        
        log.info("Found {} expired audit logs to archive", expiredLogs.size());
        
        // 转换为归档实体
        List<AuditLogArchive> archives = expiredLogs.stream()
                .map(this::convertToArchive)
                .collect(Collectors.toList());
        
        // 批量插入归档表
        int archivedCount = 0;
        for (AuditLogArchive archive : archives) {
            try {
                auditLogArchiveMapper.insert(archive);
                archivedCount++;
            } catch (Exception e) {
                log.error("Failed to archive audit log with ID: {}", archive.getOriginalLogId(), e);
            }
        }
        
        // 删除已归档的日志
        if (archivedCount > 0) {
            int deletedCount = auditLogMapper.delete(wrapper);
            log.info("Successfully archived {} audit logs and deleted {} from main table", 
                    archivedCount, deletedCount);
        }
        
        return archivedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredLogs(int retentionDays) {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("Starting to clean audit logs older than {} (retention: {} days)", 
                expirationDate, retentionDays);
        
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuditLog::getOperateTime, expirationDate);
        
        int count = auditLogMapper.selectCount(wrapper).intValue();
        
        if (count > 0) {
            int deletedCount = auditLogMapper.delete(wrapper);
            log.info("Successfully cleaned {} expired audit logs", deletedCount);
            return deletedCount;
        }
        
        log.info("No expired audit logs found for cleaning");
        return 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredArchives(int archiveRetentionDays) {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(archiveRetentionDays);
        
        log.info("Starting to clean archived logs older than {} (archive retention: {} days)", 
                expirationDate, archiveRetentionDays);
        
        LambdaQueryWrapper<AuditLogArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuditLogArchive::getArchiveTime, expirationDate);
        
        int count = auditLogArchiveMapper.selectCount(wrapper).intValue();
        
        if (count > 0) {
            int deletedCount = auditLogArchiveMapper.delete(wrapper);
            log.info("Successfully cleaned {} expired archived logs", deletedCount);
            return deletedCount;
        }
        
        log.info("No expired archived logs found for cleaning");
        return 0;
    }

    /**
     * 转换审计日志为归档实体
     */
    private AuditLogArchive convertToArchive(AuditLog auditLog) {
        AuditLogArchive archive = new AuditLogArchive();
        BeanUtils.copyProperties(auditLog, archive);
        archive.setOriginalLogId(auditLog.getId());
        archive.setArchiveTime(LocalDateTime.now());
        archive.setId(null); // 清除ID，让数据库生成新ID
        return archive;
    }
}
