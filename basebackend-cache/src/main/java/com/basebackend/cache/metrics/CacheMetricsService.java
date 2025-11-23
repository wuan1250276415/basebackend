package com.basebackend.cache.metrics;

import com.basebackend.cache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/**
 * 缓存指标服务
 * 提供缓存指标的记录和查询功能
 */
@Slf4j
@Service
public class CacheMetricsService {
    
    private final CacheMetricsCollector metricsCollector;
    private final CacheProperties cacheProperties;
    
    public CacheMetricsService(CacheMetricsCollector metricsCollector, 
                               CacheProperties cacheProperties) {
        this.metricsCollector = metricsCollector;
        this.cacheProperties = cacheProperties;
    }
    
    /**
     * 记录缓存命中
     */
    public void recordHit(String cacheName) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        CacheMetrics metrics = CacheMetrics.builder()
                .cacheName(cacheName)
                .operationType(CacheMetrics.OperationType.GET)
                .success(true)
                .hit(true)
                .latencyMs(0)
                .timestamp(Instant.now())
                .build();
        
        metricsCollector.recordMetrics(metrics);
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordMiss(String cacheName) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        CacheMetrics metrics = CacheMetrics.builder()
                .cacheName(cacheName)
                .operationType(CacheMetrics.OperationType.GET)
                .success(true)
                .hit(false)
                .latencyMs(0)
                .timestamp(Instant.now())
                .build();
        
        metricsCollector.recordMetrics(metrics);
        
        // 检查命中率是否低于阈值
        checkHitRate(cacheName);
    }
    
    /**
     * 记录操作延迟
     */
    public void recordLatency(String operation, long milliseconds) {
        recordLatency("default", operation, milliseconds, true, null);
    }
    
    /**
     * 记录操作延迟（带缓存名称）
     */
    public void recordLatency(String cacheName, String operation, long milliseconds) {
        recordLatency(cacheName, operation, milliseconds, true, null);
    }
    
    /**
     * 记录操作延迟（完整版本）
     */
    public void recordLatency(String cacheName, String operation, long milliseconds, 
                             boolean success, String errorMessage) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        CacheMetrics.OperationType operationType;
        try {
            operationType = CacheMetrics.OperationType.valueOf(operation.toUpperCase());
        } catch (IllegalArgumentException e) {
            operationType = CacheMetrics.OperationType.GET;
        }
        
        CacheMetrics metrics = CacheMetrics.builder()
                .cacheName(cacheName)
                .operationType(operationType)
                .success(success)
                .latencyMs(milliseconds)
                .timestamp(Instant.now())
                .errorMessage(errorMessage)
                .build();
        
        metricsCollector.recordMetrics(metrics);
    }
    
    /**
     * 记录缓存设置操作
     */
    public void recordSet(String cacheName, long latencyMs, boolean success) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        CacheMetrics metrics = CacheMetrics.builder()
                .cacheName(cacheName)
                .operationType(CacheMetrics.OperationType.SET)
                .success(success)
                .latencyMs(latencyMs)
                .timestamp(Instant.now())
                .build();
        
        metricsCollector.recordMetrics(metrics);
    }
    
    /**
     * 记录缓存淘汰操作
     */
    public void recordEviction(String cacheName, long latencyMs, boolean success) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        CacheMetrics metrics = CacheMetrics.builder()
                .cacheName(cacheName)
                .operationType(CacheMetrics.OperationType.EVICT)
                .success(success)
                .latencyMs(latencyMs)
                .timestamp(Instant.now())
                .build();
        
        metricsCollector.recordMetrics(metrics);
    }
    
    /**
     * 记录操作失败
     */
    public void recordError(String cacheName, String operation, String errorMessage) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        CacheMetrics.OperationType operationType;
        try {
            operationType = CacheMetrics.OperationType.valueOf(operation.toUpperCase());
        } catch (IllegalArgumentException e) {
            operationType = CacheMetrics.OperationType.GET;
        }
        
        CacheMetrics metrics = CacheMetrics.builder()
                .cacheName(cacheName)
                .operationType(operationType)
                .success(false)
                .latencyMs(0)
                .timestamp(Instant.now())
                .errorMessage(errorMessage)
                .build();
        
        metricsCollector.recordMetrics(metrics);
        
        log.error("Cache operation error: cache={}, operation={}, error={}", 
                cacheName, operation, errorMessage);
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics(String cacheName) {
        return metricsCollector.getStatistics(cacheName);
    }
    
    /**
     * 重置缓存统计信息
     */
    public void resetStatistics(String cacheName) {
        metricsCollector.resetStatistics(cacheName);
    }
    
    /**
     * 获取所有缓存名称
     */
    public Set<String> getAllCacheNames() {
        return metricsCollector.getAllCacheNames();
    }
    
    /**
     * 检查命中率是否低于阈值
     */
    private void checkHitRate(String cacheName) {
        CacheStatistics stats = metricsCollector.getStatistics(cacheName);
        
        if (stats.getTotalCount() < 100) {
            // 样本数太少，不检查
            return;
        }
        
        double threshold = cacheProperties.getMetrics().getLowHitRateThreshold();
        if (stats.getHitRate() < threshold) {
            log.warn("Cache hit rate is low: cache={}, hitRate={}, threshold={}", 
                    cacheName, stats.getHitRate(), threshold);
        }
    }
    
    /**
     * 检查指标是否启用
     */
    private boolean isMetricsEnabled() {
        return cacheProperties.getMetrics().isEnabled();
    }
}
