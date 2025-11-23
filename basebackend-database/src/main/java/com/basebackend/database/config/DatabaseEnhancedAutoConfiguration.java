package com.basebackend.database.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 数据库增强自动配置类
 * 根据配置启用各种增强功能
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DatabaseEnhancedProperties.class)
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class DatabaseEnhancedAutoConfiguration {

    public DatabaseEnhancedAutoConfiguration(DatabaseEnhancedProperties properties) {
        log.info("Database Enhanced Module initialized");
        logEnabledFeatures(properties);
    }

    /**
     * 记录启用的功能
     */
    private void logEnabledFeatures(DatabaseEnhancedProperties properties) {
        log.info("Database Enhanced Features Status:");
        log.info("  - Audit System: {}", properties.getAudit().isEnabled());
        log.info("  - Multi-Tenancy: {}", properties.getMultiTenancy().isEnabled());
        log.info("  - Data Encryption: {}", properties.getSecurity().getEncryption().isEnabled());
        log.info("  - Data Masking: {}", properties.getSecurity().getMasking().isEnabled());
        log.info("  - Health Monitoring: {}", properties.getHealth().isEnabled());
        log.info("  - Dynamic DataSource: {}", properties.getDynamicDatasource().isEnabled());
        log.info("  - Failover: {}", properties.getFailover().isEnabled());
        log.info("  - SQL Statistics: {}", properties.getSqlStatistics().isEnabled());
    }
}
