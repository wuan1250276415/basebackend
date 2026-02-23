package com.basebackend.common.idempotent.store.impl;

import com.basebackend.common.idempotent.store.IdempotentStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的幂等存储实现
 * <p>
 * 使用 SET NX EX 原子操作实现幂等性检查。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class RedisIdempotentStore implements IdempotentStore {

    private static final String IDEMPOTENT_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotentStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, IDEMPOTENT_VALUE, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(key);
    }
}
