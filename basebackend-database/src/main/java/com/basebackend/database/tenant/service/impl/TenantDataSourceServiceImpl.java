package com.basebackend.database.tenant.service.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.entity.TenantConfig;
import com.basebackend.database.tenant.router.TenantDataSourceRouter;
import com.basebackend.database.tenant.service.TenantConfigService;
import com.basebackend.database.tenant.service.TenantDataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 租户数据源服务实现
 */
@Slf4j
@Service
@ConditionalOnBean(TenantDataSourceRouter.class)
@RequiredArgsConstructor
public class TenantDataSourceServiceImpl implements TenantDataSourceService {
    
    private final TenantDataSourceRouter tenantDataSourceRouter;
    private final TenantConfigService tenantConfigService;
    private final DatabaseEnhancedProperties properties;
    private final DataSourceProperties dataSourceProperties;
    
    @Value("${spring.datasource.druid.initial-size:5}")
    private int initialSize;
    
    @Value("${spring.datasource.druid.min-idle:5}")
    private int minIdle;
    
    @Value("${spring.datasource.druid.max-active:20}")
    private int maxActive;
    
    @Value("${spring.datasource.druid.max-wait:60000}")
    private long maxWait;
    
    @Override
    public void registerTenantDataSource(String tenantId, DataSource dataSource) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source cannot be null");
        }
        
        log.info("Registering data source for tenant: {}", tenantId);
        
        // 验证数据源连接
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                throw new TenantContextException("Data source connection is not valid for tenant: " + tenantId);
            }
        } catch (SQLException e) {
            log.error("Failed to validate data source for tenant: {}", tenantId, e);
            throw new TenantContextException("Failed to validate data source for tenant: " + tenantId, e);
        }
        
        // 注册到路由器
        tenantDataSourceRouter.addTenantDataSource(tenantId, dataSource);
        
        log.info("Successfully registered data source for tenant: {}", tenantId);
    }
    
    @Override
    public void unregisterTenantDataSource(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        log.info("Unregistering data source for tenant: {}", tenantId);
        
        // 获取数据源
        DataSource dataSource = tenantDataSourceRouter.getTenantDataSource(tenantId);
        
        // 从路由器移除
        tenantDataSourceRouter.removeTenantDataSource(tenantId);
        
        // 关闭数据源（如果是 DruidDataSource）
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
            log.info("Closed DruidDataSource for tenant: {}", tenantId);
        }
        
        log.info("Successfully unregistered data source for tenant: {}", tenantId);
    }
    
    @Override
    public DataSource getTenantDataSource(String tenantId) {
        return tenantDataSourceRouter.getTenantDataSource(tenantId);
    }
    
    @Override
    public boolean isDataSourceRegistered(String tenantId) {
        return tenantDataSourceRouter.hasTenantDataSource(tenantId);
    }
    
    @Override
    public void initializeTenantDataSources() {
        if (!properties.getMultiTenancy().isEnabled()) {
            log.info("Multi-tenancy is disabled, skipping tenant data source initialization");
            return;
        }
        
        log.info("Initializing tenant data sources...");
        
        // 获取所有激活的租户
        List<TenantConfig> activeTenants = tenantConfigService.listActiveTenants();
        
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.info("No active tenants found");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (TenantConfig tenantConfig : activeTenants) {
            try {
                // 只为 SEPARATE_DB 模式的租户初始化数据源
                if ("SEPARATE_DB".equals(tenantConfig.getIsolationMode())) {
                    String dataSourceKey = tenantConfig.getDataSourceKey();
                    if (dataSourceKey == null || dataSourceKey.trim().isEmpty()) {
                        log.warn("Data source key not configured for tenant: {}", tenantConfig.getTenantId());
                        failCount++;
                        continue;
                    }
                    
                    // 创建数据源
                    DataSource dataSource = createDataSource(tenantConfig);
                    
                    // 注册数据源
                    registerTenantDataSource(tenantConfig.getTenantId(), dataSource);
                    
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to initialize data source for tenant: {}", tenantConfig.getTenantId(), e);
                failCount++;
            }
        }
        
        log.info("Tenant data source initialization completed. Success: {}, Failed: {}", successCount, failCount);
    }
    
    @Override
    public void refreshTenantDataSource(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        log.info("Refreshing data source for tenant: {}", tenantId);
        
        // 获取租户配置
        TenantConfig tenantConfig = tenantConfigService.getByTenantId(tenantId);
        if (tenantConfig == null) {
            log.warn("Tenant config not found: {}", tenantId);
            throw new TenantContextException("Tenant config not found: " + tenantId);
        }
        
        // 只有 SEPARATE_DB 模式需要刷新数据源
        if (!"SEPARATE_DB".equals(tenantConfig.getIsolationMode())) {
            log.debug("Tenant {} is not using SEPARATE_DB mode, no need to refresh", tenantId);
            return;
        }
        
        // 先注销旧的数据源
        if (isDataSourceRegistered(tenantId)) {
            unregisterTenantDataSource(tenantId);
        }
        
        // 创建新的数据源
        DataSource dataSource = createDataSource(tenantConfig);
        
        // 注册新的数据源
        registerTenantDataSource(tenantId, dataSource);
        
        log.info("Successfully refreshed data source for tenant: {}", tenantId);
    }
    
    @Override
    public boolean validateTenantDataSource(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return false;
        }
        
        DataSource dataSource = getTenantDataSource(tenantId);
        if (dataSource == null) {
            log.warn("Data source not found for tenant: {}", tenantId);
            return false;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            if (valid) {
                log.debug("Data source validation successful for tenant: {}", tenantId);
            } else {
                log.warn("Data source validation failed for tenant: {}", tenantId);
            }
            return valid;
        } catch (SQLException e) {
            log.error("Failed to validate data source for tenant: {}", tenantId, e);
            return false;
        }
    }
    
    /**
     * 创建租户数据源
     * 根据租户配置创建 DruidDataSource
     * 
     * @param tenantConfig 租户配置
     * @return 数据源
     */
    private DataSource createDataSource(TenantConfig tenantConfig) {
        String dataSourceKey = tenantConfig.getDataSourceKey();
        
        // 这里简化处理，实际应该从配置中读取对应的数据源配置
        // 或者从租户配置中读取完整的数据库连接信息
        DruidDataSource dataSource = new DruidDataSource();
        
        // 基本配置（这里使用默认配置，实际应该从租户配置或外部配置读取）
        dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        
        // 注意：这里需要根据实际情况配置 URL、用户名、密码
        // 可以从租户配置的扩展字段中读取，或者从配置中心读取
        // 这里仅作示例
        String url = buildTenantDataSourceUrl(tenantConfig);
        dataSource.setUrl(url);
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        
        // 连接池配置
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);
        
        // 验证配置
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        
        try {
            dataSource.init();
            log.info("Created data source for tenant: {}", tenantConfig.getTenantId());
        } catch (SQLException e) {
            log.error("Failed to initialize data source for tenant: {}", tenantConfig.getTenantId(), e);
            throw new TenantContextException("Failed to initialize data source for tenant: " + tenantConfig.getTenantId(), e);
        }
        
        return dataSource;
    }
    
    /**
     * 构建租户数据源 URL
     * 
     * @param tenantConfig 租户配置
     * @return 数据源 URL
     */
    private String buildTenantDataSourceUrl(TenantConfig tenantConfig) {
        // 这里简化处理，实际应该从租户配置中读取完整的数据库连接信息
        // 或者根据命名规则构建 URL
        
        String baseUrl = dataSourceProperties.getUrl();
        String dataSourceKey = tenantConfig.getDataSourceKey();
        
        // 示例：假设租户数据库名称就是 dataSourceKey
        // jdbc:mysql://localhost:3306/tenant_db_001
        if (baseUrl != null && baseUrl.contains("?")) {
            String[] parts = baseUrl.split("\\?");
            return parts[0].substring(0, parts[0].lastIndexOf("/") + 1) + dataSourceKey + "?" + parts[1];
        } else if (baseUrl != null) {
            return baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1) + dataSourceKey;
        }
        
        // 如果无法解析，抛出异常
        throw new TenantContextException("Cannot build data source URL for tenant: " + tenantConfig.getTenantId());
    }
}
