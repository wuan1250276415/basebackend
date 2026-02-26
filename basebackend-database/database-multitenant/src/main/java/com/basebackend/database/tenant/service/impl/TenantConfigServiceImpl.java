package com.basebackend.database.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.database.tenant.entity.TenantConfig;
import com.basebackend.database.tenant.mapper.TenantConfigMapper;
import com.basebackend.database.tenant.service.TenantConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 租户配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigServiceImpl implements TenantConfigService {
    
    private final TenantConfigMapper tenantConfigMapper;
    
    @Override
    @Cacheable(value = "tenant:config", key = "#tenantId", unless = "#result == null")
    public TenantConfig getByTenantId(String tenantId) {
        log.debug("Getting tenant config for tenant: {}", tenantId);
        LambdaQueryWrapper<TenantConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantConfig::getTenantId, tenantId);
        return tenantConfigMapper.selectOne(wrapper);
    }
    
    @Override
    @Cacheable(value = "tenant:active", unless = "#result == null || #result.isEmpty()")
    public List<TenantConfig> listActiveTenants() {
        log.debug("Listing all active tenants");
        LambdaQueryWrapper<TenantConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantConfig::getStatus, "ACTIVE");
        return tenantConfigMapper.selectList(wrapper);
    }
    
    @Override
    @CacheEvict(value = {"tenant:config", "tenant:active"}, allEntries = true)
    public boolean save(TenantConfig tenantConfig) {
        log.info("Saving tenant config: {}", tenantConfig.getTenantId());
        
        // 检查租户 ID 是否已存在
        TenantConfig existing = getByTenantId(tenantConfig.getTenantId());
        if (existing != null) {
            log.warn("Tenant ID already exists: {}", tenantConfig.getTenantId());
            return false;
        }
        
        // 设置默认状态
        if (tenantConfig.getStatus() == null) {
            tenantConfig.setStatus("ACTIVE");
        }
        
        int result = tenantConfigMapper.insert(tenantConfig);
        return result > 0;
    }
    
    @Override
    @CacheEvict(value = {"tenant:config", "tenant:active"}, allEntries = true)
    public boolean update(TenantConfig tenantConfig) {
        log.info("Updating tenant config: {}", tenantConfig.getTenantId());
        
        LambdaUpdateWrapper<TenantConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TenantConfig::getTenantId, tenantConfig.getTenantId());
        
        int result = tenantConfigMapper.update(tenantConfig, wrapper);
        return result > 0;
    }
    
    @Override
    @CacheEvict(value = {"tenant:config", "tenant:active"}, allEntries = true)
    public boolean delete(String tenantId) {
        log.info("Deleting tenant config: {}", tenantId);
        
        LambdaQueryWrapper<TenantConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantConfig::getTenantId, tenantId);
        
        int result = tenantConfigMapper.delete(wrapper);
        return result > 0;
    }
    
    @Override
    @CacheEvict(value = {"tenant:config", "tenant:active"}, allEntries = true)
    public boolean activate(String tenantId) {
        log.info("Activating tenant: {}", tenantId);
        return updateStatus(tenantId, "ACTIVE");
    }
    
    @Override
    @CacheEvict(value = {"tenant:config", "tenant:active"}, allEntries = true)
    public boolean deactivate(String tenantId) {
        log.info("Deactivating tenant: {}", tenantId);
        return updateStatus(tenantId, "INACTIVE");
    }
    
    /**
     * 更新租户状态
     */
    private boolean updateStatus(String tenantId, String status) {
        LambdaUpdateWrapper<TenantConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TenantConfig::getTenantId, tenantId)
               .set(TenantConfig::getStatus, status);
        
        int result = tenantConfigMapper.update(null, wrapper);
        return result > 0;
    }
}
