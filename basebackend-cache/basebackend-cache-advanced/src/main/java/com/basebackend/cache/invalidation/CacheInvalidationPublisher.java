package com.basebackend.cache.invalidation;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.cache.config.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

/**
 * 缓存失效事件发布器
 * 将失效事件序列化为 JSON 并通过 Redis Pub/Sub 发布到指定通道
 */
@Slf4j
public class CacheInvalidationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;
    private final MeterRegistry meterRegistry;

    public CacheInvalidationPublisher(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 发布单个 key 失效事件
     */
    public void publishEvict(String cacheName, String key) {
        publish(CacheInvalidationEvent.builder()
                .type(CacheInvalidationEvent.Type.EVICT)
                .cacheName(cacheName)
                .keyPattern(key)
                .build());
    }

    /**
     * 发布清空指定缓存事件
     */
    public void publishClear(String cacheName) {
        publish(CacheInvalidationEvent.builder()
                .type(CacheInvalidationEvent.Type.CLEAR)
                .cacheName(cacheName)
                .keyPattern(cacheName + ":*")
                .build());
    }

    /**
     * 发布清空所有缓存事件
     */
    public void publishClearAll() {
        publish(CacheInvalidationEvent.builder()
                .type(CacheInvalidationEvent.Type.CLEAR_ALL)
                .cacheName("*")
                .keyPattern("*")
                .build());
    }

    private void publish(CacheInvalidationEvent event) {
        CacheProperties.Invalidation config = cacheProperties.getInvalidation();

        event.setSource(config.getServiceName());
        event.setTimestamp(System.currentTimeMillis());
        event.setCorrelationId(UUID.randomUUID().toString());

        String channel = config.getChannel();
        String json = JsonUtils.toJsonString(event);

        try {
            redisTemplate.convertAndSend(channel, json);
            log.debug("Published invalidation event: channel={}, type={}, cacheName={}, key={}",
                    channel, event.getType(), event.getCacheName(), event.getKeyPattern());
            recordPublished(event);
        } catch (Exception e) {
            log.error("Failed to publish invalidation event: {}", event, e);
        }
    }

    private void recordPublished(CacheInvalidationEvent event) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.invalidation.published")
                .tag("cacheName", event.getCacheName())
                .tag("type", event.getType().name())
                .register(meterRegistry)
                .increment();
    }
}
