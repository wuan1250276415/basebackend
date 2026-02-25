package com.basebackend.database.tenant.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.tenant.router.TenantDataSourceRouter;
import com.basebackend.database.tenant.service.TenantConfigService;
import com.basebackend.database.tenant.service.TenantDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;

/**
 * 租户数据源配置
 * 配置租户数据源路由器和相关服务
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true")
public class TenantDataSourceConfig {
    
    private final DatabaseEnhancedProperties properties;
    private final TenantConfigService tenantConfigService;
    private final TenantDataSourceService tenantDataSourceService;
    
    /**
     * Constructor with @Lazy to break circular dependency with SqlSessionFactory
     */
    public TenantDataSourceConfig(
            DatabaseEnhancedProperties properties,
            @Lazy TenantConfigService tenantConfigService,
            @Lazy TenantDataSourceService tenantDataSourceService) {
        this.properties = properties;
        this.tenantConfigService = tenantConfigService;
        this.tenantDataSourceService = tenantDataSourceService;
    }
    
    /**
     * 创建租户数据源路由器
     * 
     * @param defaultDataSource 默认数据源
     * @return 租户数据源路由器
     */
    @Bean
    public TenantDataSourceRouter tenantDataSourceRouter(DataSource defaultDataSource) {
        log.info("Initializing TenantDataSourceRouter");
        
        TenantDataSourceRouter router = new TenantDataSourceRouter(tenantConfigService, properties);
        
        // 设置默认数据源
        router.setDefaultTargetDataSource(defaultDataSource);
        
        log.info("TenantDataSourceRouter initialized successfully");
        return router;
    }
    
    /**
     * 应用启动完成后初始化租户数据源
     * 监听 ApplicationReadyEvent 事件，在应用完全启动后初始化租户数据源
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready, initializing tenant data sources...");
        
        try {
            // 初始化所有激活租户的数据源
            tenantDataSourceService.initializeTenantDataSources();
            
            log.info("Tenant data sources initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize tenant data sources", e);
            // 不抛出异常，避免影响应用启动
        }
    }
}
