package com.basebackend.database.tenant.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

/**
 * 租户字段自动填充处理器
 * 在插入数据时自动填充租户 ID
 */
@Slf4j
@RequiredArgsConstructor
public class TenantMetaObjectHandler implements MetaObjectHandler {
    
    private final DatabaseEnhancedProperties properties;
    
    @Override
    public void insertFill(MetaObject metaObject) {
        // 如果多租户未启用，直接返回
        if (!properties.getMultiTenancy().isEnabled()) {
            return;
        }
        
        // 获取租户字段名
        String tenantColumn = properties.getMultiTenancy().getTenantColumn();
        
        // 检查对象是否有租户字段
        if (!metaObject.hasSetter(tenantColumn)) {
            log.debug("Entity does not have tenant field: {}", tenantColumn);
            return;
        }
        
        // 获取当前租户 ID
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant context is not set for insert operation");
            throw new TenantContextException("Tenant context is not set. Please set tenant ID before database operations.");
        }
        
        // 检查是否已经设置了租户 ID
        Object existingValue = metaObject.getValue(tenantColumn);
        if (existingValue != null) {
            log.debug("Tenant ID already set in entity: {}", existingValue);
            // 验证设置的租户 ID 是否与当前上下文一致
            if (!tenantId.equals(existingValue.toString())) {
                log.warn("Entity tenant ID {} does not match context tenant ID {}", existingValue, tenantId);
                throw new TenantContextException(
                    String.format("Entity tenant ID (%s) does not match context tenant ID (%s)", 
                                existingValue, tenantId));
            }
            return;
        }
        
        // 自动填充租户 ID
        this.strictInsertFill(metaObject, tenantColumn, String.class, tenantId);
        log.debug("Auto-filled tenant ID: {} for entity", tenantId);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新操作不需要填充租户 ID
        // 租户 ID 应该是不可变的
    }
}
