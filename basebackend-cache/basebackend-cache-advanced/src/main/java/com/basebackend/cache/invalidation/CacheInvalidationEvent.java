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

    /**
     * 消息签名（HMAC-SHA256，Base64 编码）
     */
    private String signature;

    /**
     * 构建签名原文。
     * 注意：签名字段本身不参与签名，避免循环依赖。
     */
    public String buildSignaturePayload() {
        return String.join("|",
                safe(source),
                safe(cacheName),
                safe(keyPattern),
                type == null ? "" : type.name(),
                String.valueOf(timestamp),
                safe(correlationId));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
