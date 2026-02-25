package com.basebackend.database.audit.scheduler;

import com.basebackend.database.audit.service.AuditLogArchiveService;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计日志清理定时任务
 * 定期归档和清理过期的审计日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "database.enhanced.audit.archive",
    name = "auto-cleanup-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AuditLogCleanupScheduler {

    private final AuditLogArchiveService auditLogArchiveService;
    private final DatabaseEnhancedProperties properties;

    /**
     * 定时归档和清理审计日志
     * 默认每天凌晨2点执行
     */
    @Scheduled(cron = "${database.enhanced.audit.archive.cleanup-cron:0 0 2 * * ?}")
    public void cleanupAuditLogs() {
        log.info("Starting scheduled audit log cleanup task");
        
        try {
            DatabaseEnhancedProperties.AuditProperties auditProps = properties.getAudit();
            DatabaseEnhancedProperties.ArchiveProperties archiveProps = auditProps.getArchive();
            
            int retentionDays = auditProps.getRetentionDays();
            
            if (archiveProps.isEnabled()) {
                // 归档模式：先归档再删除
                log.info("Archive mode enabled, archiving logs older than {} days", retentionDays);
                int archivedCount = auditLogArchiveService.archiveExpiredLogs(retentionDays);
                log.info("Archived {} audit logs", archivedCount);
                
                // 清理归档表中的过期数据
                int archiveRetentionDays = archiveProps.getArchiveRetentionDays();
                log.info("Cleaning archived logs older than {} days", archiveRetentionDays);
                int cleanedArchiveCount = auditLogArchiveService.cleanExpiredArchives(archiveRetentionDays);
                log.info("Cleaned {} expired archived logs", cleanedArchiveCount);
            } else {
                // 直接删除模式
                log.info("Archive mode disabled, directly cleaning logs older than {} days", retentionDays);
                int cleanedCount = auditLogArchiveService.cleanExpiredLogs(retentionDays);
                log.info("Cleaned {} audit logs", cleanedCount);
            }
            
            log.info("Scheduled audit log cleanup task completed successfully");
        } catch (Exception e) {
            log.error("Error occurred during scheduled audit log cleanup", e);
        }
    }
}
