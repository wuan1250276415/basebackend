package com.basebackend.backup.scheduler;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自动备份调度器
 * <p>
 * 支持所有已注册的数据源（MySQL、PostgreSQL等），
 * 通过注入所有 {@link BackupService} 实现自动执行备份。
 * </p>
 *
 * @author BaseBackend
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "backup", name = "auto-backup-enabled", havingValue = "true")
public class AutoBackupScheduler {

    private final List<BackupService> backupServices;
    private final BackupProperties backupProperties;

    /**
     * 自动全量备份（每天凌晨2点）
     */
    @Scheduled(cron = "${backup.auto-backup-cron:0 0 2 * * ?}")
    public void autoFullBackup() {
        if (!backupProperties.isEnabled()) {
            return;
        }

        for (BackupService service : backupServices) {
            log.info("开始执行自动全量备份任务: datasource={}", service.getDatasourceType());
            try {
                service.fullBackup();
                log.info("自动全量备份任务完成: datasource={}", service.getDatasourceType());
            } catch (Exception e) {
                log.error("自动全量备份任务失败: datasource={}", service.getDatasourceType(), e);
            }
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

        for (BackupService service : backupServices) {
            log.info("开始执行自动清理过期备份任务: datasource={}", service.getDatasourceType());
            try {
                int count = service.cleanExpiredBackups();
                log.info("自动清理过期备份任务完成: datasource={}, 清理 {} 个备份",
                        service.getDatasourceType(), count);
            } catch (Exception e) {
                log.error("自动清理过期备份任务失败: datasource={}", service.getDatasourceType(), e);
            }
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

        for (BackupService service : backupServices) {
            log.info("开始执行自动增量备份任务: datasource={}", service.getDatasourceType());
            try {
                service.incrementalBackup();
                log.info("自动增量备份任务完成: datasource={}", service.getDatasourceType());
            } catch (Exception e) {
                log.error("自动增量备份任务失败: datasource={}", service.getDatasourceType(), e);
            }
        }
    }
}
