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

    private final CacheProperties cacheProperties;
    private final MeterRegistry meterRegistry;

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
        enforceMemoryBound();
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
    private void enforceMemoryBound() {
        int maxSize = cacheProperties.getHotKey().getTopK() * 10;
        if (currentWindow.size() > maxSize) {
            // 移除计数最小的条目
            List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(currentWindow.entrySet());
            entries.sort(Comparator.comparingLong(e -> e.getValue().sum()));

            int removeCount = currentWindow.size() - maxSize;
            for (int i = 0; i < removeCount && i < entries.size(); i++) {
                currentWindow.remove(entries.get(i).getKey());
            }
        }
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
