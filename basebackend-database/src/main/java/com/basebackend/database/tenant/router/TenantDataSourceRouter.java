package com.basebackend.database.tenant.router;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.context.TenantContext;
import com.basebackend.database.tenant.entity.TenantConfig;
import com.basebackend.database.tenant.service.TenantConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户数据源路由器
 * 根据租户配置动态路由到不同的数据源
 * 
 * 支持三种隔离模式：
 * 1. SHARED_DB: 共享数据库，通过 tenant_id 字段隔离（不需要路由）
 * 2. SEPARATE_DB: 独立数据库，每个租户使用独立的数据源
 * 3. SEPARATE_SCHEMA: 独立 Schema，每个租户使用独立的 Schema
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TenantDataSourceRouter extends AbstractRoutingDataSource {
    
    private final TenantConfigService tenantConfigService;
    private final DatabaseEnhancedProperties properties;
    
    public TenantDataSourceRouter(@Lazy TenantConfigService tenantConfigService,
                                  DatabaseEnhancedProperties properties) {
        this.tenantConfigService = tenantConfigService;
        this.properties = properties;
    }
    
    @PostConstruct
    public void init() {
        // Initialize with empty target data sources to satisfy AbstractRoutingDataSource requirement
        Map<Object, Object> targetDataSources = new HashMap<>();
        setTargetDataSources(targetDataSources);
        afterPropertiesSet();
        log.info("TenantDataSourceRouter initialized");
    }
    
    /**
     * 租户数据源缓存
     * Key: tenantId, Value: DataSource
     */
    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    
    /**
     * Schema 缓存
     * Key: tenantId, Value: schemaName
     */
    private final Map<String, String> tenantSchemas = new ConcurrentHashMap<>();
    
    @Override
    protected Object determineCurrentLookupKey() {
        // 如果多租户未启用，返回默认数据源
        if (!properties.getMultiTenancy().isEnabled()) {
            return properties.getDynamicDatasource().getPrimary();
        }
        
        String tenantId = TenantContext.getTenantId();
        
        // 如果租户上下文为空，返回默认数据源
        if (tenantId == null) {
            log.debug("No tenant context, using default data source");
            return properties.getDynamicDatasource().getPrimary();
        }
        
        // 获取租户配置
        TenantConfig tenantConfig = tenantConfigService.getByTenantId(tenantId);
        if (tenantConfig == null) {
            log.warn("Tenant config not found for tenant: {}", tenantId);
            throw new TenantContextException("Tenant config not found: " + tenantId);
        }
        
        // 检查租户状态
        if (!"ACTIVE".equals(tenantConfig.getStatus())) {
            log.warn("Tenant is not active: {}", tenantId);
            throw new TenantContextException("Tenant is not active: " + tenantId);
        }
        
        String isolationMode = tenantConfig.getIsolationMode();
        
        // 根据隔离模式返回数据源键
        switch (isolationMode) {
            case "SHARED_DB":
                // 共享数据库模式，使用默认数据源
                log.debug("Tenant {} using SHARED_DB mode, default data source", tenantId);
                return properties.getDynamicDatasource().getPrimary();
                
            case "SEPARATE_DB":
                // 独立数据库模式，使用租户专属数据源
                String dataSourceKey = tenantConfig.getDataSourceKey();
                if (dataSourceKey == null || dataSourceKey.trim().isEmpty()) {
                    log.error("Data source key not configured for tenant: {}", tenantId);
                    throw new TenantContextException("Data source key not configured for tenant: " + tenantId);
                }
                log.debug("Tenant {} using SEPARATE_DB mode, data source: {}", tenantId, dataSourceKey);
                return dataSourceKey;
                
            case "SEPARATE_SCHEMA":
                // 独立 Schema 模式，使用默认数据源但切换 Schema
                String schemaName = tenantConfig.getSchemaName();
                if (schemaName == null || schemaName.trim().isEmpty()) {
                    log.error("Schema name not configured for tenant: {}", tenantId);
                    throw new TenantContextException("Schema name not configured for tenant: " + tenantId);
                }
                // 缓存 Schema 名称，供后续使用
                tenantSchemas.put(tenantId, schemaName);
                log.debug("Tenant {} using SEPARATE_SCHEMA mode, schema: {}", tenantId, schemaName);
                return properties.getDynamicDatasource().getPrimary();
                
            default:
                log.error("Unknown isolation mode: {} for tenant: {}", isolationMode, tenantId);
                throw new TenantContextException("Unknown isolation mode: " + isolationMode);
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        
        // 如果是 SEPARATE_SCHEMA 模式，需要切换 Schema
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && properties.getMultiTenancy().isEnabled()) {
            TenantConfig tenantConfig = tenantConfigService.getByTenantId(tenantId);
            if (tenantConfig != null && "SEPARATE_SCHEMA".equals(tenantConfig.getIsolationMode())) {
                String schemaName = tenantSchemas.get(tenantId);
                if (schemaName != null) {
                    switchSchema(connection, schemaName);
                }
            }
        }
        
        return connection;
    }
    
    /**
     * 切换数据库 Schema
     * 
     * @param connection 数据库连接
     * @param schemaName Schema 名称
     */
    private void switchSchema(Connection connection, String schemaName) {
        try {
            // 使用 USE 语句切换数据库（MySQL）
            connection.createStatement().execute("USE `" + schemaName + "`");
            log.debug("Switched to schema: {}", schemaName);
        } catch (SQLException e) {
            log.error("Failed to switch schema to: {}", schemaName, e);
            throw new TenantContextException("Failed to switch schema: " + schemaName, e);
        }
    }
    
    /**
     * 添加租户数据源
     * 用于动态添加新租户的数据源
     * 
     * @param tenantId 租户 ID
     * @param dataSource 数据源
     */
    public void addTenantDataSource(String tenantId, DataSource dataSource) {
        if (tenantId == null || dataSource == null) {
            throw new IllegalArgumentException("Tenant ID and data source cannot be null");
        }
        
        tenantDataSources.put(tenantId, dataSource);
        
        // 更新 AbstractRoutingDataSource 的目标数据源
        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
        targetDataSources.putAll(tenantDataSources);
        setTargetDataSources(targetDataSources);
        afterPropertiesSet();
        
        log.info("Added data source for tenant: {}", tenantId);
    }
    
    /**
     * 移除租户数据源
     * 
     * @param tenantId 租户 ID
     */
    public void removeTenantDataSource(String tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        DataSource removed = tenantDataSources.remove(tenantId);
        tenantSchemas.remove(tenantId);
        
        if (removed != null) {
            // 更新 AbstractRoutingDataSource 的目标数据源
            Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
            targetDataSources.putAll(tenantDataSources);
            setTargetDataSources(targetDataSources);
            afterPropertiesSet();
            
            log.info("Removed data source for tenant: {}", tenantId);
        } else {
            log.warn("No data source found for tenant: {}", tenantId);
        }
    }
    
    /**
     * 获取租户数据源
     * 
     * @param tenantId 租户 ID
     * @return 数据源，如果不存在则返回 null
     */
    public DataSource getTenantDataSource(String tenantId) {
        return tenantDataSources.get(tenantId);
    }
    
    /**
     * 检查租户数据源是否存在
     * 
     * @param tenantId 租户 ID
     * @return true 如果存在，否则返回 false
     */
    public boolean hasTenantDataSource(String tenantId) {
        return tenantDataSources.containsKey(tenantId);
    }
    
    /**
     * 清除所有租户数据源缓存
     */
    public void clearTenantDataSources() {
        tenantDataSources.clear();
        tenantSchemas.clear();
        log.info("Cleared all tenant data sources");
    }
}
