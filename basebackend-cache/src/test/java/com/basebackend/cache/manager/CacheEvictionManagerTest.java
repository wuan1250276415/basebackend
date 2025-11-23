package com.basebackend.cache.manager;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存淘汰管理器单元测试
 */
@ExtendWith(MockitoExtension.class)
class CacheEvictionManagerTest {

    @Mock
    private RedisService redisService;

    @Mock
    private CacheProperties cacheProperties;

    private CacheEvictionManager evictionManager;

    @BeforeEach
    void setUp() {
        evictionManager = new CacheEvictionManager(redisService, cacheProperties);
    }

    @Test
    void testEvictByPattern_Success() {
        // Given
        String pattern = "user:*";
        when(redisService.deleteByPattern(pattern)).thenReturn(10L);

        // When
        long deleted = evictionManager.evictByPattern(pattern);

        // Then
        assertEquals(10L, deleted);
        verify(redisService).deleteByPattern(pattern);
    }

    @Test
    void testEvictByPattern_InvalidPattern() {
        // When
        long deleted = evictionManager.evictByPattern(null);

        // Then
        assertEquals(0L, deleted);
        verify(redisService, never()).deleteByPattern(anyString());
    }

    @Test
    void testEvict_Success() {
        // Given
        String key = "user:123";
        when(redisService.delete(key)).thenReturn(true);

        // When
        boolean result = evictionManager.evict(key);

        // Then
        assertTrue(result);
        verify(redisService).delete(key);
    }

    @Test
    void testEvict_InvalidKey() {
        // When
        boolean result = evictionManager.evict(null);

        // Then
        assertFalse(result);
        verify(redisService, never()).delete(anyString());
    }

    @Test
    void testEvictBatch_Success() {
        // Given
        Set<String> keys = Set.of("key1", "key2", "key3");
        when(redisService.delete(keys)).thenReturn(3L);

        // When
        long deleted = evictionManager.evictBatch(keys);

        // Then
        assertEquals(3L, deleted);
        verify(redisService).delete(keys);
    }

    @Test
    void testEvictBatch_EmptyKeys() {
        // When
        long deleted = evictionManager.evictBatch(Set.of());

        // Then
        assertEquals(0L, deleted);
        verify(redisService, never()).delete(anySet());
    }

    @Test
    void testGetCacheSize() {
        // Given
        Set<String> keys = Set.of("key1", "key2", "key3");
        when(redisService.keys("*")).thenReturn(keys);

        // When
        long size = evictionManager.getCacheSize();

        // Then
        assertEquals(3L, size);
        verify(redisService).keys("*");
    }

    @Test
    void testGetCacheSizeByPattern() {
        // Given
        String pattern = "user:*";
        Set<String> keys = Set.of("user:1", "user:2");
        when(redisService.keys(pattern)).thenReturn(keys);

        // When
        long size = evictionManager.getCacheSize(pattern);

        // Then
        assertEquals(2L, size);
        verify(redisService).keys(pattern);
    }

    @Test
    void testSetAndGetMaxCacheSize() {
        // When
        evictionManager.setMaxCacheSize(1000L);

        // Then
        assertEquals(1000L, evictionManager.getMaxCacheSize());
    }

    @Test
    void testSetAndGetEvictionPolicy() {
        // When
        evictionManager.setEvictionPolicy(CacheEvictionManager.EvictionPolicy.LFU);

        // Then
        assertEquals(CacheEvictionManager.EvictionPolicy.LFU, evictionManager.getEvictionPolicy());
    }

    @Test
    void testClearAll() {
        // Given
        Set<String> keys = Set.of("key1", "key2", "key3");
        when(redisService.keys("*")).thenReturn(keys);
        when(redisService.delete(keys)).thenReturn(3L);

        // When
        long deleted = evictionManager.clearAll();

        // Then
        assertEquals(3L, deleted);
        verify(redisService).keys("*");
        verify(redisService).delete(keys);
    }

    @Test
    void testClearByPrefix() {
        // Given
        String prefix = "user";
        String pattern = "user*";
        when(redisService.deleteByPattern(pattern)).thenReturn(5L);

        // When
        long deleted = evictionManager.clearByPrefix(prefix);

        // Then
        assertEquals(5L, deleted);
        verify(redisService).deleteByPattern(pattern);
    }

    @Test
    void testValidateKey_Valid() {
        // When
        boolean valid = evictionManager.validateKey("user:123");

        // Then
        assertTrue(valid);
    }

    @Test
    void testValidateKey_Null() {
        // When
        boolean valid = evictionManager.validateKey(null);

        // Then
        assertFalse(valid);
    }

    @Test
    void testValidateKey_Empty() {
        // When
        boolean valid = evictionManager.validateKey("");

        // Then
        assertFalse(valid);
    }

    @Test
    void testValidateKey_TooLong() {
        // Given
        String longKey = "a".repeat(1025);

        // When
        boolean valid = evictionManager.validateKey(longKey);

        // Then
        assertFalse(valid);
    }

    @Test
    void testGetKeyExpiration() {
        // Given
        String key = "user:123";
        when(redisService.getExpire(key)).thenReturn(3600L);

        // When
        long expiration = evictionManager.getKeyExpiration(key);

        // Then
        assertEquals(3600L, expiration);
        verify(redisService).getExpire(key);
    }

    @Test
    void testSetKeyExpiration() {
        // Given
        String key = "user:123";
        Duration duration = Duration.ofHours(1);
        when(redisService.expire(key, duration.getSeconds(), TimeUnit.SECONDS)).thenReturn(true);

        // When
        boolean result = evictionManager.setKeyExpiration(key, duration);

        // Then
        assertTrue(result);
        verify(redisService).expire(key, duration.getSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void testEnforceCapacity_NoLimit() {
        // Given
        evictionManager.setMaxCacheSize(-1);

        // When
        long evicted = evictionManager.enforceCapacity();

        // Then
        assertEquals(0L, evicted);
    }

    @Test
    void testEnforceCapacity_WithinLimit() {
        // Given
        evictionManager.setMaxCacheSize(100);
        Set<String> keys = Set.of("key1", "key2");
        when(redisService.keys("*")).thenReturn(keys);

        // When
        long evicted = evictionManager.enforceCapacity();

        // Then
        assertEquals(0L, evicted);
    }
}
