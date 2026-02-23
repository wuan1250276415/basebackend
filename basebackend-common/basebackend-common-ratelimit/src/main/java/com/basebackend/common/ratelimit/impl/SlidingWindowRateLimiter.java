package com.basebackend.common.ratelimit.impl;

import com.basebackend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 滑动窗口限流器
 * <p>
 * 基于内存的滑动窗口限流实现，支持自动清理过期条目和内存上限保护。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final int maxKeys;
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * 使用默认配置创建限流器（最大10000个key，每5分钟自动清理）
     */
    public SlidingWindowRateLimiter() {
        this(10000, 5);
    }

    /**
     * 创建限流器
     *
     * @param maxKeys                最大key数量，超出时清理空窗口
     * @param cleanupIntervalMinutes 自动清理间隔（分钟）
     */
    public SlidingWindowRateLimiter(int maxKeys, int cleanupIntervalMinutes) {
        this.maxKeys = maxKeys;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ratelimit-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
        );
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        Deque<Long> timestamps = windows.computeIfAbsent(key, k -> new LinkedList<>());

        synchronized (timestamps) {
            Iterator<Long> it = timestamps.iterator();
            while (it.hasNext()) {
                if (it.next() <= windowStart) {
                    it.remove();
                } else {
                    break;
                }
            }

            if (timestamps.size() >= limit) {
                return false;
            }

            timestamps.addLast(now);
        }

        // 超过最大key数量时触发异步清理
        if (windows.size() > maxKeys) {
            cleanup();
        }

        return true;
    }

    /**
     * 清理空窗口条目
     * <p>
     * 移除所有时间戳队列为空的key，释放内存。
     * 如果清理后仍超过最大key数量限制，按最早访问时间淘汰。
     * </p>
     */
    public void cleanup() {
        int before = windows.size();

        // 移除空窗口
        windows.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().isEmpty();
            }
        });

        int after = windows.size();
        if (before != after) {
            log.debug("RateLimiter cleanup: removed {} empty entries, {} remaining", before - after, after);
        }

        // 如果仍超过上限，淘汰最早访问的key
        if (windows.size() > maxKeys) {
            int toEvict = windows.size() - maxKeys;
            windows.entrySet().stream()
                    .sorted((a, b) -> {
                        Long aFirst, bFirst;
                        synchronized (a.getValue()) {
                            aFirst = a.getValue().peekFirst();
                        }
                        synchronized (b.getValue()) {
                            bFirst = b.getValue().peekFirst();
                        }
                        if (aFirst == null) return -1;
                        if (bFirst == null) return 1;
                        return Long.compare(aFirst, bFirst);
                    })
                    .limit(toEvict)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(windows::remove);
            log.info("RateLimiter eviction: removed {} oldest entries to stay within maxKeys={}", toEvict, maxKeys);
        }
    }

    /**
     * 关闭清理线程
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }

    /**
     * 获取当前追踪的key数量（用于监控）
     *
     * @return 当前key数量
     */
    public int getKeyCount() {
        return windows.size();
    }
}
