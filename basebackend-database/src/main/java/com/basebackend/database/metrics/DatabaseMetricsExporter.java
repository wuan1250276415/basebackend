package com.basebackend.database.metrics;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.interceptor.SqlInjectionPreventionInterceptor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;

/**
 * 数据库模块 Prometheus 指标导出器
 * 
 * 导出指标：
 * - 连接池指标（活跃连接、空闲连接、使用率等）
 * - SQL注入防护统计
 * - 加密操作统计
 * - 审计操作统计
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "database.enhanced.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMetricsExporter {
    
    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final DatabaseEnhancedProperties properties;
    
    private static final String METRIC_PREFIX = "database_enhanced";
    
    public DatabaseMetricsExporter(MeterRegistry meterRegistry, 
                                   DataSource dataSource,
                                   DatabaseEnhancedProperties properties) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        this.properties = properties;
    }
    
    @PostConstruct
    public void registerMetrics() {
        String prefix = properties.getMetrics() != null ? 
            properties.getMetrics().getPrefix() : METRIC_PREFIX;
        
        log.info("Registering database enhanced metrics with prefix: {}", prefix);
        
        // 连接池指标
        if (properties.getMetrics() == null || properties.getMetrics().isConnectionPoolMetrics()) {
            registerConnectionPoolMetrics(prefix);
        }
        
        // SQL注入防护指标
        if (properties.getMetrics() == null || properties.getMetrics().isSqlStatisticsMetrics()) {
            registerSqlInjectionMetrics(prefix);
        }
        
        log.info("Database enhanced metrics registered successfully");
    }
    
    private void registerConnectionPoolMetrics(String prefix) {
        if (!(dataSource instanceof DruidDataSource)) {
            log.warn("DataSource is not DruidDataSource, connection pool metrics unavailable");
            return;
        }
        
        DruidDataSource druid = (DruidDataSource) dataSource;
        Tags tags = Tags.of("datasource", "primary");
        
        // 活跃连接数
        Gauge.builder(prefix + "_connection_pool_active", druid, DruidDataSource::getActiveCount)
            .tags(tags)
            .description("Number of active connections")
            .register(meterRegistry);
        
        // 空闲连接数
        Gauge.builder(prefix + "_connection_pool_idle", druid, DruidDataSource::getPoolingCount)
            .tags(tags)
            .description("Number of idle connections")
            .register(meterRegistry);
        
        // 最大连接数
        Gauge.builder(prefix + "_connection_pool_max", druid, DruidDataSource::getMaxActive)
            .tags(tags)
            .description("Maximum number of connections")
            .register(meterRegistry);
        
        // 使用率
        Gauge.builder(prefix + "_connection_pool_usage_rate", druid, ds -> {
            int max = ds.getMaxActive();
            return max > 0 ? (double) ds.getActiveCount() / max * 100 : 0;
        })
            .tags(tags)
            .description("Connection pool usage rate percentage")
            .register(meterRegistry);
        
        // 等待线程数
        Gauge.builder(prefix + "_connection_pool_wait_threads", druid, DruidDataSource::getWaitThreadCount)
            .tags(tags)
            .description("Number of threads waiting for connection")
            .register(meterRegistry);
        
        // 创建连接数
        Gauge.builder(prefix + "_connection_pool_create_count", druid, DruidDataSource::getCreateCount)
            .tags(tags)
            .description("Total connections created")
            .register(meterRegistry);
        
        // 错误数
        Gauge.builder(prefix + "_connection_pool_error_count", druid, DruidDataSource::getErrorCount)
            .tags(tags)
            .description("Total connection errors")
            .register(meterRegistry);
        
        log.debug("Connection pool metrics registered");
    }
    
    private void registerSqlInjectionMetrics(String prefix) {
        // SQL注入检测统计
        Gauge.builder(prefix + "_sql_injection_total_checks", () -> {
            Map<String, Object> stats = SqlInjectionPreventionInterceptor.getStatistics();
            return ((Number) stats.getOrDefault("totalChecks", 0L)).doubleValue();
        })
            .description("Total SQL injection checks performed")
            .register(meterRegistry);
        
        Gauge.builder(prefix + "_sql_injection_blocked_count", () -> {
            Map<String, Object> stats = SqlInjectionPreventionInterceptor.getStatistics();
            return ((Number) stats.getOrDefault("blockedCount", 0L)).doubleValue();
        })
            .description("Total SQL injection attempts blocked")
            .register(meterRegistry);
        
        Gauge.builder(prefix + "_sql_injection_whitelisted_count", () -> {
            Map<String, Object> stats = SqlInjectionPreventionInterceptor.getStatistics();
            return ((Number) stats.getOrDefault("whitelistedCount", 0L)).doubleValue();
        })
            .description("Total SQL queries whitelisted")
            .register(meterRegistry);
        
        log.debug("SQL injection metrics registered");
    }
}
