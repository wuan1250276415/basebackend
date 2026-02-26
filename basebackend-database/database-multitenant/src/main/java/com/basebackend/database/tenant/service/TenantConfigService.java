package com.basebackend.database.tenant.service;

import com.basebackend.database.tenant.entity.TenantConfig;

import java.util.List;

/**
 * 租户配置服务接口
 */
public interface TenantConfigService {
    
    /**
     * 根据租户 ID 获取租户配置
     * 
     * @param tenantId 租户 ID
     * @return 租户配置，如果不存在则返回 null
     */
    TenantConfig getByTenantId(String tenantId);
    
    /**
     * 获取所有激活的租户配置
     * 
     * @return 激活的租户配置列表
     */
    List<TenantConfig> listActiveTenants();
    
    /**
     * 保存租户配置
     * 
     * @param tenantConfig 租户配置
     * @return 是否保存成功
     */
    boolean save(TenantConfig tenantConfig);
    
    /**
     * 更新租户配置
     * 
     * @param tenantConfig 租户配置
     * @return 是否更新成功
     */
    boolean update(TenantConfig tenantConfig);
    
    /**
     * 删除租户配置
     * 
     * @param tenantId 租户 ID
     * @return 是否删除成功
     */
    boolean delete(String tenantId);
    
    /**
     * 激活租户
     * 
     * @param tenantId 租户 ID
     * @return 是否激活成功
     */
    boolean activate(String tenantId);
    
    /**
     * 停用租户
     * 
     * @param tenantId 租户 ID
     * @return 是否停用成功
     */
    boolean deactivate(String tenantId);
}
