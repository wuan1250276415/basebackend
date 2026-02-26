package com.basebackend.database.tenant.cache;

import com.basebackend.database.tenant.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 租户级缓存管理器
 * <p>
 * 包装原始 CacheManager，自动为缓存 key 添加租户前缀，实现租户级缓存隔离。
 * <p>
 * 原始缓存名 "users" → 实际缓存名 "tenant:1001:users"
 */
@Slf4j
public class TenantCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public TenantCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            // 无租户上下文时直接使用原始缓存
            return delegate.getCache(name);
        }

        String tenantCacheName = "tenant:" + tenantId + ":" + name;
        return cacheMap.computeIfAbsent(tenantCacheName, k -> {
            log.debug("创建租户级缓存: {}", tenantCacheName);
            return delegate.getCache(tenantCacheName);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
