package com.basebackend.messaging.idempotency;

import com.basebackend.messaging.config.MessagingProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 幂等性服务
 * 基于Redis实现消息去重
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "messaging.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyService {

    private final RedissonClient redissonClient;
    private final MessagingProperties properties;

    public IdempotencyService(RedissonClient redissonClient, MessagingProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    /**
     * 检查消息是否已处理（幂等性检查）
     *
     * @param messageId 消息ID
     * @return true-已处理，false-未处理
     */
    public boolean isDuplicate(String messageId) {
        String key = properties.getIdempotency().getKeyPrefix() + messageId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    /**
     * 标记消息已处理
     *
     * @param messageId 消息ID
     */
    public void markAsProcessed(String messageId) {
        String key = properties.getIdempotency().getKeyPrefix() + messageId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set("1", Duration.ofSeconds(properties.getIdempotency().getExpireTime()));
        log.debug("Message marked as processed: messageId={}", messageId);
    }

    /**
     * 尝试标记消息为处理中（防止并发处理）
     *
     * @param messageId 消息ID
     * @return true-成功，false-失败（已有其他线程在处理）
     */
    public boolean tryLock(String messageId) {
        String key = properties.getIdempotency().getKeyPrefix() + messageId + ":lock";
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent("1", Duration.ofSeconds(60));
    }

    /**
     * 释放消息处理锁
     *
     * @param messageId 消息ID
     */
    public void unlock(String messageId) {
        String key = properties.getIdempotency().getKeyPrefix() + messageId + ":lock";
        redissonClient.getBucket(key).delete();
    }
}
