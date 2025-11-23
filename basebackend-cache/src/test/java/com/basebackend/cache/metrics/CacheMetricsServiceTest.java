package com.basebackend.cache.metrics;

import com.basebackend.cache.config.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存指标服务测试
 */
class CacheMetricsServiceTest {
    
    private CacheMetricsService metricsService;
    private CacheMetricsCollector metricsCollector;
    private CacheProperties cacheProperties;
    private MeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new CacheMetricsCollector(meterRegistry);
        
        cacheProperties = new CacheProperties();
        cacheProperties.getMetrics().setEnabled(true);
        cacheProperties.getMetrics().setLowHitRateThreshold(0.5);
        
        metricsService = new CacheMetricsService(metricsCollector, cacheProperties);
    }
    
    @Test
    void testRecordHit() {
        // When
        metricsService.recordHit("testCache");
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertEquals(1, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        assertEquals(1, stats.getTotalCount());
        assertEquals(1.0, stats.getHitRate(), 0.001);
    }
    
    @Test
    void testRecordMiss() {
        // When
        metricsService.recordMiss("testCache");
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertEquals(0, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(1, stats.getTotalCount());
        assertEquals(0.0, stats.getHitRate(), 0.001);
    }
    
    @Test
    void testRecordHitRate() {
        // When
        metricsService.recordHit("testCache");
        metricsService.recordHit("testCache");
        metricsService.recordMiss("testCache");
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertEquals(2, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(3, stats.getTotalCount());
        assertEquals(2.0 / 3.0, stats.getHitRate(), 0.001);
    }
    
    @Test
    void testRecordLatency() {
        // When
        metricsService.recordLatency("testCache", "GET", 100);
        metricsService.recordLatency("testCache", "SET", 50);
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertEquals(75, stats.getAverageLoadTime()); // (100 + 50) / 2
    }
    
    @Test
    void testRecordSet() {
        // When
        metricsService.recordSet("testCache", 50, true);
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertNotNull(stats);
        assertEquals("testCache", stats.getCacheName());
    }
    
    @Test
    void testRecordEviction() {
        // When
        metricsService.recordEviction("testCache", 10, true);
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertEquals(1, stats.getEvictionCount());
    }
    
    @Test
    void testRecordError() {
        // When
        metricsService.recordError("testCache", "GET", "Connection failed");
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertNotNull(stats);
    }
    
    @Test
    void testResetStatistics() {
        // Given
        metricsService.recordHit("testCache");
        metricsService.recordMiss("testCache");
        
        // When
        metricsService.resetStatistics("testCache");
        
        // Then
        CacheStatistics stats = metricsService.getStatistics("testCache");
        assertEquals(0, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
    }
    
    @Test
    void testGetAllCacheNames() {
        // When
        metricsService.recordHit("cache1");
        metricsService.recordHit("cache2");
        metricsService.recordHit("cache3");
        
        // Then
        assertEquals(3, metricsService.getAllCacheNames().size());
        assertTrue(metricsService.getAllCacheNames().contains("cache1"));
        assertTrue(metricsService.getAllCacheNames().contains("cache2"));
        assertTrue(metricsService.getAllCacheNames().contains("cache3"));
    }
    
    @Test
    void testMetricsDisabled() {
        // Given
        cacheProperties.getMetrics().setEnabled(false);
        CacheMetricsService disabledService = new CacheMetricsService(metricsCollector, cacheProperties);
        
        // When
        disabledService.recordHit("testCache");
        
        // Then - metrics should not be recorded when disabled
        CacheStatistics stats = disabledService.getStatistics("testCache");
        assertEquals(0, stats.getHitCount());
    }
    
    @Test
    void testGetStatisticsForNonExistentCache() {
        // When
        CacheStatistics stats = metricsService.getStatistics("nonExistent");
        
        // Then
        assertNotNull(stats);
        assertEquals("nonExistent", stats.getCacheName());
        assertEquals(0, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        assertEquals(0.0, stats.getHitRate());
    }
}
