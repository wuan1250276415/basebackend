package com.basebackend.scheduler.idempotent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 任务幂等性管理器
 * 基于Redis分布式锁实现任务幂等性保障
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobIdempotencyManager {

    private final RedissonClient redissonClient;
    private static final String IDEMPOTENT_KEY_PREFIX = "job:idempotent:";
    private static final long LOCK_TIMEOUT = 300; // 锁超时时间5分钟

    /**
     * 检查任务是否已执行过
     *
     * @param jobId 任务ID
     * @param instanceId 实例ID
     * @return true表示已执行过，false表示未执行
     */
    public boolean isDuplicate(Long jobId, Long instanceId) {
        String key = buildKey(jobId, instanceId);
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 标记任务已执行
     *
     * @param jobId 任务ID
     * @param instanceId 实例ID
     * @param ttl 过期时间(秒)
     */
    public void markExecuted(Long jobId, Long instanceId, long ttl) {
        String key = buildKey(jobId, instanceId);
        redissonClient.getBucket(key).set("executed", ttl, TimeUnit.SECONDS);
        log.debug("标记任务已执行: jobId={}, instanceId={}", jobId, instanceId);
    }

    /**
     * 尝试获取任务执行锁
     *
     * @param jobId 任务ID
     * @param instanceId 实例ID
     * @return 获取成功返回锁对象，失败返回null
     */
    public RLock tryAcquireLock(Long jobId, Long instanceId) {
        String lockKey = buildKey(jobId, instanceId) + ":lock";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(0, LOCK_TIMEOUT, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("获取任务执行锁成功: jobId={}, instanceId={}", jobId, instanceId);
                return lock;
            } else {
                log.warn("获取任务执行锁失败，任务可能正在执行: jobId={}, instanceId={}", jobId, instanceId);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取任务执行锁被中断", e);
            return null;
        }
    }

    /**
     * 释放任务执行锁
     *
     * @param lock 锁对象
     */
    public void releaseLock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放任务执行锁");
        }
    }

    /**
     * 清除任务执行标记
     *
     * @param jobId 任务ID
     * @param instanceId 实例ID
     */
    public void clear(Long jobId, Long instanceId) {
        String key = buildKey(jobId, instanceId);
        redissonClient.getBucket(key).delete();
        log.debug("清除任务执行标记: jobId={}, instanceId={}", jobId, instanceId);
    }

    /**
     * 构建幂等性键
     */
    private String buildKey(Long jobId, Long instanceId) {
        return IDEMPOTENT_KEY_PREFIX + jobId + ":" + instanceId;
    }
}
