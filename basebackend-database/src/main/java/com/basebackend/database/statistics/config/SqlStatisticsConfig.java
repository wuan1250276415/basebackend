package com.basebackend.database.statistics.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.statistics.collector.SqlStatisticsCollector;
import com.basebackend.database.statistics.interceptor.SqlStatisticsInterceptor;
import com.basebackend.database.statistics.mapper.SqlStatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SQL统计配置
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(prefix = "database.enhanced.sql-statistics", name = "enabled", havingValue = "true")
public class SqlStatisticsConfig {

    /**
     * Register SQL statistics interceptor
     * Use @Lazy to break circular dependency with SqlSessionFactory
     */
    @Bean
    public SqlStatisticsInterceptor sqlStatisticsInterceptor(
            @Lazy SqlStatisticsCollector sqlStatisticsCollector,
            DatabaseEnhancedProperties properties) {
        log.info("Registering SQL statistics interceptor");
        return new SqlStatisticsInterceptor(sqlStatisticsCollector, properties);
    }

    /**
     * Register SQL statistics collector
     * Use @Lazy to break circular dependency with SqlSessionFactory
     */
    @Bean
    public SqlStatisticsCollector sqlStatisticsCollector(
            @Lazy SqlStatisticsMapper sqlStatisticsMapper,
            DatabaseEnhancedProperties properties) {
        log.info("Registering SQL statistics collector");
        return new SqlStatisticsCollector(sqlStatisticsMapper, properties);
    }
}
