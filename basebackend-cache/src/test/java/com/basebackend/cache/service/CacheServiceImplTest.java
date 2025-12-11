package com.basebackend.cache.service;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.CacheEvictionManager;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.metrics.CacheMetricsService;
import com.basebackend.cache.metrics.CacheStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存服务实现单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheServiceImplTest {

    @Mock
    private RedisService redisService;

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private CacheMetricsService metricsService;

    @Mock
    private CacheEvictionManager evictionManager;

    @Mock
    private MultiLevelCacheManager multiLevelCacheManager;

    private CacheServiceImpl cacheService;

    @BeforeEach
    void setUp() {
        // Create cache service instance
        cacheService = new CacheServiceImpl(redisService, cacheProperties, metricsService, evictionManager);

        // Set multiLevelCacheManager to null via reflection to ensure Redis path is used
        try {
            var field = CacheServiceImpl.class.getDeclaredField("multiLevelCacheManager");
            field.setAccessible(true);
            field.set(cacheService, null);
        } catch (Exception e) {
            // Ignore if field doesn't exist or can't be set
        }
    }

    /**
     * 设置缓存属性配置的辅助方法
     * 用于需要buildCachePattern或isMultiLevelEnabled的测试
     */
    private void setupCacheProperties() {
        // Setup multi-level cache to be disabled
        CacheProperties.MultiLevel multiLevel = new CacheProperties.MultiLevel();
        multiLevel.setEnabled(false);
        when(cacheProperties.getMultiLevel()).thenReturn(multiLevel);

        // Setup key configuration
        CacheProperties.Key key = new CacheProperties.Key();
        key.setPrefix("basebackend");
        key.setSeparator(":");
        when(cacheProperties.getKey()).thenReturn(key);

        // Ensure multiLevelCacheManager is null to avoid using multi-level cache
        // This is needed because @Mock will not be null
    }

    @Test
    void testGet_Success() {
        // Given
        setupCacheProperties();
        String key = "user:123";
        String value = "John Doe";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.get(key)).thenReturn(value);

        // When
        String result = cacheService.get(key, String.class);

        // Then
        assertEquals(value, result);
        verify(redisService).get(key);
        verify(metricsService).recordHit(anyString());
        verify(metricsService).recordLatency(anyString(), eq("GET"), anyLong());
    }

    @Test
    void testGet_Miss() {
        // Given
        setupCacheProperties();
        String key = "user:123";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.get(key)).thenReturn(null);

        // When
        String result = cacheService.get(key, String.class);

        // Then
        assertNull(result);
        verify(redisService).get(key);
        verify(metricsService).recordMiss(anyString());
        verify(metricsService).recordLatency(anyString(), eq("GET"), anyLong());
    }

    @Test
    void testGet_InvalidKey() {
        // Given
        String key = null;
        when(evictionManager.validateKey(key)).thenReturn(false);

        // When
        String result = cacheService.get(key, String.class);

        // Then
        assertNull(result);
        verify(redisService, never()).get(anyString());
    }

    @Test
    void testSet_Success() {
        // Given
        setupCacheProperties();
        String key = "user:123";
        String value = "John Doe";
        Duration ttl = Duration.ofHours(1);
        when(evictionManager.validateKey(key)).thenReturn(true);

        // When
        cacheService.set(key, value, ttl);

        // Then
        verify(redisService).set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
        verify(metricsService).recordSet(anyString(), anyLong(), eq(true));
    }

    @Test
    void testSet_InvalidKey() {
        // Given
        String key = null;
        String value = "John Doe";
        when(evictionManager.validateKey(key)).thenReturn(false);

        // When
        cacheService.set(key, value);

        // Then
        verify(redisService, never()).set(anyString(), any());
    }

    @Test
    void testDelete_Success() {
        // Given
        setupCacheProperties();
        String key = "user:123";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.delete(key)).thenReturn(true);

        // When
        boolean result = cacheService.delete(key);

        // Then
        assertTrue(result);
        verify(redisService).delete(key);
        verify(metricsService).recordEviction(anyString(), anyLong(), eq(true));
    }

    @Test
    void testExists_True() {
        // Given
        String key = "user:123";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.hasKey(key)).thenReturn(true);

        // When
        boolean result = cacheService.exists(key);

        // Then
        assertTrue(result);
        verify(redisService).hasKey(key);
    }

    @Test
    void testExists_False() {
        // Given
        String key = "user:123";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.hasKey(key)).thenReturn(false);

        // When
        boolean result = cacheService.exists(key);

        // Then
        assertFalse(result);
        verify(redisService).hasKey(key);
    }

    @Test
    void testGetOrLoad_CacheHit() {
        // Given
        setupCacheProperties();
        String key = "user:123";
        String value = "John Doe";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.get(key)).thenReturn(value);

        // When
        String result = cacheService.getOrLoad(key, () -> "Loaded Value", Duration.ofHours(1));

        // Then
        assertEquals(value, result);
        verify(redisService).get(key);
        verify(redisService, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void testGetOrLoad_CacheMiss() {
        // Given
        setupCacheProperties();
        String key = "user:123";
        String loadedValue = "Loaded Value";
        Duration ttl = Duration.ofHours(1);
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.get(key)).thenReturn(null);

        // When
        String result = cacheService.getOrLoad(key, () -> loadedValue, ttl);

        // Then
        assertEquals(loadedValue, result);
        verify(redisService).get(key);
        verify(redisService).set(key, loadedValue, ttl.getSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void testMultiGet() {
        // Given
        Set<String> keys = Set.of("key1", "key2");
        Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2");
        when(redisService.multiGet(keys)).thenReturn(expected);

        // When
        Map<String, String> result = cacheService.multiGet(keys, String.class);

        // Then
        assertNotNull(result);
        verify(redisService).multiGet(keys);
    }

    @Test
    void testMultiSet() {
        // Given
        Map<String, Object> entries = Map.of("key1", "value1", "key2", "value2");
        Duration ttl = Duration.ofHours(1);

        // When
        cacheService.multiSet(entries, ttl);

        // Then
        verify(redisService).multiSet(entries, ttl);
    }

    @Test
    void testMultiDelete() {
        // Given
        Set<String> keys = Set.of("key1", "key2");
        when(redisService.delete(keys)).thenReturn(2L);

        // When
        long result = cacheService.multiDelete(keys);

        // Then
        assertEquals(2L, result);
        verify(redisService).delete(keys);
    }

    @Test
    void testDeleteByPattern() {
        // Given
        String pattern = "user:*";
        when(redisService.deleteByPattern(pattern)).thenReturn(10L);

        // When
        long result = cacheService.deleteByPattern(pattern);

        // Then
        assertEquals(10L, result);
        verify(redisService).deleteByPattern(pattern);
    }

    @Test
    void testKeys() {
        // Given
        String pattern = "user:*";
        Set<String> expected = Set.of("user:1", "user:2");
        when(redisService.keys(pattern)).thenReturn(expected);

        // When
        Set<String> result = cacheService.keys(pattern);

        // Then
        assertEquals(expected, result);
        verify(redisService).keys(pattern);
    }

    @Test
    void testValidateCacheName_Valid() {
        // When
        boolean result = cacheService.validateCacheName("user-cache");

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateCacheName_Invalid_Null() {
        // When
        boolean result = cacheService.validateCacheName(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCacheName_Invalid_TooLong() {
        // Given
        String longName = "a".repeat(101);

        // When
        boolean result = cacheService.validateCacheName(longName);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateCacheName_Invalid_SpecialChars() {
        // When
        boolean result = cacheService.validateCacheName("user@cache");

        // Then
        assertFalse(result);
    }

    @Test
    void testClearCache() {
        // Given
        setupCacheProperties();
        String cacheName = "user-cache";
        when(evictionManager.evictByPattern(anyString())).thenReturn(10L);

        // When
        long result = cacheService.clearCache(cacheName);

        // Then
        assertEquals(10L, result);
        verify(evictionManager).evictByPattern(anyString());
    }

    @Test
    void testClearAllCaches() {
        // Given
        when(evictionManager.clearAll()).thenReturn(100L);

        // When
        long result = cacheService.clearAllCaches();

        // Then
        assertEquals(100L, result);
        verify(evictionManager).clearAll();
    }

    @Test
    void testGetStatistics() {
        // Given
        setupCacheProperties();
        String cacheName = "user-cache";
        CacheStatistics expected = CacheStatistics.builder()
                .cacheName(cacheName)
                .hitCount(100)
                .missCount(20)
                .build();
        when(metricsService.getStatistics(cacheName)).thenReturn(expected);

        // When
        CacheStatistics result = cacheService.getStatistics(cacheName);

        // Then
        assertEquals(expected, result);
        verify(metricsService).getStatistics(cacheName);
    }

    @Test
    void testGetCacheSize() {
        // Given
        setupCacheProperties();
        String cacheName = "user-cache";
        when(evictionManager.getCacheSize(anyString())).thenReturn(50L);

        // When
        long result = cacheService.getCacheSize(cacheName);

        // Then
        assertEquals(50L, result);
        verify(evictionManager).getCacheSize(anyString());
    }

    @Test
    void testGetAllCacheNames() {
        // Given
        Set<String> expected = Set.of("cache1", "cache2");
        when(metricsService.getAllCacheNames()).thenReturn(expected);

        // When
        Set<String> result = cacheService.getAllCacheNames();

        // Then
        assertEquals(expected, result);
        verify(metricsService).getAllCacheNames();
    }

    @Test
    void testResetStatistics() {
        // Given
        setupCacheProperties();
        String cacheName = "user-cache";

        // When
        cacheService.resetStatistics(cacheName);

        // Then
        verify(metricsService).resetStatistics(cacheName);
    }

    @Test
    void testGetExpiration() {
        // Given
        String key = "user:123";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(evictionManager.getKeyExpiration(key)).thenReturn(3600L);

        // When
        long result = cacheService.getExpiration(key);

        // Then
        assertEquals(3600L, result);
        verify(evictionManager).getKeyExpiration(key);
    }

    @Test
    void testSetExpiration() {
        // Given
        String key = "user:123";
        Duration duration = Duration.ofHours(1);
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(evictionManager.setKeyExpiration(key, duration)).thenReturn(true);

        // When
        boolean result = cacheService.setExpiration(key, duration);

        // Then
        assertTrue(result);
        verify(evictionManager).setKeyExpiration(key, duration);
    }

    @Test
    void testRemoveExpiration() {
        // Given
        String key = "user:123";
        when(evictionManager.validateKey(key)).thenReturn(true);
        when(redisService.expire(key, -1, TimeUnit.SECONDS)).thenReturn(true);

        // When
        boolean result = cacheService.removeExpiration(key);

        // Then
        assertTrue(result);
        verify(redisService).expire(key, -1, TimeUnit.SECONDS);
    }

    @Test
    void testEnforceCapacity() {
        // Given
        when(evictionManager.enforceCapacity()).thenReturn(10L);

        // When
        long result = cacheService.enforceCapacity();

        // Then
        assertEquals(10L, result);
        verify(evictionManager).enforceCapacity();
    }

    @Test
    void testSetMaxCacheSize() {
        // When
        cacheService.setMaxCacheSize(1000L);

        // Then
        verify(evictionManager).setMaxCacheSize(1000L);
    }

    @Test
    void testGetMaxCacheSize() {
        // Given
        when(evictionManager.getMaxCacheSize()).thenReturn(1000L);

        // When
        long result = cacheService.getMaxCacheSize();

        // Then
        assertEquals(1000L, result);
        verify(evictionManager).getMaxCacheSize();
    }
}
