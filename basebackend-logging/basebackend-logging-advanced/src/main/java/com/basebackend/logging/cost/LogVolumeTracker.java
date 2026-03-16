package com.basebackend.logging.cost;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志量跟踪器
 *
 * 按服务名（或租户 ID）统计每个时间窗口内的日志事件数和字节数。
 * 使用滑动窗口机制，窗口到期后自动重置计数器。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
public class LogVolumeTracker {

    private final int windowSeconds;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public LogVolumeTracker(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    /**
     * 记录一条日志事件
     *
     * @param key       服务名或租户 ID
     * @param byteSize  该日志事件的预估字节数
     */
    public void record(String key, int byteSize) {
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(windowSeconds));
        counter.record(byteSize);
    }

    /**
     * 获取指定 key 当前窗口内的事件计数
     */
    public long getEventCount(String key) {
        WindowCounter counter = counters.get(key);
        return counter != null ? counter.getEventCount() : 0;
    }

    /**
     * 获取指定 key 当前窗口内的字节计数
     */
    public long getByteCount(String key) {
        WindowCounter counter = counters.get(key);
        return counter != null ? counter.getByteCount() : 0;
    }

    /**
     * 获取所有 key 的当前窗口快照
     */
    public Map<String, VolumeSnapshot> getAllSnapshots() {
        Map<String, VolumeSnapshot> result = new LinkedHashMap<>();
        counters.forEach((key, counter) -> {
            counter.rollIfExpired();
            result.put(key, new VolumeSnapshot(
                    counter.getEventCount(),
                    counter.getByteCount(),
                    counter.getTotalEvents(),
                    counter.getTotalBytes()));
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 窗口计数器：在每个时间窗口内累加，窗口到期自动重置
     */
    static class WindowCounter {
        private final int windowSeconds;
        private volatile long windowStart;
        private final AtomicLong windowEvents = new AtomicLong();
        private final AtomicLong windowBytes = new AtomicLong();
        private final AtomicLong totalEvents = new AtomicLong();
        private final AtomicLong totalBytes = new AtomicLong();

        WindowCounter(int windowSeconds) {
            this.windowSeconds = windowSeconds;
            this.windowStart = Instant.now().getEpochSecond();
        }

        void record(int byteSize) {
            rollIfExpired();
            windowEvents.incrementAndGet();
            windowBytes.addAndGet(byteSize);
            totalEvents.incrementAndGet();
            totalBytes.addAndGet(byteSize);
        }

        long getEventCount() {
            rollIfExpired();
            return windowEvents.get();
        }

        long getByteCount() {
            rollIfExpired();
            return windowBytes.get();
        }

        long getTotalEvents() {
            return totalEvents.get();
        }

        long getTotalBytes() {
            return totalBytes.get();
        }

        /**
         * 若当前时间窗口已过期则重置，使用 synchronized 保证 check-then-act 的原子性，
         * 防止并发调用下同一窗口被多次重置导致计数丢失。
         */
        synchronized void rollIfExpired() {
            long now = Instant.now().getEpochSecond();
            if (now - windowStart >= windowSeconds) {
                windowEvents.set(0);
                windowBytes.set(0);
                windowStart = now;
            }
        }
    }

    /**
     * 卷快照
     */
    public record VolumeSnapshot(long windowEvents, long windowBytes,
                                  long totalEvents, long totalBytes) {
    }
}
