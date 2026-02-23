package com.basebackend.logging.audit.scheduler;

import com.basebackend.logging.audit.config.AuditProperties;
import com.basebackend.logging.audit.storage.AuditStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 审计日志保留期调度器
 *
 * 定期清理超过保留期的审计日志，防止存储无限增长。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class AuditRetentionScheduler {

    private final AuditStorage storage;
    private final int retentionDays;

    public AuditRetentionScheduler(AuditStorage storage, int retentionDays) {
        this.storage = storage;
        this.retentionDays = retentionDays;
    }

    /**
     * 每天凌晨 2 点执行清理（可通过配置覆盖）
     */
    @Scheduled(cron = "${basebackend.logging.audit.retention.cron:0 0 2 * * ?}")
    public void executeCleanup() {
        log.info("开始审计日志保留期清理，保留天数: {}", retentionDays);
        try {
            int deleted = storage.cleanup(retentionDays);
            log.info("审计日志保留期清理完成，删除 {} 条过期记录", deleted);
        } catch (Exception e) {
            log.error("审计日志保留期清理失败", e);
        }
    }
}
