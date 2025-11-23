package com.basebackend.database.tenant.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.tenant.handler.TenantMetaObjectHandler;
import com.basebackend.database.tenant.interceptor.TenantInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多租户自动配置类
 * 根据配置启用多租户功能
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true")
public class MultiTenancyAutoConfiguration {
    
    private final DatabaseEnhancedProperties properties;
    
    public MultiTenancyAutoConfiguration(DatabaseEnhancedProperties properties) {
        this.properties = properties;
        log.info("Multi-Tenancy Module initialized");
        log.info("  - Isolation Mode: {}", properties.getMultiTenancy().getIsolationMode());
        log.info("  - Tenant Column: {}", properties.getMultiTenancy().getTenantColumn());
        log.info("  - Excluded Tables: {}", properties.getMultiTenancy().getExcludedTables());
    }
    
    /**
     * 租户拦截器
     * 自动为 SQL 添加租户过滤条件
     */
    @Bean
    public TenantInterceptor tenantInterceptor() {
        log.info("Registering TenantInterceptor");
        return new TenantInterceptor(properties);
    }
    
    /**
     * 租户字段自动填充处理器
     * 在插入数据时自动填充租户 ID
     */
    @Bean
    public TenantMetaObjectHandler tenantMetaObjectHandler() {
        log.info("Registering TenantMetaObjectHandler");
        return new TenantMetaObjectHandler(properties);
    }
}
