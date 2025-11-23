package com.basebackend.database.tenant.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文
 * 使用 ThreadLocal 存储当前线程的租户 ID
 * 
 * 使用示例:
 * <pre>
 * // 设置租户ID
 * TenantContext.setTenantId("tenant-001");
 * 
 * // 获取租户ID
 * String tenantId = TenantContext.getTenantId();
 * 
 * // 清除租户ID（重要：避免内存泄漏）
 * TenantContext.clear();
 * </pre>
 */
@Slf4j
public class TenantContext {
    
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    
    /**
     * 设置当前线程的租户 ID
     * 
     * @param tenantId 租户 ID
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Attempting to set null or empty tenant ID");
            return;
        }
        TENANT_ID.set(tenantId);
        log.debug("Tenant context set: {}", tenantId);
    }
    
    /**
     * 获取当前线程的租户 ID
     * 
     * @return 租户 ID，如果未设置则返回 null
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }
    
    /**
     * 检查当前线程是否设置了租户 ID
     * 
     * @return true 如果已设置租户 ID，否则返回 false
     */
    public static boolean hasTenantId() {
        return TENANT_ID.get() != null;
    }
    
    /**
     * 清除当前线程的租户 ID
     * 重要：在请求结束时必须调用此方法，避免内存泄漏
     */
    public static void clear() {
        String tenantId = TENANT_ID.get();
        if (tenantId != null) {
            log.debug("Tenant context cleared: {}", tenantId);
        }
        TENANT_ID.remove();
    }
}
