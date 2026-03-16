package com.basebackend.cache.hook;

import com.basebackend.cache.hotkey.HotKeyDetector;
import com.basebackend.cache.hotkey.HotKeyMitigator;
import com.basebackend.cache.invalidation.CacheInvalidationPublisher;
import com.basebackend.cache.refresh.NearExpiryRefreshManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.util.StringUtils;

@Slf4j
public class AdvancedCacheOperationHook implements CacheOperationHook {

    private final HotKeyDetector hotKeyDetector;
    private final HotKeyMitigator hotKeyMitigator;
    private final NearExpiryRefreshManager nearExpiryRefreshManager;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public AdvancedCacheOperationHook(
            HotKeyDetector hotKeyDetector,
            HotKeyMitigator hotKeyMitigator,
            NearExpiryRefreshManager nearExpiryRefreshManager,
            CacheInvalidationPublisher cacheInvalidationPublisher) {
        this.hotKeyDetector = hotKeyDetector;
        this.hotKeyMitigator = hotKeyMitigator;
        this.nearExpiryRefreshManager = nearExpiryRefreshManager;
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
    }

    @Override
    public Object beforeCacheLookup(String cacheName, String key, long ttlSeconds, ProceedingJoinPoint joinPoint) {
        if (hotKeyDetector != null) {
            hotKeyDetector.recordAccess(key);
        }
        if (hotKeyMitigator == null) {
            return null;
        }
        Object value = hotKeyMitigator.get(key);
        if (value != null) {
            log.debug("Hot key mitigation cache hit: key={}", key);
        }
        return value;
    }

    @Override
    public void afterCacheHit(String cacheName, String key, Object value, long ttlSeconds, ProceedingJoinPoint joinPoint) {
        if (hotKeyMitigator != null) {
            hotKeyMitigator.onCacheHit(key, value);
        }
        if (nearExpiryRefreshManager != null && ttlSeconds > 0) {
            nearExpiryRefreshManager.checkAndRefresh(key, ttlSeconds, resolveCacheName(cacheName, key), joinPoint);
        }
    }

    @Override
    public void afterCachePut(String cacheName, String key, Object value) {
        if (hotKeyMitigator != null) {
            hotKeyMitigator.invalidate(key);
        }
        if (cacheInvalidationPublisher != null) {
            cacheInvalidationPublisher.publishEvict(resolveCacheName(cacheName, key), key);
        }
    }

    @Override
    public void afterCacheEvict(String cacheName, String key) {
        if (hotKeyMitigator != null) {
            hotKeyMitigator.invalidate(key);
        }
        if (cacheInvalidationPublisher != null) {
            cacheInvalidationPublisher.publishEvict(resolveCacheName(cacheName, key), key);
        }
    }

    @Override
    public void afterCacheClear(String cacheName) {
        if (hotKeyMitigator != null) {
            hotKeyMitigator.invalidateAll();
        }
        if (cacheInvalidationPublisher != null) {
            if (StringUtils.hasText(cacheName)) {
                cacheInvalidationPublisher.publishClear(cacheName);
            } else {
                cacheInvalidationPublisher.publishClearAll();
            }
        }
    }

    private String resolveCacheName(String cacheName, String key) {
        if (StringUtils.hasText(cacheName)) {
            return cacheName;
        }
        if (!StringUtils.hasText(key)) {
            return "*";
        }
        int separatorIndex = key.indexOf(':');
        return separatorIndex > 0 ? key.substring(0, separatorIndex) : key;
    }
}
