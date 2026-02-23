package com.basebackend.cache.refresh;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.lock.DistributedLockService;
import com.basebackend.cache.service.RedisService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * 近过期缓存异步刷新管理器
 * 当缓存命中但剩余 TTL 低于阈值时，异步重新加载数据并刷新缓存，
 * 实现 stale-while-revalidate 模式，避免缓存击穿
 */
@Slf4j
public class NearExpiryRefreshManager {

    private final RedisService redisService;
    private final DistributedLockService distributedLockService;
    private final CacheProperties cacheProperties;
    private final ExecutorService refreshExecutor;
    private final MeterRegistry meterRegistry;

    /**
     * 去重标记：防止同一个 key 同时发起多次刷新
     */
    private final ConcurrentHashMap<String, Boolean> refreshInProgress = new ConcurrentHashMap<>();

    public NearExpiryRefreshManager(
            RedisService redisService,
            DistributedLockService distributedLockService,
            CacheProperties cacheProperties,
            ExecutorService refreshExecutor,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.redisService = redisService;
        this.distributedLockService = distributedLockService;
        this.cacheProperties = cacheProperties;
        this.refreshExecutor = refreshExecutor;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 检查缓存是否接近过期，如果是则提交异步刷新任务
     *
     * @param key         缓存键
     * @param originalTtl 注解上声明的原始 TTL（秒）
     * @param cacheName   缓存名称（用于指标）
     * @param joinPoint   原始方法调用点（用于重新执行方法获取新值）
     */
    public void checkAndRefresh(String key, long originalTtl, String cacheName, ProceedingJoinPoint joinPoint) {
        CacheProperties.Refresh config = cacheProperties.getRefresh();
        if (!config.isEnabled()) {
            return;
        }

        try {
            long remainingTtl = redisService.getExpire(key);
            // remainingTtl < 0 表示无 TTL 或 key 不存在
            if (remainingTtl < 0 || originalTtl <= 0) {
                return;
            }

            double ratio = (double) remainingTtl / originalTtl;
            if (ratio <= config.getThresholdRatio()) {
                submitRefresh(key, originalTtl, cacheName, joinPoint);
            }
        } catch (Exception e) {
            log.debug("Near-expiry refresh check failed for key: {}", key, e);
        }
    }

    /**
     * 提交异步刷新任务
     */
    private void submitRefresh(String key, long originalTtl, String cacheName, ProceedingJoinPoint joinPoint) {
        // 去重：同一个 key 只允许一个刷新任务在执行
        if (refreshInProgress.putIfAbsent(key, Boolean.TRUE) != null) {
            recordMetric(cacheName, "skipped");
            return;
        }

        recordMetric(cacheName, "triggered");

        CompletableFuture.runAsync(() -> {
            CacheProperties.Refresh config = cacheProperties.getRefresh();
            String lockKey = "refresh:" + key;
            try {
                long lockWait = config.getLockWaitTime().toSeconds();
                long lockLease = config.getLockLeaseTime().toSeconds();

                if (!distributedLockService.tryLock(lockKey, lockWait, lockLease, TimeUnit.SECONDS)) {
                    recordMetric(cacheName, "skipped");
                    return;
                }

                try {
                    Object newValue = joinPoint.proceed();
                    if (newValue != null) {
                        redisService.set(key, newValue, originalTtl, TimeUnit.SECONDS);
                        log.debug("Near-expiry refresh completed: key={}, ttl={}s", key, originalTtl);
                        recordMetric(cacheName, "success");
                    }
                } finally {
                    distributedLockService.unlock(lockKey);
                }
            } catch (Throwable e) {
                log.warn("Near-expiry refresh failed for key: {}", key, e);
                recordMetric(cacheName, "failure");
            } finally {
                refreshInProgress.remove(key);
            }
        }, refreshExecutor);
    }

    private void recordMetric(String cacheName, String result) {
        if (meterRegistry == null) {
            return;
        }
        String metricName = "triggered".equals(result) ? "cache.refresh.triggered" : "cache.refresh.completed";
        Counter.Builder builder = Counter.builder(metricName).tag("cacheName", cacheName);
        if (!"triggered".equals(result)) {
            builder.tag("result", result);
        }
        builder.register(meterRegistry).increment();
    }
}
