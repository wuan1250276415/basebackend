package com.basebackend.cache.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存指标收集器
 * 负责收集和聚合缓存操作的指标数据
 */
@Slf4j
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // 每个缓存的统计信息
    private final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();
    
    public CacheMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * 记录缓存操作指标
     */
    public void recordMetrics(CacheMetrics metrics) {
        if (metrics == null) {
            return;
        }
        
        String cacheName = metrics.getCacheName();
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats(cacheName));
        
        // 更新统计信息
        stats.lastAccessTime = Instant.now();
        
        // 记录到 Micrometer
        recordToMicrometer(metrics);
        
        // 根据操作类型更新统计
        switch (metrics.getOperationType()) {
            case GET:
            case MULTI_GET:
                if (metrics.isSuccess()) {
                    if (Boolean.TRUE.equals(metrics.getHit())) {
                        stats.hitCount.incrementAndGet();
                        incrementCounter(cacheName, "hit");
                    } else {
                        stats.missCount.incrementAndGet();
                        incrementCounter(cacheName, "miss");
                    }
                }
                break;
            case SET:
            case MULTI_SET:
                if (metrics.isSuccess()) {
                    incrementCounter(cacheName, "set");
                }
                break;
            case EVICT:
            case DELETE_BY_PATTERN:
                if (metrics.isSuccess()) {
                    stats.evictionCount.incrementAndGet();
                    incrementCounter(cacheName, "eviction");
                }
                break;
            case CLEAR:
                if (metrics.isSuccess()) {
                    incrementCounter(cacheName, "clear");
                }
                break;
        }
        
        // 记录延迟
        if (metrics.getLatencyMs() > 0) {
            stats.totalLatency.addAndGet(metrics.getLatencyMs());
            stats.operationCount.incrementAndGet();
        }
        
        // 记录失败
        if (!metrics.isSuccess()) {
            stats.errorCount.incrementAndGet();
            incrementCounter(cacheName, "error");
            log.warn("Cache operation failed: cache={}, operation={}, error={}", 
                    cacheName, metrics.getOperationType(), metrics.getErrorMessage());
        }
    }
    
    /**
     * 记录指标到 Micrometer
     */
    private void recordToMicrometer(CacheMetrics metrics) {
        String cacheName = metrics.getCacheName();
        String operation = metrics.getOperationType().name().toLowerCase();
        
        // 记录操作延迟
        Timer.builder("cache.operation.latency")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .tag("success", String.valueOf(metrics.isSuccess()))
                .register(meterRegistry)
                .record(metrics.getLatencyMs(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 增加计数器
     */
    private void incrementCounter(String cacheName, String type) {
        Counter.builder("cache.operations")
                .tag("cache", cacheName)
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics(String cacheName) {
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats == null) {
            return CacheStatistics.builder()
                    .cacheName(cacheName)
                    .hitCount(0)
                    .missCount(0)
                    .totalCount(0)
                    .hitRate(0.0)
                    .evictionCount(0)
                    .size(0)
                    .averageLoadTime(0)
                    .lastAccessTime(null)
                    .build();
        }
        
        long hitCount = stats.hitCount.get();
        long missCount = stats.missCount.get();
        long totalCount = hitCount + missCount;
        double hitRate = totalCount > 0 ? (double) hitCount / totalCount : 0.0;
        
        long operationCount = stats.operationCount.get();
        long averageLoadTime = operationCount > 0 ? stats.totalLatency.get() / operationCount : 0;
        
        return CacheStatistics.builder()
                .cacheName(cacheName)
                .hitCount(hitCount)
                .missCount(missCount)
                .totalCount(totalCount)
                .hitRate(hitRate)
                .evictionCount(stats.evictionCount.get())
                .size(0) // Size will be updated by cache manager
                .averageLoadTime(averageLoadTime)
                .lastAccessTime(stats.lastAccessTime)
                .build();
    }
    
    /**
     * 重置缓存统计信息
     */
    public void resetStatistics(String cacheName) {
        cacheStatsMap.remove(cacheName);
    }
    
    /**
     * 获取所有缓存名称
     */
    public java.util.Set<String> getAllCacheNames() {
        return cacheStatsMap.keySet();
    }
    
    /**
     * 内部统计类
     */
    private static class CacheStats {
        final String cacheName;
        final AtomicLong hitCount = new AtomicLong(0);
        final AtomicLong missCount = new AtomicLong(0);
        final AtomicLong evictionCount = new AtomicLong(0);
        final AtomicLong errorCount = new AtomicLong(0);
        final AtomicLong totalLatency = new AtomicLong(0);
        final AtomicLong operationCount = new AtomicLong(0);
        volatile Instant lastAccessTime;
        
        CacheStats(String cacheName) {
            this.cacheName = cacheName;
            this.lastAccessTime = Instant.now();
        }
    }
}
