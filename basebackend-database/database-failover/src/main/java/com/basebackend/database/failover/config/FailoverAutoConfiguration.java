package com.basebackend.database.failover.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.dynamic.DynamicDataSource;
import com.basebackend.database.failover.DataSourceFailoverHandler;
import com.basebackend.database.failover.DataSourceRecoveryManager;
import com.basebackend.database.failover.integration.DatabaseFailoverIntegrationImpl;
import com.basebackend.database.health.indicator.DataSourceHealthIndicator;
import com.basebackend.database.health.spi.DatabaseFailoverIntegration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 故障转移自动配置
 *
 * @author basebackend
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(DynamicDataSource.class)
@ConditionalOnProperty(prefix = "database.enhanced.failover", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FailoverAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSourceFailoverHandler dataSourceFailoverHandler(
            DatabaseEnhancedProperties properties,
            DataSourceHealthIndicator healthIndicator,
            DynamicDataSource dynamicDataSource) {
        log.info("Initializing DataSourceFailoverHandler");
        return new DataSourceFailoverHandler(properties, healthIndicator, dynamicDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRecoveryManager dataSourceRecoveryManager(
            DatabaseEnhancedProperties properties,
            DataSourceFailoverHandler failoverHandler,
            DynamicDataSource dynamicDataSource) {
        log.info("Initializing DataSourceRecoveryManager");
        return new DataSourceRecoveryManager(properties, failoverHandler, dynamicDataSource);
    }

    /**
     * 注册故障转移集成 Bean，供 database-core 的 HealthCheckScheduler 使用
     */
    @Bean
    @ConditionalOnMissingBean
    public DatabaseFailoverIntegration databaseFailoverIntegration(
            DataSourceFailoverHandler failoverHandler,
            DataSourceRecoveryManager recoveryManager) {
        log.info("Registering DatabaseFailoverIntegration for health check scheduler");
        return new DatabaseFailoverIntegrationImpl(failoverHandler, recoveryManager);
    }
}
