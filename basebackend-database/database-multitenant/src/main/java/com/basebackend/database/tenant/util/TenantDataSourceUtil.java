package com.basebackend.database.tenant.util;

import com.basebackend.database.tenant.context.TenantContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 租户数据源工具类
 * 提供便捷的租户上下文切换方法
 */
@Slf4j
public class TenantDataSourceUtil {
    
    /**
     * 在指定租户上下文中执行操作
     * 自动管理租户上下文的设置和清理
     * 
     * @param tenantId 租户 ID
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithTenant(String tenantId, Supplier<T> action) {
        String originalTenantId = TenantContext.getTenantId();
        
        try {
            // 设置租户上下文
            TenantContext.setTenantId(tenantId);
            log.debug("Switched to tenant: {}", tenantId);
            
            // 执行操作
            return action.get();
        } finally {
            // 恢复原租户上下文
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
                log.debug("Restored tenant context to: {}", originalTenantId);
            } else {
                TenantContext.clear();
                log.debug("Cleared tenant context");
            }
        }
    }
    
    /**
     * 在指定租户上下文中执行操作（无返回值）
     * 
     * @param tenantId 租户 ID
     * @param action 要执行的操作
     */
    public static void executeWithTenant(String tenantId, Runnable action) {
        executeWithTenant(tenantId, () -> {
            action.run();
            return null;
        });
    }
    
    /**
     * 在默认租户上下文中执行操作
     * 临时清除租户上下文，使用默认数据源
     * 
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithDefaultDataSource(Supplier<T> action) {
        String originalTenantId = TenantContext.getTenantId();
        
        try {
            // 清除租户上下文，使用默认数据源
            TenantContext.clear();
            log.debug("Cleared tenant context, using default data source");
            
            // 执行操作
            return action.get();
        } finally {
            // 恢复原租户上下文
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
                log.debug("Restored tenant context to: {}", originalTenantId);
            }
        }
    }
    
    /**
     * 在默认租户上下文中执行操作（无返回值）
     * 
     * @param action 要执行的操作
     */
    public static void executeWithDefaultDataSource(Runnable action) {
        executeWithDefaultDataSource(() -> {
            action.run();
            return null;
        });
    }
}
