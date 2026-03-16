package com.basebackend.cache.hook;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 缓存主链路扩展点
 * 用于在不修改核心流程语义的前提下接入高级能力
 */
public interface CacheOperationHook {

    CacheOperationHook NO_OP = new CacheOperationHook() {
    };

    /**
     * 在访问主缓存前执行，可返回提前命中的值
     */
    default Object beforeCacheLookup(String cacheName, String key, long ttlSeconds, ProceedingJoinPoint joinPoint) {
        return null;
    }

    /**
     * 主缓存命中后回调
     */
    default void afterCacheHit(String cacheName, String key, Object value, long ttlSeconds, ProceedingJoinPoint joinPoint) {
    }

    /**
     * 缓存写入后回调
     */
    default void afterCachePut(String cacheName, String key, Object value) {
    }

    /**
     * 缓存键淘汰后回调
     */
    default void afterCacheEvict(String cacheName, String key) {
    }

    /**
     * 缓存级清空后回调
     */
    default void afterCacheClear(String cacheName) {
    }
}
