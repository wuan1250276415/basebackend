package com.basebackend.database.tenant.service;

import javax.sql.DataSource;

/**
 * 租户数据源服务接口
 * 提供租户数据源的注册、切换和管理功能
 */
public interface TenantDataSourceService {
    
    /**
     * 注册租户数据源
     * 用于 SEPARATE_DB 模式，为租户注册独立的数据源
     * 
     * @param tenantId 租户 ID
     * @param dataSource 数据源
     */
    void registerTenantDataSource(String tenantId, DataSource dataSource);
    
    /**
     * 注销租户数据源
     * 
     * @param tenantId 租户 ID
     */
    void unregisterTenantDataSource(String tenantId);
    
    /**
     * 获取租户数据源
     * 
     * @param tenantId 租户 ID
     * @return 数据源，如果不存在则返回 null
     */
    DataSource getTenantDataSource(String tenantId);
    
    /**
     * 检查租户数据源是否已注册
     * 
     * @param tenantId 租户 ID
     * @return true 如果已注册，否则返回 false
     */
    boolean isDataSourceRegistered(String tenantId);
    
    /**
     * 初始化所有激活租户的数据源
     * 在应用启动时调用，根据租户配置初始化数据源
     */
    void initializeTenantDataSources();
    
    /**
     * 刷新租户数据源
     * 重新加载租户配置并更新数据源
     * 
     * @param tenantId 租户 ID
     */
    void refreshTenantDataSource(String tenantId);
    
    /**
     * 验证租户数据源连接
     * 
     * @param tenantId 租户 ID
     * @return true 如果连接正常，否则返回 false
     */
    boolean validateTenantDataSource(String tenantId);
}
