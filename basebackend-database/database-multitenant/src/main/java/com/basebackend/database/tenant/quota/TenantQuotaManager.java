package com.basebackend.database.tenant.quota;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 租户配额管理器
 * <p>
 * 管理每个租户的资源配额（用户数、存储量、API 调用量等）。
 * 内存实现，生产环境可替换为 Redis 实现。
 */
public class TenantQuotaManager {

    private final Map<String, TenantQuota> quotaMap = new ConcurrentHashMap<>();

    /**
     * 设置租户配额
     *
     * @param tenantId  租户 ID
     * @param resource  资源类型（如 "users"、"storage"、"api_calls"）
     * @param maxAmount 最大限额
     */
    public void setQuota(String tenantId, String resource, long maxAmount) {
        quotaMap.computeIfAbsent(tenantId, k -> new TenantQuota())
                .setLimit(resource, maxAmount);
    }

    /**
     * 检查是否超出配额
     *
     * @param tenantId 租户 ID
     * @param resource 资源类型
     * @param amount   本次请求数量
     * @return true = 超出配额
     */
    public boolean isExceeded(String tenantId, String resource, long amount) {
        TenantQuota quota = quotaMap.get(tenantId);
        if (quota == null) {
            return false; // 未设置配额表示不限制
        }
        return quota.isExceeded(resource, amount);
    }

    /**
     * 增加使用量
     *
     * @param tenantId 租户 ID
     * @param resource 资源类型
     * @param amount   增加量
     * @return 增加后的总使用量
     */
    public long increment(String tenantId, String resource, long amount) {
        return quotaMap.computeIfAbsent(tenantId, k -> new TenantQuota())
                .increment(resource, amount);
    }

    /**
     * 获取当前使用量
     */
    public long getUsage(String tenantId, String resource) {
        TenantQuota quota = quotaMap.get(tenantId);
        return quota != null ? quota.getUsage(resource) : 0;
    }

    /**
     * 获取配额限制
     */
    public long getLimit(String tenantId, String resource) {
        TenantQuota quota = quotaMap.get(tenantId);
        return quota != null ? quota.getLimit(resource) : -1; // -1 表示无限制
    }

    /**
     * 重置使用量（通常由定时任务调用，如每日/每月重置 API 调用量）
     */
    public void resetUsage(String tenantId, String resource) {
        TenantQuota quota = quotaMap.get(tenantId);
        if (quota != null) {
            quota.resetUsage(resource);
        }
    }

    /**
     * 重置租户的所有使用量
     */
    public void resetAllUsage(String tenantId) {
        TenantQuota quota = quotaMap.get(tenantId);
        if (quota != null) {
            quota.resetAllUsage();
        }
    }

    // --- 内部数据结构 ---

    private static class TenantQuota {
        private final Map<String, Long> limits = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> usages = new ConcurrentHashMap<>();

        void setLimit(String resource, long maxAmount) {
            limits.put(resource, maxAmount);
        }

        boolean isExceeded(String resource, long amount) {
            Long limit = limits.get(resource);
            if (limit == null || limit < 0) {
                return false; // 未设置限制
            }
            long currentUsage = getUsage(resource);
            return currentUsage + amount > limit;
        }

        long increment(String resource, long amount) {
            return usages.computeIfAbsent(resource, k -> new AtomicLong(0))
                    .addAndGet(amount);
        }

        long getUsage(String resource) {
            AtomicLong usage = usages.get(resource);
            return usage != null ? usage.get() : 0;
        }

        long getLimit(String resource) {
            Long limit = limits.get(resource);
            return limit != null ? limit : -1;
        }

        void resetUsage(String resource) {
            AtomicLong usage = usages.get(resource);
            if (usage != null) {
                usage.set(0);
            }
        }

        void resetAllUsage() {
            usages.values().forEach(u -> u.set(0));
        }
    }
}
