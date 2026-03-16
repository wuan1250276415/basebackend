package com.basebackend.cache.hotkey;

import com.basebackend.cache.config.CacheProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * 热点 Key 检测器
 * 基于双滑动窗口计数器实现热点 Key 识别
 *
 * 算法:
 * - 维护 current 和 previous 两个窗口的访问计数
 * - 每隔 windowSize 轮转：swap current <-> previous，清空新的 current
 * - 判定热点：currentCount + previousCount > threshold
 * - 内存限制：计数 Map 大小不超过 topK * 10
 */
@Slf4j
public class HotKeyDetector {

    /**
     * 回收检查步长，避免每次访问都做昂贵回收
     */
    private static final int TRIM_CHECK_INTERVAL = 64;

    /**
     * 强制回收阈值倍率，窗口超过该倍率时立即回收到上限
     */
    private static final int FORCE_TRIM_RATIO = 2;

    /**
     * 单次常规回收上限
     */
    private static final int MAX_TRIM_BATCH = 128;

    private final CacheProperties cacheProperties;
    private final MeterRegistry meterRegistry;
    private final LongAdder accessCounter = new LongAdder();

    private volatile ConcurrentHashMap<String, LongAdder> currentWindow = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, LongAdder> previousWindow = new ConcurrentHashMap<>();

    /**
     * Top-K 排行榜
     */
    private final ConcurrentSkipListSet<HotKeyStats> topKSet = new ConcurrentSkipListSet<>();

    public HotKeyDetector(
            CacheProperties cacheProperties,
            ScheduledExecutorService scheduler,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.cacheProperties = cacheProperties;
        this.meterRegistry = meterRegistry;

        CacheProperties.HotKey config = cacheProperties.getHotKey();
        long windowMs = config.getWindowSize().toMillis();
        scheduler.scheduleAtFixedRate(this::rotateWindow, windowMs, windowMs, TimeUnit.MILLISECONDS);

        registerGauges();
    }

    /**
     * 记录一次访问
     */
    public void recordAccess(String key) {
        currentWindow.computeIfAbsent(key, k -> new LongAdder()).increment();
        maybeEnforceMemoryBound();
    }

    /**
     * 判断指定 key 是否为热点
     */
    public boolean isHot(String key) {
        long count = getAccessCount(key);
        return count >= cacheProperties.getHotKey().getThreshold();
    }

    /**
     * 获取指定 key 的当前访问计数（current + previous）
     */
    public long getAccessCount(String key) {
        long current = getCount(currentWindow, key);
        long previous = getCount(previousWindow, key);
        return current + previous;
    }

    /**
     * 获取当前热点 Key 数量
     */
    public int getHotKeyCount() {
        return (int) topKSet.stream().filter(HotKeyStats::isHot).count();
    }

    /**
     * 获取 Top-K 统计列表
     */
    public List<HotKeyStats> getTopK() {
        return new ArrayList<>(topKSet);
    }

    /**
     * 窗口轮转
     */
    private void rotateWindow() {
        ConcurrentHashMap<String, LongAdder> oldCurrent = currentWindow;
        currentWindow = new ConcurrentHashMap<>();
        previousWindow = oldCurrent;

        rebuildTopK();
        log.debug("Hot key detection window rotated, tracking {} keys", previousWindow.size());
    }

    /**
     * 重建 Top-K 排行榜
     */
    private void rebuildTopK() {
        CacheProperties.HotKey config = cacheProperties.getHotKey();
        long threshold = config.getThreshold();
        int topK = config.getTopK();

        // 合并两个窗口的所有 key
        Set<String> allKeys = new HashSet<>(currentWindow.keySet());
        allKeys.addAll(previousWindow.keySet());

        List<HotKeyStats> candidates = allKeys.stream()
                .map(key -> {
                    long count = getAccessCount(key);
                    return new HotKeyStats(key, count, count >= threshold);
                })
                .sorted()
                .limit(topK)
                .collect(Collectors.toList());

        topKSet.clear();
        topKSet.addAll(candidates);
    }

    /**
     * 内存限制：计数 Map 大小不超过 topK * 10
     */
    private void maybeEnforceMemoryBound() {
        int maxSize = cacheProperties.getHotKey().getTopK() * 10;
        int currentSize = currentWindow.size();
        if (currentSize <= maxSize) {
            return;
        }

        boolean forceTrim = maxSize <= 0 || currentSize > maxSize * FORCE_TRIM_RATIO;
        if (!forceTrim) {
            accessCounter.increment();
            if (accessCounter.sum() % TRIM_CHECK_INTERVAL != 0) {
                return;
            }
        }

        enforceMemoryBound(maxSize, currentSize, forceTrim);
    }

    /**
     * 内存限制：计数 Map 大小不超过 topK * 10
     * 算法：使用固定大小最大堆选出“计数最小”的 removeCount 个 key，复杂度 O(n log k)
     */
    private void enforceMemoryBound(int maxSize, int currentSize, boolean forceTrim) {
        if (maxSize <= 0) {
            currentWindow.clear();
            return;
        }

        int overflow = currentSize - maxSize;
        if (overflow <= 0) {
            return;
        }

        int removeCount = forceTrim
                ? overflow
                : Math.min(overflow, Math.max(16, Math.min(MAX_TRIM_BATCH, maxSize / 4)));

        PriorityQueue<KeyCount> smallestKeys = new PriorityQueue<>(Comparator.comparingLong(KeyCount::count).reversed());
        for (Map.Entry<String, LongAdder> entry : currentWindow.entrySet()) {
            long count = entry.getValue().sum();
            if (smallestKeys.size() < removeCount) {
                smallestKeys.offer(new KeyCount(entry.getKey(), count));
                continue;
            }

            KeyCount currentMaxInSmallest = smallestKeys.peek();
            if (currentMaxInSmallest != null && count < currentMaxInSmallest.count()) {
                smallestKeys.poll();
                smallestKeys.offer(new KeyCount(entry.getKey(), count));
            }
        }

        while (!smallestKeys.isEmpty()) {
            currentWindow.remove(smallestKeys.poll().key());
        }
    }

    private record KeyCount(String key, long count) {
    }

    private long getCount(ConcurrentHashMap<String, LongAdder> window, String key) {
        LongAdder adder = window.get(key);
        return adder != null ? adder.sum() : 0;
    }

    private void registerGauges() {
        if (meterRegistry == null) {
            return;
        }
        Gauge.builder("cache.hotkey.detected", this, HotKeyDetector::getHotKeyCount)
                .register(meterRegistry);
    }
}
