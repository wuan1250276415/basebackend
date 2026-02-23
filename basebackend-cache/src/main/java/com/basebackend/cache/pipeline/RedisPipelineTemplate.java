package com.basebackend.cache.pipeline;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.exception.CacheConnectionException;
import com.basebackend.cache.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis Pipeline 批量操作模板
 * 提供流式 API 构建批量操作，通过 RedisTemplate.executePipelined 一次性执行
 */
@Slf4j
@Component
public class RedisPipelineTemplate {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisService redisService;
    private final CacheProperties cacheProperties;

    public RedisPipelineTemplate(
            RedisTemplate<String, Object> redisTemplate,
            RedisService redisService,
            CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.redisService = redisService;
        this.cacheProperties = cacheProperties;
    }

    /**
     * 创建新的 Pipeline 构建器
     */
    public PipelineBuilder pipeline() {
        return new PipelineBuilder();
    }

    /**
     * 流式 Pipeline 构建器
     */
    public class PipelineBuilder {

        private final List<PipelineOperation> operations = new ArrayList<>();

        public PipelineBuilder get(String key) {
            operations.add(PipelineOperation.get(key));
            return this;
        }

        public PipelineBuilder set(String key, Object value) {
            operations.add(PipelineOperation.set(key, value));
            return this;
        }

        public PipelineBuilder set(String key, Object value, Duration ttl) {
            operations.add(PipelineOperation.set(key, value, ttl));
            return this;
        }

        public PipelineBuilder delete(String key) {
            operations.add(PipelineOperation.delete(key));
            return this;
        }

        public PipelineBuilder expire(String key, Duration ttl) {
            operations.add(PipelineOperation.expire(key, ttl));
            return this;
        }

        public PipelineBuilder exists(String key) {
            operations.add(PipelineOperation.exists(key));
            return this;
        }

        public PipelineBuilder incr(String key) {
            operations.add(PipelineOperation.incr(key));
            return this;
        }

        /**
         * 执行 Pipeline 中的所有操作
         *
         * @return 按顺序排列的操作结果
         * @throws IllegalStateException    如果操作数超过 maxBatchSize
         * @throws CacheConnectionException 如果 Redis 熔断器处于 OPEN 状态
         */
        public PipelineResult execute() {
            int maxBatchSize = cacheProperties.getPipeline().getMaxBatchSize();
            if (operations.size() > maxBatchSize) {
                throw new IllegalStateException(
                        "Pipeline operations (" + operations.size() + ") exceed max batch size (" + maxBatchSize + ")");
            }

            if (!redisService.isRedisAvailable()) {
                throw new CacheConnectionException("Redis circuit breaker is OPEN, pipeline execution rejected");
            }

            if (operations.isEmpty()) {
                return new PipelineResult(List.of());
            }

            log.debug("Executing pipeline with {} operations", operations.size());

            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (PipelineOperation op : operations) {
                    byte[] keyBytes = redisTemplate.getStringSerializer().serialize(op.getKey());
                    switch (op.getType()) {
                        case GET -> connection.stringCommands().get(keyBytes);
                        case SET -> {
                            byte[] valueBytes = serializeValue(op.getValue());
                            if (op.getTtl() != null) {
                                connection.stringCommands().setEx(keyBytes, op.getTtl().getSeconds(), valueBytes);
                            } else {
                                connection.stringCommands().set(keyBytes, valueBytes);
                            }
                        }
                        case DELETE -> connection.keyCommands().del(keyBytes);
                        case EXPIRE -> connection.keyCommands().expire(keyBytes, op.getTtl().getSeconds());
                        case EXISTS -> connection.keyCommands().exists(keyBytes);
                        case INCR -> connection.stringCommands().incr(keyBytes);
                    }
                }
                return null;
            });

            return new PipelineResult(results != null ? results : List.of());
        }

        private byte[] serializeValue(Object value) {
            if (value == null) {
                return new byte[0];
            }
            if (value instanceof byte[]) {
                return (byte[]) value;
            }
            return redisService.getSerializer().serialize(value);
        }
    }
}
