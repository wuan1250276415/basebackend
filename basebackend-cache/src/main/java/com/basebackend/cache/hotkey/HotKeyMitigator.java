package com.basebackend.cache.hotkey;

import com.basebackend.cache.config.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 热点 Key 缓解器
 * 当检测到热点 Key 时，将其提升到独立的本地 Caffeine 缓存中，
 * 减少对 Redis 的访问压力
 *
 * 此 Caffeine 实例独立于 MultiLevelCacheManager，专用于热点缓解
 */
@Slf4j
public class HotKeyMitigator {

    private final Cache<String, Object> mitigationCache;
    private final HotKeyDetector hotKeyDetector;
    private final CacheProperties cacheProperties;
    private final MeterRegistry meterRegistry;

    public HotKeyMitigator(
            HotKeyDetector hotKeyDetector,
            CacheProperties cacheProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.hotKeyDetector = hotKeyDetector;
        this.cacheProperties = cacheProperties;
        this.meterRegistry = meterRegistry;

        CacheProperties.HotKey config = cacheProperties.getHotKey();
        this.mitigationCache = Caffeine.newBuilder()
                .maximumSize(config.getLocalCacheMaxSize())
                .expireAfterWrite(computeTtlWithJitter(config))
                .build();

        log.info("HotKeyMitigator initialized: maxSize={}, baseTtl={}",
                config.getLocalCacheMaxSize(), config.getLocalCacheTtl());
    }

    /**
     * 尝试从缓解缓存中获取值
     * 仅当热点 Key 检测已启用且 key 已被缓解时才返回非 null
     *
     * @param key 缓存键
     * @return 缓存值，未命中返回 null
     */
    public Object get(String key) {
        return mitigationCache.getIfPresent(key);
    }

    /**
     * 在缓存命中后调用：如果 key 被检测为热点，将其提升到本地缓解缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void onCacheHit(String key, Object value) {
        if (hotKeyDetector.isHot(key)) {
            mitigationCache.put(key, value);
            recordMitigated(key);
            log.debug("Hot key mitigated to local cache: {}", key);
        }
    }

    /**
     * 主动失效缓解缓存中的条目
     */
    public void invalidate(String key) {
        mitigationCache.invalidate(key);
    }

    /**
     * 清空缓解缓存
     */
    public void invalidateAll() {
        mitigationCache.invalidateAll();
    }

    /**
     * 计算带随机抖动的 TTL
     */
    private Duration computeTtlWithJitter(CacheProperties.HotKey config) {
        Duration baseTtl = config.getLocalCacheTtl();
        int jitterPercent = config.getJitterPercent();
        if (jitterPercent <= 0) {
            return baseTtl;
        }
        long baseMs = baseTtl.toMillis();
        long jitterMs = (long) (baseMs * ThreadLocalRandom.current().nextDouble(0, jitterPercent / 100.0));
        return Duration.ofMillis(baseMs + jitterMs);
    }

    private void recordMitigated(String key) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.hotkey.mitigated")
                .tag("key", key)
                .register(meterRegistry)
                .increment();
    }
}
