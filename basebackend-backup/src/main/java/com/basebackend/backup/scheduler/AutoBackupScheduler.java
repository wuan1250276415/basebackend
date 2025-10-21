package com.basebackend.backup.scheduler;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.service.MySQLBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动备份调度器
 *
 * @author BaseBackend
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "backup", name = "auto-backup-enabled", havingValue = "true")
public class AutoBackupScheduler {

    private final MySQLBackupService backupService;
    private final BackupProperties backupProperties;

    /**
     * 自动全量备份（每天凌晨2点）
     */
    @Scheduled(cron = "${backup.auto-backup-cron:0 0 2 * * ?}")
    public void autoFullBackup() {
        if (!backupProperties.isEnabled()) {
            return;
        }

        log.info("开始执行自动全量备份任务");
        try {
            backupService.fullBackup();
            log.info("自动全量备份任务完成");
        } catch (Exception e) {
            log.error("自动全量备份任务失败", e);
        }
    }

    /**
     * 自动清理过期备份（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoCleanExpired() {
        if (!backupProperties.isEnabled()) {
            return;
        }

        log.info("开始执行自动清理过期备份任务");
        try {
            int count = backupService.cleanExpiredBackups();
            log.info("自动清理过期备份任务完成，清理 {} 个备份", count);
        } catch (Exception e) {
            log.error("自动清理过期备份任务失败", e);
        }
    }

    /**
     * 自动增量备份（每小时）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoIncrementalBackup() {
        if (!backupProperties.isEnabled() || !backupProperties.isIncrementalBackupEnabled()) {
            return;
        }

        log.info("开始执行自动增量备份任务");
        try {
            backupService.incrementalBackup();
            log.info("自动增量备份任务完成");
        } catch (Exception e) {
            log.error("自动增量备份任务失败", e);
        }
    }
}
