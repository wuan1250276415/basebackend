package com.basebackend.examples.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 缓存保护工具
 * 提供缓存穿透防护、雪崩防护、击穿防护功能
 */
@Slf4j
@Component
public class CacheProtector {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ========================================
    // 布隆过滤器 (缓存穿透防护)
    // ========================================

    /**
     * 布隆过滤器实例 (按数据类型分组)
     */
    private final java.util.Map<String, BloomFilter<String>> bloomFilters = new ConcurrentHashMap<>();

    /**
     * 初始化布隆过滤器
     *
     * @param name     过滤器名称
     * @param capacity 预期容量
     * @param fpp      误判率
     */
    public void initBloomFilter(String name, long capacity, double fpp) {
        BloomFilter<String> filter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            capacity,
            fpp
        );
        bloomFilters.put(name, filter);
        log.info("布隆过滤器初始化完成: name={}, capacity={}, fpp={}", name, capacity, fpp);
    }

    /**
     * 添加 key 到布隆过滤器
     *
     * @param name 过滤器名称
     * @param key  缓存键
     */
    public void addToBloomFilter(String name, String key) {
        BloomFilter<String> filter = bloomFilters.get(name);
        if (filter != null) {
            filter.put(key);
            log.debug("布隆过滤器添加 key: {}", key);
        }
    }

    /**
     * 批量添加 keys 到布隆过滤器
     *
     * @param name 过滤器名称
     * @param keys 缓存键列表
     */
    public void addAllToBloomFilter(String name, java.util.List<String> keys) {
        BloomFilter<String> filter = bloomFilters.get(name);
        if (filter != null) {
            keys.forEach(filter::put);
            log.debug("布隆过滤器批量添加: {} 条", keys.size());
        }
    }

    /**
     * 检查 key 是否可能存在于缓存中
     *
     * @param name 过滤器名称
     * @param key  缓存键
     * @return true-可能存在, false-一定不存在
     */
    public boolean mightContain(String name, String key) {
        BloomFilter<String> filter = bloomFilters.get(name);
        if (filter == null) {
            log.warn("布隆过滤器不存在: {}", name);
            return true; // 过滤器不存在时默认返回 true，避免误杀
        }
        return filter.mightContain(key);
    }

    // ========================================
    // 缓存穿透防护
    // ========================================

    /**
     * 获取缓存（防穿透）
     *
     * @param name     布隆过滤器名称
     * @param key      缓存键
     * @param type     数据类型
     * @param ttl      过期时间
     * @param loader   数据加载器
     * @return 缓存值
     */
    public <T> T getWithPenetrationPrevent(String name, String key, Class<T> type, java.time.Duration ttl, Supplier<T> loader) {
        // 1. 布隆过滤器检查
        if (!mightContain(name, key)) {
            log.debug("布隆过滤器判定 key 不存在: {}", key);
            return null;
        }

        // 2. 尝试从缓存获取
        String cacheKey = "cache:" + key;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String cachedValue = ops.get(cacheKey);
        if (cachedValue != null) {
            log.debug("缓存命中: {}", key);
            return parseJson(cachedValue, type);
        }

        // 3. 缓存未命中，加载数据
        log.debug("缓存未命中，加载数据: {}", key);
        T value = loader.get();

        if (value != null) {
            // 写入缓存
            ops.set(cacheKey, toJson(value), ttl);
            // 添加到布隆过滤器
            addToBloomFilter(name, key);
            log.debug("数据已缓存: {}", key);
        } else {
            // 缓存空值（短期），防止穿透
            ops.set(cacheKey, "NULL", java.time.Duration.ofMinutes(5));
            log.debug("缓存空值: {}", key);
        }

        return value;
    }

    // ========================================
    // 缓存雪崩防护
    // ========================================

    /**
     * 缓存雪崩防护包装
     * 避免大量缓存在同一时间过期导致大量请求直达数据库
     *
     * @param key      缓存键
     * @param type     数据类型
     * @param baseTtl  基础过期时间
     * @param loader   数据加载器
     * @return 缓存值
     */
    public <T> T getWithAvalanchePrevent(String key, Class<T> type, java.time.Duration baseTtl, Supplier<T> loader) {
        // 1. 生成随机过期时间
        java.time.Duration randomTtl = generateRandomTtl(baseTtl);
        log.debug("随机 TTL: {}ms", randomTtl.toMillis());

        // 2. 先查缓存
        String cacheKey = "cache:" + key;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String cachedValue = ops.get(cacheKey);
        if (cachedValue != null) {
            log.debug("缓存命中: {}", key);
            return parseJson(cachedValue, type);
        }

        // 3. 使用分布式锁控制并发
        String lockKey = "lock:" + cacheKey;
        String lockValue = generateLockValue();

        try {
            // 获取锁（10秒过期）
            Boolean lockAcquired = ops.setIfAbsent(lockKey, lockValue, java.time.Duration.ofSeconds(10));
            if (Boolean.TRUE.equals(lockAcquired)) {
                log.debug("获取锁成功: {}", key);

                // 双重检查
                cachedValue = ops.get(cacheKey);
                if (cachedValue != null) {
                    log.debug("双重检查命中: {}", key);
                    return parseJson(cachedValue, type);
                }

                // 加载数据
                T value = loader.get();
                if (value != null) {
                    // 使用随机 TTL 写入缓存
                    ops.set(cacheKey, toJson(value), randomTtl);
                    log.debug("数据已缓存 (随机 TTL): {}", key);
                }

                return value;
            } else {
                // 获取锁失败，等待后重试
                log.debug("等待锁释放: {}", key);
                try {
                    Thread.sleep(100);
                    return getWithAvalanchePrevent(key, type, baseTtl, loader);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待锁释放被中断", e);
                    return null;
                }
            }
        } finally {
            // 释放锁
            String currentLockValue = ops.get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
                log.debug("释放锁: {}", key);
            }
        }
    }

    /**
     * 生成随机过期时间 (基础时间的 90% ~ 110%)
     */
    private java.time.Duration generateRandomTtl(java.time.Duration baseTtl) {
        long baseMillis = baseTtl.toMillis();
        double randomFactor = 0.9 + Math.random() * 0.2; // 0.9 ~ 1.1
        long randomMillis = (long) (baseMillis * randomFactor);
        return java.time.Duration.ofMillis(randomMillis);
    }

    // ========================================
    // 缓存击穿防护 (SingleFlight)
    // ========================================

    /**
     * 缓存击穿防护
     * 防止热点数据过期瞬间大量请求同时访问数据库
     */
    public <T> T getWithBreakdownPrevent(String key, Class<T> type, java.time.Duration ttl, Supplier<T> loader) {
        String cacheKey = "cache:" + key;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 1. 先查缓存
        String cachedValue = ops.get(cacheKey);
        if (cachedValue != null) {
            log.debug("缓存命中: {}", key);
            return parseJson(cachedValue, type);
        }

        // 2. 尝试加载数据（只允许一个请求加载）
        return loadWithSingleFlight(cacheKey, key, type, ttl, loader);
    }

    /**
     * SingleFlight 模式加载数据
     */
    private <T> T loadWithSingleFlight(String cacheKey, String key, Class<T> type,
                                       java.time.Duration ttl, Supplier<T> loader) {
        // 这里可以使用基于 Redis 的分布式锁实现
        // 简化为使用缓存空值标记
        String loadingKey = "loading:" + cacheKey;

        // 检查是否已有请求在加载
        if (Boolean.TRUE.equals(redisTemplate.hasKey(loadingKey))) {
            log.debug("已有请求正在加载，等待: {}", key);
            // 等待其他请求完成
            return waitForData(cacheKey, type, 3000); // 等待3秒
        }

        // 设置加载标记
        redisTemplate.opsForValue().set(loadingKey, "1", java.time.Duration.ofSeconds(30));

        try {
            // 加载数据
            T value = loader.get();

            if (value != null) {
                String jsonValue = toJson(value);
                // 写入缓存
                redisTemplate.opsForValue().set(cacheKey, jsonValue, ttl);
                log.debug("数据已缓存: {}", key);
            }

            return value;

        } finally {
            // 清除加载标记
            redisTemplate.delete(loadingKey);
        }
    }

    /**
     * 等待数据加载完成
     */
    private <T> T waitForData(String cacheKey, Class<T> type, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Thread.sleep(100);
                String value = redisTemplate.opsForValue().get(cacheKey);
                if (value != null && !"NULL".equals(value)) {
                    return parseJson(value, type);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    // ========================================
    // 辅助方法
    // ========================================

    private String generateLockValue() {
        return "lock_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    private <T> String toJson(T value) {
        // 这里应该使用 Jackson 或 Gson 序列化
        // 简化示例
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T parseJson(String json, Class<T> type) {
        // 这里应该使用 Jackson 或 Gson 反序列化
        // 简化示例
        return (T) json;
    }
}
