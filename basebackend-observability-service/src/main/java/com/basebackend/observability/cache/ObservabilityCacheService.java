package com.basebackend.observability.cache;

import com.basebackend.observability.config.ObservabilityProperties;
import com.basebackend.observability.constants.ObservabilityConstants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 可观测性缓存服务
 * <p>
 * 使用Caffeine本地缓存减少对外部服务的调用。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "observability.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityCacheService {

    private final ObservabilityProperties properties;

    private Cache<String, List<String>> servicesCache;
    private Cache<String, Map<String, Object>> tracesCache;
    private Cache<String, Object> metricsCache;

    @Autowired
    public ObservabilityCacheService(ObservabilityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        ObservabilityProperties.Cache cacheConfig = properties.getCache();

        // 服务列表缓存（短时间，经常变化）
        this.servicesCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheConfig.getServicesTtl(), TimeUnit.SECONDS)
                .maximumSize(100)
                .recordStats()
                .build();

        // 追踪数据缓存（较长时间，历史数据不变）
        this.tracesCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheConfig.getTracesTtl(), TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats()
                .build();

        // 指标数据缓存（最短时间，实时性要求高）
        this.metricsCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheConfig.getMetricsTtl(), TimeUnit.SECONDS)
                .maximumSize(500)
                .recordStats()
                .build();

        log.info("ObservabilityCacheService initialized: servicesTtl={}s, tracesTtl={}s, metricsTtl={}s",
                cacheConfig.getServicesTtl(),
                cacheConfig.getTracesTtl(),
                cacheConfig.getMetricsTtl());
    }

    /**
     * 获取或加载服务列表
     *
     * @param key    缓存键
     * @param loader 数据加载器
     * @return 服务列表
     */
    public List<String> getOrLoadServices(String key, Supplier<List<String>> loader) {
        List<String> cached = servicesCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Services cache hit: key={}", key);
            return cached;
        }

        List<String> data = loader.get();
        if (data != null && !data.isEmpty()) {
            servicesCache.put(key, data);
            log.debug("Services cached: key={}, size={}", key, data.size());
        }
        return data;
    }

    /**
     * 获取或加载追踪数据
     *
     * @param traceId 追踪ID
     * @param loader  数据加载器
     * @return 追踪数据
     */
    public Map<String, Object> getOrLoadTrace(String traceId, Supplier<Map<String, Object>> loader) {
        Map<String, Object> cached = tracesCache.getIfPresent(traceId);
        if (cached != null) {
            log.debug("Trace cache hit: traceId={}", traceId);
            return cached;
        }

        Map<String, Object> data = loader.get();
        if (data != null && !data.isEmpty()) {
            tracesCache.put(traceId, data);
            log.debug("Trace cached: traceId={}", traceId);
        }
        return data;
    }

    /**
     * 获取或加载指标数据
     *
     * @param key    缓存键
     * @param loader 数据加载器
     * @return 指标数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoadMetrics(String key, Supplier<T> loader) {
        Object cached = metricsCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Metrics cache hit: key={}", key);
            return (T) cached;
        }

        T data = loader.get();
        if (data != null) {
            metricsCache.put(key, data);
            log.debug("Metrics cached: key={}", key);
        }
        return data;
    }

    /**
     * 清除服务列表缓存
     */
    public void invalidateServicesCache() {
        servicesCache.invalidateAll();
        log.info("Services cache invalidated");
    }

    /**
     * 清除追踪缓存
     *
     * @param traceId 追踪ID
     */
    public void invalidateTraceCache(String traceId) {
        tracesCache.invalidate(traceId);
        log.debug("Trace cache invalidated: traceId={}", traceId);
    }

    /**
     * 清除指标缓存
     */
    public void invalidateMetricsCache() {
        metricsCache.invalidateAll();
        log.info("Metrics cache invalidated");
    }

    /**
     * 清除所有缓存
     */
    public void invalidateAll() {
        servicesCache.invalidateAll();
        tracesCache.invalidateAll();
        metricsCache.invalidateAll();
        log.info("All caches invalidated");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "services", Map.of(
                        "size", servicesCache.estimatedSize(),
                        "hitRate", servicesCache.stats().hitRate()),
                "traces", Map.of(
                        "size", tracesCache.estimatedSize(),
                        "hitRate", tracesCache.stats().hitRate()),
                "metrics", Map.of(
                        "size", metricsCache.estimatedSize(),
                        "hitRate", metricsCache.stats().hitRate()));
    }
}
