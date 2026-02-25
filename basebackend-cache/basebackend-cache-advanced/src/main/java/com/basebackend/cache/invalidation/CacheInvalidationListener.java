package com.basebackend.cache.invalidation;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.service.CacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * 跨服务缓存失效监听器
 * 订阅 Redis Pub/Sub 通道，接收其他服务发布的失效事件并应用到本地
 *
 * 行为：
 * 1. 反序列化 CacheInvalidationEvent
 * 2. 跳过自身发布的事件（防止自失效循环）
 * 3. 执行对应的缓存失效操作
 * 4. 如果启用了多级缓存，同时失效本地 Caffeine
 */
@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private final CacheService cacheService;
    private final CacheProperties cacheProperties;
    private final MultiLevelCacheManager multiLevelCacheManager;
    private final MeterRegistry meterRegistry;

    public CacheInvalidationListener(
            CacheService cacheService,
            CacheProperties cacheProperties,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
        this.multiLevelCacheManager = multiLevelCacheManager;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body;
        try {
            body = new String(message.getBody());
        } catch (Exception e) {
            log.warn("Failed to read invalidation message body", e);
            return;
        }

        CacheInvalidationEvent event;
        try {
            event = JsonUtils.parseObject(body, CacheInvalidationEvent.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize invalidation event: {}", body, e);
            return;
        }

        if (event == null || event.getType() == null) {
            log.warn("Received invalid invalidation event: {}", body);
            return;
        }

        // 跳过自身发布的事件
        String selfName = cacheProperties.getInvalidation().getServiceName();
        if (selfName.equals(event.getSource())) {
            recordSkippedSelf();
            log.debug("Skipping self-origin invalidation event: correlationId={}", event.getCorrelationId());
            return;
        }

        log.info("Received invalidation event: source={}, type={}, cacheName={}, key={}",
                event.getSource(), event.getType(), event.getCacheName(), event.getKeyPattern());

        applyInvalidation(event);
        recordReceived(event);
    }

    private void applyInvalidation(CacheInvalidationEvent event) {
        try {
            switch (event.getType()) {
                case EVICT -> {
                    cacheService.delete(event.getKeyPattern());
                    evictFromLocal(event.getKeyPattern());
                }
                case CLEAR -> {
                    cacheService.clearCache(event.getCacheName());
                    evictAllFromLocal();
                }
                case CLEAR_ALL -> {
                    cacheService.clearAllCaches();
                    evictAllFromLocal();
                }
            }
        } catch (Exception e) {
            log.error("Failed to apply invalidation event: type={}, cacheName={}, key={}",
                    event.getType(), event.getCacheName(), event.getKeyPattern(), e);
        }
    }

    private void evictFromLocal(String key) {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.evict(key);
        }
    }

    private void evictAllFromLocal() {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.clear();
        }
    }

    private void recordReceived(CacheInvalidationEvent event) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.invalidation.received")
                .tag("cacheName", event.getCacheName())
                .tag("type", event.getType().name())
                .tag("source", event.getSource())
                .register(meterRegistry)
                .increment();
    }

    private void recordSkippedSelf() {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.invalidation.skipped.self")
                .register(meterRegistry)
                .increment();
    }
}
