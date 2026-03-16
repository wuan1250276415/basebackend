package com.basebackend.cache.hook;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Hook 安全调用工具类
 * 封装 {@link CacheOperationHook} 的异常隔离调用，防止 Hook 异常传播到业务方法。
 * 所有调用均以 try-catch 包裹，失败时仅记录 WARN 日志。
 */
@Slf4j
public final class CacheHookInvoker {

    private CacheHookInvoker() {
        // utility class
    }

    /**
     * 安全调用 {@link CacheOperationHook#beforeCacheLookup}
     *
     * @return Hook 返回的预加载值，或 null（Hook 异常时）
     */
    public static Object safeBeforeCacheLookup(CacheOperationHook hook, String cacheName,
                                                String key, long ttlSeconds, ProceedingJoinPoint joinPoint) {
        try {
            return hook.beforeCacheLookup(cacheName, key, ttlSeconds, joinPoint);
        } catch (Exception e) {
            log.warn("CacheOperationHook beforeCacheLookup failed for key={}", key, e);
            return null;
        }
    }

    /**
     * 安全调用 {@link CacheOperationHook#afterCacheHit}
     */
    public static void safeAfterCacheHit(CacheOperationHook hook, String cacheName,
                                          String key, Object value, long ttlSeconds, ProceedingJoinPoint joinPoint) {
        try {
            hook.afterCacheHit(cacheName, key, value, ttlSeconds, joinPoint);
        } catch (Exception e) {
            log.warn("CacheOperationHook afterCacheHit failed for key={}", key, e);
        }
    }

    /**
     * 安全调用 {@link CacheOperationHook#afterCachePut}
     */
    public static void safeAfterCachePut(CacheOperationHook hook, String cacheName, String key, Object value) {
        try {
            hook.afterCachePut(cacheName, key, value);
        } catch (Exception e) {
            log.warn("CacheOperationHook afterCachePut failed for key={}", key, e);
        }
    }

    /**
     * 安全调用 {@link CacheOperationHook#afterCacheEvict}
     */
    public static void safeAfterCacheEvict(CacheOperationHook hook, String cacheName, String key) {
        try {
            hook.afterCacheEvict(cacheName, key);
        } catch (Exception e) {
            log.warn("CacheOperationHook afterCacheEvict failed for key={}", key, e);
        }
    }

    /**
     * 安全调用 {@link CacheOperationHook#afterCacheClear}
     */
    public static void safeAfterCacheClear(CacheOperationHook hook, String cacheName) {
        try {
            hook.afterCacheClear(cacheName);
        } catch (Exception e) {
            log.warn("CacheOperationHook afterCacheClear failed for cache={}", cacheName, e);
        }
    }
}
