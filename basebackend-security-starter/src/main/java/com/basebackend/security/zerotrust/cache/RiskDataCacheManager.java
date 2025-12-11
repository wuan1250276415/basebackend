package com.basebackend.security.zerotrust.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 风险数据缓存管理
 * <p>
 * 解决 RiskAssessmentEngine 中的内存泄漏问题：
 * - 添加事件过期机制
 * - 添加最大容量限制
 * - 定时清理过期数据
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class RiskDataCacheManager {

    @Value("${basebackend.security.zerotrust.cache.risk-events-max-size:10000}")
    private int riskEventsMaxSize;

    @Value("${basebackend.security.zerotrust.cache.risk-events-expire-hours:24}")
    private int riskEventsExpireHours;

    @Value("${basebackend.security.zerotrust.cache.user-profiles-max-size:50000}")
    private int userProfilesMaxSize;

    @Value("${basebackend.security.zerotrust.cache.user-profiles-expire-hours:168}")
    private int userProfilesExpireHours;

    @Value("${basebackend.security.zerotrust.cache.cleanup-interval-minutes:30}")
    private int cleanupIntervalMinutes;

    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        // 启动定时清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "risk-cache-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        log.info("RiskDataCacheManager initialized: riskEventsMaxSize={}, riskEventsExpireHours={}, " +
                "userProfilesMaxSize={}, userProfilesExpireHours={}",
                riskEventsMaxSize, riskEventsExpireHours, userProfilesMaxSize, userProfilesExpireHours);
    }

    @PreDestroy
    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 创建带过期功能的缓存
     *
     * @param maxSize     最大容量
     * @param expireHours 过期时间（小时）
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 缓存包装器
     */
    public <K, V> ExpiringCache<K, V> createCache(int maxSize, int expireHours) {
        ExpiringCache<K, V> cache = new ExpiringCache<>(maxSize, expireHours);

        // 定期清理
        cleanupExecutor.scheduleAtFixedRate(
                cache::cleanup,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES);

        return cache;
    }

    /**
     * 创建风险事件缓存
     */
    public <K, V> ExpiringCache<K, V> createRiskEventsCache() {
        return createCache(riskEventsMaxSize, riskEventsExpireHours);
    }

    /**
     * 创建用户风险档案缓存
     */
    public <K, V> ExpiringCache<K, V> createUserProfilesCache() {
        return createCache(userProfilesMaxSize, userProfilesExpireHours);
    }

    /**
     * 带过期功能的缓存实现
     */
    public static class ExpiringCache<K, V> {
        private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
        private final int maxSize;
        private final long expireMillis;

        public ExpiringCache(int maxSize, int expireHours) {
            this.maxSize = maxSize;
            this.expireMillis = expireHours * 60L * 60L * 1000L;
        }

        public void put(K key, V value) {
            // 如果是新key且容量已满，先移除最老的条目
            if (!cache.containsKey(key) && cache.size() >= maxSize) {
                removeOldest();
            }
            cache.put(key, new CacheEntry<>(value, Instant.now()));
        }

        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (isExpired(entry)) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        }

        public void remove(K key) {
            cache.remove(key);
        }

        public int size() {
            return cache.size();
        }

        public void clear() {
            cache.clear();
        }

        /**
         * 清理过期条目
         */
        public void cleanup() {
            int removed = 0;
            Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<K, CacheEntry<V>> entry = it.next();
                if (isExpired(entry.getValue())) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                log.debug("Cache cleanup: removed {} expired entries, remaining: {}", removed, cache.size());
            }
        }

        private boolean isExpired(CacheEntry<V> entry) {
            return Instant.now().toEpochMilli() - entry.createdAt.toEpochMilli() > expireMillis;
        }

        private void removeOldest() {
            Instant oldest = Instant.now();
            K oldestKey = null;

            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                if (entry.getValue().createdAt.isBefore(oldest)) {
                    oldest = entry.getValue().createdAt;
                    oldestKey = entry.getKey();
                }
            }

            if (oldestKey != null) {
                cache.remove(oldestKey);
                log.debug("Cache eviction: removed oldest entry due to size limit");
            }
        }

        private static class CacheEntry<V> {
            final V value;
            final Instant createdAt;

            CacheEntry(V value, Instant createdAt) {
                this.value = value;
                this.createdAt = createdAt;
            }
        }
    }
}
