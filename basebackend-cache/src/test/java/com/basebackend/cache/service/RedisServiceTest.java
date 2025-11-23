package com.basebackend.cache.service;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.exception.CacheConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisService 单元测试
 * 测试批量操作、模式匹配删除、异常处理和降级逻辑
 * Requirements: 6.1, 8.3, 9.1
 */
@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    private CacheProperties cacheProperties;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        cacheProperties = new CacheProperties();
        cacheProperties.setEnabled(true);
        
        // 配置容错
        CacheProperties.Resilience resilience = new CacheProperties.Resilience();
        resilience.setFallbackEnabled(true);
        resilience.setTimeout(Duration.ofSeconds(3));
        cacheProperties.setResilience(resilience);
        
        // 配置序列化
        CacheProperties.Serialization serialization = new CacheProperties.Serialization();
        serialization.setType("json");
        cacheProperties.setSerialization(serialization);
        
        redisService = new RedisService(redisTemplate, cacheProperties);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void testMultiGet_Success() {
        // Arrange
        Set<String> keys = new HashSet<>(Arrays.asList("key1", "key2", "key3"));
        List<Object> values = Arrays.asList("value1", "value2", "value3");
        when(valueOperations.multiGet(keys)).thenReturn(values);

        // Act
        Map<String, String> result = redisService.multiGet(keys);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(valueOperations).multiGet(keys);
    }

    @Test
    void testMultiGet_EmptyKeys() {
        // Act
        Map<String, String> result = redisService.multiGet(Collections.emptySet());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(valueOperations, never()).multiGet(any());
    }

    @Test
    void testMultiSet_WithTTL() {
        // Arrange
        Map<String, Object> entries = new HashMap<>();
        entries.put("key1", "value1");
        entries.put("key2", "value2");
        Duration ttl = Duration.ofMinutes(5);

        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class)))
            .thenReturn(Collections.emptyList());

        // Act
        redisService.multiSet(entries, ttl);

        // Assert
        verify(redisTemplate).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    @Test
    void testMultiSet_WithoutTTL() {
        // Arrange
        Map<String, Object> entries = new HashMap<>();
        entries.put("key1", "value1");
        entries.put("key2", "value2");

        // Act
        redisService.multiSet(entries);

        // Assert
        verify(valueOperations).multiSet(entries);
    }

    @Test
    void testDeleteByPattern_Success() {
        // Arrange
        String pattern = "test:*";
        Set<String> matchingKeys = new HashSet<>(Arrays.asList("test:1", "test:2", "test:3"));
        when(redisTemplate.keys(pattern)).thenReturn(matchingKeys);
        when(redisTemplate.delete(matchingKeys)).thenReturn(3L);

        // Act
        long deleted = redisService.deleteByPattern(pattern);

        // Assert
        assertEquals(3L, deleted);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(matchingKeys);
    }

    @Test
    void testDeleteByPattern_NoMatches() {
        // Arrange
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(Collections.emptySet());

        // Act
        long deleted = redisService.deleteByPattern(pattern);

        // Assert
        assertEquals(0L, deleted);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    @Test
    void testFallback_WhenRedisConnectionFails() {
        // Arrange
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act
        Object result = redisService.get("test-key");

        // Assert
        assertNull(result); // 降级返回 null
        assertFalse(redisService.isRedisAvailable());
    }

    @Test
    void testSet_WithFallback() {
        // Arrange
        doThrow(new RedisConnectionFailureException("Connection failed"))
            .when(valueOperations).set(anyString(), any());

        // Act & Assert
        assertDoesNotThrow(() -> redisService.set("key", "value"));
        assertFalse(redisService.isRedisAvailable());
    }

    @Test
    void testGet_WithFallback() {
        // Arrange
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act
        Object result = redisService.get("test-key");

        // Assert
        assertNull(result);
        assertFalse(redisService.isRedisAvailable());
    }

    @Test
    void testDelete_WithFallback() {
        // Arrange
        when(redisTemplate.delete(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act
        Boolean result = redisService.delete("test-key");

        // Assert
        assertFalse(result);
        assertFalse(redisService.isRedisAvailable());
    }

    @Test
    void testRedisRecovery() {
        // Arrange - 先模拟失败
        when(valueOperations.get("key1"))
            .thenThrow(new RedisConnectionFailureException("Connection failed"));
        
        // Act - 第一次调用失败
        Object result1 = redisService.get("key1");
        assertNull(result1);
        assertFalse(redisService.isRedisAvailable());

        // Arrange - 模拟恢复
        when(valueOperations.get("key2")).thenReturn("value2");
        
        // Act - 第二次调用成功
        Object result2 = redisService.get("key2");
        
        // Assert - Redis 应该标记为可用
        assertEquals("value2", result2);
        assertTrue(redisService.isRedisAvailable());
    }

    @Test
    void testIsRedisAvailable_InitiallyTrue() {
        assertTrue(redisService.isRedisAvailable());
    }

    // ========== 批量操作测试 ==========

    @Test
    void testMultiGet_WithNullValues() {
        // Arrange
        Set<String> keys = new HashSet<>(Arrays.asList("key1", "key2", "key3"));
        List<Object> values = Arrays.asList("value1", null, "value3");
        when(valueOperations.multiGet(keys)).thenReturn(values);

        // Act
        Map<String, String> result = redisService.multiGet(keys);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // 只有非 null 的值
        assertFalse(result.containsKey("key2"));
    }

    @Test
    void testMultiGet_NullInput() {
        // Act
        Map<String, String> result = redisService.multiGet(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMultiSet_EmptyEntries() {
        // Act
        redisService.multiSet(Collections.emptyMap(), Duration.ofMinutes(5));

        // Assert
        verify(valueOperations, never()).multiSet(any());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void testMultiSet_NullEntries() {
        // Act
        redisService.multiSet(null, Duration.ofMinutes(5));

        // Assert
        verify(valueOperations, never()).multiSet(any());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void testMultiSet_ZeroTTL() {
        // Arrange
        Map<String, Object> entries = new HashMap<>();
        entries.put("key1", "value1");
        Duration ttl = Duration.ZERO;

        // Act
        redisService.multiSet(entries, ttl);

        // Assert
        verify(valueOperations).multiSet(entries);
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void testMultiSet_NegativeTTL() {
        // Arrange
        Map<String, Object> entries = new HashMap<>();
        entries.put("key1", "value1");
        Duration ttl = Duration.ofSeconds(-1);

        // Act
        redisService.multiSet(entries, ttl);

        // Assert
        verify(valueOperations).multiSet(entries);
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    // ========== 模式匹配删除测试 ==========

    @Test
    void testDeleteByPattern_EmptyPattern() {
        // Act
        long deleted = redisService.deleteByPattern("");

        // Assert
        assertEquals(0L, deleted);
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void testDeleteByPattern_NullPattern() {
        // Act
        long deleted = redisService.deleteByPattern(null);

        // Assert
        assertEquals(0L, deleted);
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void testDeleteByPattern_NullKeysReturned() {
        // Arrange
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(null);

        // Act
        long deleted = redisService.deleteByPattern(pattern);

        // Assert
        assertEquals(0L, deleted);
        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    @Test
    void testDeleteByPattern_DeleteReturnsNull() {
        // Arrange
        String pattern = "test:*";
        Set<String> matchingKeys = new HashSet<>(Arrays.asList("test:1", "test:2"));
        when(redisTemplate.keys(pattern)).thenReturn(matchingKeys);
        when(redisTemplate.delete(matchingKeys)).thenReturn(null);

        // Act
        long deleted = redisService.deleteByPattern(pattern);

        // Assert
        assertEquals(0L, deleted);
    }

    // ========== Pipeline 操作测试 ==========

    @Test
    void testExecutePipeline_Success() {
        // Arrange
        List<RedisService.PipelineOperation> operations = new ArrayList<>();
        operations.add(connection -> {});
        operations.add(connection -> {});
        
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
            .thenReturn(Arrays.asList("result1", "result2"));

        // Act
        List<Object> results = redisService.executePipeline(operations);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(redisTemplate).executePipelined(any(RedisCallback.class));
    }

    @Test
    void testExecutePipeline_EmptyOperations() {
        // Act
        List<Object> results = redisService.executePipeline(Collections.emptyList());

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void testExecutePipeline_NullOperations() {
        // Act
        List<Object> results = redisService.executePipeline(null);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    // ========== 异常处理和降级测试 ==========

    @Test
    void testFallback_WhenDataAccessException() {
        // Arrange
        when(valueOperations.get(anyString())).thenThrow(new DataAccessException("Data access error") {});

        // Act
        Object result = redisService.get("test-key");

        // Assert
        assertNull(result);
        assertFalse(redisService.isRedisAvailable());
    }

    @Test
    void testFallback_DisabledThrowsException() {
        // Arrange
        cacheProperties.getResilience().setFallbackEnabled(false);
        redisService = new RedisService(redisTemplate, cacheProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act & Assert
        assertThrows(CacheConnectionException.class, () -> redisService.get("test-key"));
    }

    @Test
    void testCircuitBreaker_OpensAfterFailure() {
        // Arrange
        CacheProperties.Resilience.CircuitBreaker circuitBreaker = 
            new CacheProperties.Resilience.CircuitBreaker();
        circuitBreaker.setEnabled(true);
        circuitBreaker.setOpenDuration(Duration.ofSeconds(1));
        cacheProperties.getResilience().setCircuitBreaker(circuitBreaker);
        
        redisService = new RedisService(redisTemplate, cacheProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // 模拟第一次失败
        when(valueOperations.get("key1"))
            .thenThrow(new RedisConnectionFailureException("Connection failed"));

        // Act - 第一次调用失败，熔断器打开
        Object result1 = redisService.get("key1");
        assertNull(result1);
        assertFalse(redisService.isRedisAvailable());

        // Act - 第二次调用应该直接返回降级值，不调用 Redis
        Object result2 = redisService.get("key2");
        assertNull(result2);
        
        // Assert - 验证第二次没有调用 Redis（因为熔断器打开）
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    void testCircuitBreaker_RecoveryAfterOpenDuration() throws InterruptedException {
        // Arrange
        CacheProperties.Resilience.CircuitBreaker circuitBreaker = 
            new CacheProperties.Resilience.CircuitBreaker();
        circuitBreaker.setEnabled(true);
        circuitBreaker.setOpenDuration(Duration.ofMillis(100));
        cacheProperties.getResilience().setCircuitBreaker(circuitBreaker);
        
        redisService = new RedisService(redisTemplate, cacheProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // 模拟失败
        when(valueOperations.get("key1"))
            .thenThrow(new RedisConnectionFailureException("Connection failed"));
        
        // Act - 第一次失败
        redisService.get("key1");
        assertFalse(redisService.isRedisAvailable());

        // 等待熔断器打开时长过去
        Thread.sleep(150);

        // 模拟恢复
        when(valueOperations.get("key2")).thenReturn("value2");
        
        // Act - 尝试恢复
        Object result = redisService.get("key2");
        
        // Assert
        assertEquals("value2", result);
        assertTrue(redisService.isRedisAvailable());
    }

    @Test
    void testUnexpectedException_WithFallback() {
        // Arrange
        when(valueOperations.get(anyString()))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        Object result = redisService.get("test-key");

        // Assert
        assertNull(result);
    }

    @Test
    void testUnexpectedException_WithoutFallback() {
        // Arrange
        cacheProperties.getResilience().setFallbackEnabled(false);
        redisService = new RedisService(redisTemplate, cacheProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        assertThrows(CacheConnectionException.class, () -> redisService.get("test-key"));
    }

    // ========== String 操作测试 ==========

    @Test
    void testSet_Success() {
        // Act
        redisService.set("key", "value");

        // Assert
        verify(valueOperations).set("key", "value");
    }

    @Test
    void testSet_WithTimeout() {
        // Act
        redisService.set("key", "value", 60, TimeUnit.SECONDS);

        // Assert
        verify(valueOperations).set("key", "value", 60, TimeUnit.SECONDS);
    }

    @Test
    void testSet_WithSeconds() {
        // Act
        redisService.set("key", "value", 60L);

        // Assert
        verify(valueOperations).set("key", "value", 60L, TimeUnit.SECONDS);
    }

    @Test
    void testGet_Success() {
        // Arrange
        when(valueOperations.get("key")).thenReturn("value");

        // Act
        Object result = redisService.get("key");

        // Assert
        assertEquals("value", result);
    }

    @Test
    void testKeys_Success() {
        // Arrange
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("key1", "key2"));
        when(redisTemplate.keys("test:*")).thenReturn(expectedKeys);

        // Act
        Set<String> result = redisService.keys("test:*");

        // Assert
        assertEquals(expectedKeys, result);
    }

    @Test
    void testGetList_Success() {
        // Arrange
        List<Object> expectedList = Arrays.asList("item1", "item2");
        when(valueOperations.get("key")).thenReturn(expectedList);

        // Act
        List<Object> result = redisService.getList("key");

        // Assert
        assertEquals(expectedList, result);
    }

    @Test
    void testGetList_NotAList() {
        // Arrange
        when(valueOperations.get("key")).thenReturn("not a list");

        // Act
        List<Object> result = redisService.getList("key");

        // Assert
        assertNull(result);
    }

    @Test
    void testDelete_Success() {
        // Arrange
        when(redisTemplate.delete("key")).thenReturn(true);

        // Act
        Boolean result = redisService.delete("key");

        // Assert
        assertTrue(result);
    }

    @Test
    void testDelete_Collection() {
        // Arrange
        Collection<String> keys = Arrays.asList("key1", "key2");
        when(redisTemplate.delete(keys)).thenReturn(2L);

        // Act
        Long result = redisService.delete(keys);

        // Assert
        assertEquals(2L, result);
    }

    @Test
    void testExpire_Success() {
        // Arrange
        when(redisTemplate.expire("key", 60, TimeUnit.SECONDS)).thenReturn(true);

        // Act
        Boolean result = redisService.expire("key", 60, TimeUnit.SECONDS);

        // Assert
        assertTrue(result);
    }

    @Test
    void testGetExpire_Success() {
        // Arrange
        when(redisTemplate.getExpire("key")).thenReturn(60L);

        // Act
        Long result = redisService.getExpire("key");

        // Assert
        assertEquals(60L, result);
    }

    @Test
    void testHasKey_Success() {
        // Arrange
        when(redisTemplate.hasKey("key")).thenReturn(true);

        // Act
        Boolean result = redisService.hasKey("key");

        // Assert
        assertTrue(result);
    }

    @Test
    void testIncrement_Success() {
        // Arrange
        when(valueOperations.increment("key", 1L)).thenReturn(2L);

        // Act
        Long result = redisService.increment("key", 1L);

        // Assert
        assertEquals(2L, result);
    }

    @Test
    void testDecrement_Success() {
        // Arrange
        when(valueOperations.decrement("key", 1L)).thenReturn(0L);

        // Act
        Long result = redisService.decrement("key", 1L);

        // Assert
        assertEquals(0L, result);
    }

    // ========== Hash 操作测试 ==========

    @Test
    void testHGet_Success() {
        // Arrange
        when(hashOperations.get("key", "field")).thenReturn("value");

        // Act
        Object result = redisService.hGet("key", "field");

        // Assert
        assertEquals("value", result);
    }

    @Test
    void testHGetAll_Success() {
        // Arrange
        Map<Object, Object> expectedMap = new HashMap<>();
        expectedMap.put("field1", "value1");
        when(hashOperations.entries("key")).thenReturn(expectedMap);

        // Act
        Map<Object, Object> result = redisService.hGetAll("key");

        // Assert
        assertEquals(expectedMap, result);
    }

    @Test
    void testHSet_Success() {
        // Act
        redisService.hSet("key", "field", "value");

        // Assert
        verify(hashOperations).put("key", "field", "value");
    }

    @Test
    void testHSetAll_Success() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put("field1", "value1");

        // Act
        redisService.hSetAll("key", map);

        // Assert
        verify(hashOperations).putAll("key", map);
    }

    @Test
    void testHDelete_Success() {
        // Arrange
        when(hashOperations.delete("key", "field1", "field2")).thenReturn(2L);

        // Act
        Long result = redisService.hDelete("key", "field1", "field2");

        // Assert
        assertEquals(2L, result);
    }

    @Test
    void testHHasKey_Success() {
        // Arrange
        when(hashOperations.hasKey("key", "field")).thenReturn(true);

        // Act
        Boolean result = redisService.hHasKey("key", "field");

        // Assert
        assertTrue(result);
    }

    // ========== Set 操作测试 ==========

    @Test
    void testSAdd_Success() {
        // Arrange
        when(setOperations.add("key", "value1", "value2")).thenReturn(2L);

        // Act
        Long result = redisService.sAdd("key", "value1", "value2");

        // Assert
        assertEquals(2L, result);
    }

    @Test
    void testSMembers_Success() {
        // Arrange
        Set<Object> expectedSet = new HashSet<>(Arrays.asList("value1", "value2"));
        when(setOperations.members("key")).thenReturn(expectedSet);

        // Act
        Set<Object> result = redisService.sMembers("key");

        // Assert
        assertEquals(expectedSet, result);
    }

    @Test
    void testSIsMember_Success() {
        // Arrange
        when(setOperations.isMember("key", "value")).thenReturn(true);

        // Act
        Boolean result = redisService.sIsMember("key", "value");

        // Assert
        assertTrue(result);
    }

    @Test
    void testSSize_Success() {
        // Arrange
        when(setOperations.size("key")).thenReturn(5L);

        // Act
        Long result = redisService.sSize("key");

        // Assert
        assertEquals(5L, result);
    }

    @Test
    void testSRemove_Success() {
        // Arrange
        when(setOperations.remove("key", "value1", "value2")).thenReturn(2L);

        // Act
        Long result = redisService.sRemove("key", "value1", "value2");

        // Assert
        assertEquals(2L, result);
    }

    // ========== List 操作测试 ==========

    @Test
    void testLPush_Success() {
        // Arrange
        when(listOperations.rightPush("key", "value")).thenReturn(1L);

        // Act
        Long result = redisService.lPush("key", "value");

        // Assert
        assertEquals(1L, result);
    }

    @Test
    void testLLeftPush_Success() {
        // Arrange
        when(listOperations.leftPush("key", "value")).thenReturn(1L);

        // Act
        Long result = redisService.lLeftPush("key", "value");

        // Assert
        assertEquals(1L, result);
    }

    @Test
    void testLRange_Success() {
        // Arrange
        List<Object> expectedList = Arrays.asList("value1", "value2");
        when(listOperations.range("key", 0, -1)).thenReturn(expectedList);

        // Act
        List<Object> result = redisService.lRange("key", 0, -1);

        // Assert
        assertEquals(expectedList, result);
    }

    @Test
    void testLSize_Success() {
        // Arrange
        when(listOperations.size("key")).thenReturn(5L);

        // Act
        Long result = redisService.lSize("key");

        // Assert
        assertEquals(5L, result);
    }

    @Test
    void testLIndex_Success() {
        // Arrange
        when(listOperations.index("key", 0)).thenReturn("value");

        // Act
        Object result = redisService.lIndex("key", 0);

        // Assert
        assertEquals("value", result);
    }

    @Test
    void testLRemove_Success() {
        // Arrange
        when(listOperations.remove("key", 1, "value")).thenReturn(1L);

        // Act
        Long result = redisService.lRemove("key", 1, "value");

        // Assert
        assertEquals(1L, result);
    }

    @Test
    void testCircuitBreakerControl() {
        // Test manual circuit breaker control
        redisService.openCircuitBreaker();
        assertEquals("OPEN", redisService.getCircuitBreakerState());
        assertFalse(redisService.isRedisAvailable());

        redisService.resetCircuitBreaker();
        assertEquals("CLOSED", redisService.getCircuitBreakerState());
        assertTrue(redisService.isRedisAvailable());
    }
}
