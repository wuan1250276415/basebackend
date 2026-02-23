package com.basebackend.cache.refresh;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

/**
 * 缓存刷新任务描述
 * 记录需要刷新的缓存键、原始 TTL 等信息
 */
@Data
@AllArgsConstructor
public class RefreshTask {

    /**
     * 缓存键
     */
    private final String key;

    /**
     * 原始 TTL
     */
    private final Duration originalTtl;

    /**
     * 缓存名称（用于指标标签）
     */
    private final String cacheName;
}
