package com.basebackend.database.statistics.scheduler;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.statistics.collector.SqlStatisticsCollector;
import com.basebackend.database.statistics.service.SqlStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SQL统计清理调度器
 * 定期清理过期的SQL统计数据
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "database.enhanced.sql-statistics", name = "enabled", havingValue = "true")
public class SqlStatisticsCleanupScheduler {

    private final SqlStatisticsService sqlStatisticsService;
    private final SqlStatisticsCollector sqlStatisticsCollector;
    private final DatabaseEnhancedProperties properties;

    public SqlStatisticsCleanupScheduler(SqlStatisticsService sqlStatisticsService,
                                        SqlStatisticsCollector sqlStatisticsCollector,
                                        DatabaseEnhancedProperties properties) {
        this.sqlStatisticsService = sqlStatisticsService;
        this.sqlStatisticsCollector = sqlStatisticsCollector;
        this.properties = properties;
    }

    /**
     * 清理过期统计数据
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredStatistics() {
        if (!properties.getSqlStatistics().isEnabled()) {
            return;
        }

        log.info("Starting SQL statistics cleanup task");

        try {
            int retentionDays = properties.getSqlStatistics().getRetentionDays();
            int cleanedCount = sqlStatisticsService.cleanExpiredStatistics(retentionDays);
            log.info("SQL statistics cleanup completed. Cleaned {} records", cleanedCount);
        } catch (Exception e) {
            log.error("Failed to clean expired SQL statistics", e);
        }
    }

    /**
     * 刷新缓存到数据库
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void flushCache() {
        if (!properties.getSqlStatistics().isEnabled()) {
            return;
        }

        try {
            sqlStatisticsCollector.flushCache();
            log.debug("SQL statistics cache flushed to database");
        } catch (Exception e) {
            log.error("Failed to flush SQL statistics cache", e);
        }
    }
}
