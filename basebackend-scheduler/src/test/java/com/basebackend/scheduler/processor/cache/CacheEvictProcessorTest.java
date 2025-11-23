package com.basebackend.scheduler.processor.cache;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 缓存清理处理器测试。
 */
class CacheEvictProcessorTest {

    @Test
    void testProcessWithPattern() {
        // Mock RedisTemplate
        RedisTemplate<String, Object> mockRedis = mock(RedisTemplate.class);

        // 模拟keys()方法返回结果
        Set<String> mockKeys = Set.of("key:1", "key:2", "key:3");
        when(mockRedis.keys("*")).thenReturn(mockKeys);
        when(mockRedis.getExpire("key:1")).thenReturn(100L);
        when(mockRedis.getExpire("key:2")).thenReturn(200L);
        when(mockRedis.getExpire("key:3")).thenReturn(-1L);  // 已过期
        when(mockRedis.delete("key:3")).thenReturn(true);

        // 创建处理器
        CacheEvictProcessor processor = new CacheEvictProcessor(mockRedis);

        // 构建任务上下文
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "*");
        params.put("maxAge", -1L);  // 删除所有匹配的键
        TaskContext context = TaskContext.builder("cache-evict-test")
                .parameters(params)
                .build();

        // 执行
        TaskResult result = processor.process(context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
        assertEquals(1L, result.getOutput().get("deletedCount"));
        assertEquals(1L, result.getOutput().get("expiredDeleted"));

        // 验证Redis调用
        verify(mockRedis).keys("*");
        verify(mockRedis).delete("key:3");
    }

    @Test
    void testProcessWithMaxAgeFilter() {
        // Mock RedisTemplate
        RedisTemplate<String, Object> mockRedis = mock(RedisTemplate.class);

        // 模拟keys()方法返回结果
        Set<String> mockKeys = Set.of("key:1", "key:2", "key:3");
        when(mockRedis.keys("*")).thenReturn(mockKeys);
        when(mockRedis.getExpire("key:1")).thenReturn(150L);  // 超过阈值，不删除
        when(mockRedis.getExpire("key:2")).thenReturn(50L);   // 超过阈值，不删除
        when(mockRedis.getExpire("key:3")).thenReturn(-1L);   // 已过期，删除
        when(mockRedis.delete("key:3")).thenReturn(true);

        // 创建处理器
        CacheEvictProcessor processor = new CacheEvictProcessor(mockRedis);

        // 构建任务上下文
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "*");
        params.put("maxAge", 100L);  // 只删除TTL小于等于100秒的键
        TaskContext context = TaskContext.builder("cache-evict-test")
                .parameters(params)
                .build();

        // 执行
        TaskResult result = processor.process(context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
        assertEquals(1L, result.getOutput().get("deletedCount"));
        assertEquals(1L, result.getOutput().get("expiredDeleted"));
    }

    @Test
    void testProcessWithBatchSize() {
        // Mock RedisTemplate
        RedisTemplate<String, Object> mockRedis = mock(RedisTemplate.class);

        // 模拟keys()方法返回大量结果
        Set<String> mockKeys = Set.of("key:1", "key:2", "key:3", "key:4", "key:5");
        when(mockRedis.keys("*")).thenReturn(mockKeys);
        when(mockRedis.getExpire(anyString())).thenReturn(-1L);  // 全部已过期
        when(mockRedis.delete(anyString())).thenReturn(true);

        // 创建处理器
        CacheEvictProcessor processor = new CacheEvictProcessor(mockRedis);

        // 构建任务上下文
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "*");
        params.put("maxAge", -1L);
        params.put("batchSize", 3);  // 限制为3个
        TaskContext context = TaskContext.builder("cache-evict-test")
                .parameters(params)
                .build();

        // 执行
        TaskResult result = processor.process(context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
        // 实际删除数量应该小于等于batchSize
        Long deletedCount = (Long) result.getOutput().get("deletedCount");
        assertTrue(deletedCount <= 3);
    }

    @Test
    void testProcessWithNoKeys() {
        // Mock RedisTemplate
        RedisTemplate<String, Object> mockRedis = mock(RedisTemplate.class);

        // 模拟keys()方法返回空结果
        when(mockRedis.keys("*")).thenReturn(Collections.emptySet());

        // 创建处理器
        CacheEvictProcessor processor = new CacheEvictProcessor(mockRedis);

        // 构建任务上下文
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "*");
        params.put("maxAge", -1L);
        TaskContext context = TaskContext.builder("cache-evict-test")
                .parameters(params)
                .build();

        // 执行
        TaskResult result = processor.process(context);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
        assertEquals(0L, result.getOutput().get("deletedCount"));
        assertEquals(0L, result.getOutput().get("expiredDeleted"));
        assertEquals(0L, result.getOutput().get("freedBytes"));
    }

    @Test
    void testProcessorName() {
        RedisTemplate<String, Object> mockRedis = mock(RedisTemplate.class);
        CacheEvictProcessor processor = new CacheEvictProcessor(mockRedis);

        assertEquals("cache-evict", processor.name());
    }
}
