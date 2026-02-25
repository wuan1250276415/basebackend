package com.basebackend.database.health.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 健康监控配置类
 * 启用定时任务调度
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "database.enhanced.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HealthMonitoringConfig {

    public HealthMonitoringConfig(DatabaseEnhancedProperties properties) {
        log.info("Health monitoring enabled with check interval: {} seconds", 
                properties.getHealth().getCheckInterval());
        log.info("Slow query threshold: {} ms", 
                properties.getHealth().getSlowQueryThreshold());
        log.info("Connection pool usage threshold: {}%", 
                properties.getHealth().getPoolUsageThreshold());
    }
}
