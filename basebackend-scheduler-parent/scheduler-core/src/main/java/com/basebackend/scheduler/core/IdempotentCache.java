package com.basebackend.scheduler.core;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 线程安全的幂等缓存，支持 TTL 与 LRU 淘汰策略。
 */
@Slf4j
public class IdempotentCache<V> implements AutoCloseable {

    private final ConcurrentMap<String, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> accessOrder = new ConcurrentLinkedDeque<>();
    private final int capacity;
    private final Duration ttl;
    private final ScheduledExecutorService cleaner;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public IdempotentCache(Duration ttl, int capacity) {
        this.ttl = ttl != null ? ttl : Duration.ZERO;
        this.capacity = Math.max(1, capacity);
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "idempotent-cache-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        long intervalMs = Math.max(1000L, Math.min(effectiveTtlMillis(), 60_000L));
        cleaner.scheduleAtFixedRate(this::cleanUp, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public Optional<V> get(String key) {
        if (key == null) {
            return Optional.empty();
        }
        CacheEntry<V> entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            remove(key);
            return Optional.empty();
        }
        touch(key);
        return Optional.of(entry.value);
    }

    public void put(String key, V value) {
        if (key == null || value == null) {
            return;
        }
        store.put(key, new CacheEntry<>(value, System.currentTimeMillis() + effectiveTtlMillis()));
        touch(key);
        cleanUp();
    }

    public boolean remove(String key) {
        if (key == null) {
            return false;
        }
        boolean removed = store.remove(key) != null;
        if (removed) {
            accessOrder.remove(key);
        }
        return removed;
    }

    public int size() {
        return store.size();
    }

    /**
     * 清理过期或超出容量的缓存。
     */
    public void cleanUp() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        evictOverflow();
    }

    private void evictOverflow() {
        while (store.size() > capacity) {
            String eldest = accessOrder.pollFirst();
            if (eldest == null) {
                break;
            }
            store.remove(eldest);
        }
    }

    private void touch(String key) {
        accessOrder.remove(key);
        accessOrder.addLast(key);
    }

    private long effectiveTtlMillis() {
        if (ttl.isZero() || ttl.isNegative()) {
            return TimeUnit.MINUTES.toMillis(30);
        }
        return ttl.toMillis();
    }

    @PreDestroy
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cleaner.shutdown();
            try {
                cleaner.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            store.clear();
            accessOrder.clear();
        }
    }

    private static final class CacheEntry<V> {
        private final V value;
        private final long expireAt;

        CacheEntry(V value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long now) {
            return now >= expireAt;
        }
    }
}
