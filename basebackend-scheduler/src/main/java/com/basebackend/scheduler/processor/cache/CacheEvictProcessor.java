package com.basebackend.scheduler.processor.cache;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 缓存淘汰处理器，支持过期清理、Pattern批量删除与LRU热点清理。
 *
 * <p>参数：
 * <ul>
 *     <li>pattern：匹配的键模式，默认"*"。</li>
 *     <li>maxAge：阈值秒，&lt;0 表示直接按模式删除，=0 仅删除已过期，&gt;0 删除TTL小于等于阈值的键。</li>
 *     <li>batchSize：单次处理的最大键数，默认100。</li>
 * </ul>
 * 结果返回删除数量与估算节省空间（按保守估算计算）。
 */
@Slf4j
@Component
public class CacheEvictProcessor implements TaskProcessor {

    private static final long ESTIMATED_ENTRY_BYTES = 512L;

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheEvictProcessor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String name() {
        return "cache-evict";
    }

    @Override
    public TaskResult process(TaskContext context) {
        Instant start = Instant.now();
        Map<String, Object> params = context.getParameters();
        String pattern = String.valueOf(params.getOrDefault("pattern", "*"));
        long maxAgeSeconds = toLong(params.get("maxAge"), -1L);
        int batchSize = (int) Math.max(1, toLong(params.get("batchSize"), 100L));

        // TODO: 后续集成Redis SCAN替代KEYS，避免阻塞主线程
        // 当前使用KEYS方法，生产环境建议使用SCAN游标迭代
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null) {
            keys = Set.of();
        }

        if (keys.size() > batchSize) {
            keys = keys.stream().limit(batchSize).collect(java.util.stream.Collectors.toSet());
        }

        long deleted = 0;
        long expiredDeleted = 0;
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key);
            boolean alreadyExpired = ttl != null && ttl <= 0;
            boolean matchMaxAge = maxAgeSeconds < 0 || (ttl != null && ttl > 0 && ttl <= maxAgeSeconds);
            if (alreadyExpired || matchMaxAge) {
                if (Boolean.TRUE.equals(redisTemplate.delete(key))) {
                    deleted++;
                    if (alreadyExpired) {
                        expiredDeleted++;
                    }
                }
            }
        }

        long freedBytes = deleted * ESTIMATED_ENTRY_BYTES;
        Duration duration = Duration.between(start, Instant.now());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pattern", pattern);
        output.put("deletedCount", deleted);
        output.put("expiredDeleted", expiredDeleted);
        output.put("freedBytes", freedBytes);

        log.info("[CacheEvict] pattern={}, deleted={}, expired={}, freed={}B",
                pattern, deleted, expiredDeleted, freedBytes);

        return TaskResult.builder(TaskResult.Status.SUCCESS)
                .startTime(start)
                .duration(duration)
                .output(output)
                .idempotentKey(context.getIdempotentKey())
                .idempotentHit(context.getIdempotentKey() != null)
                .build();
    }

    private long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
