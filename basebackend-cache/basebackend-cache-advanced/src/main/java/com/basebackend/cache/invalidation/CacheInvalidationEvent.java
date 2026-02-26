package com.basebackend.cache.invalidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨服务缓存失效事件
 * 通过 Redis Pub/Sub 在服务间传播缓存失效通知
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationEvent {

    public enum Type {
        EVICT,      // 失效单个 key
        CLEAR,      // 清空指定缓存
        CLEAR_ALL   // 清空所有缓存
    }

    /**
     * 发布事件的服务名称
     */
    private String source;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 失效的键或模式
     */
    private String keyPattern;

    /**
     * 失效类型
     */
    private Type type;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 关联 ID（用于追踪）
     */
    private String correlationId;
}
