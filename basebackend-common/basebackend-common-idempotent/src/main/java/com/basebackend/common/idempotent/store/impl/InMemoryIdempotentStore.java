package com.basebackend.common.idempotent.store.impl;

import com.basebackend.common.idempotent.store.IdempotentStore;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存的幂等存储实现（单机降级方案）
 * <p>
 * 使用 ConcurrentHashMap 存储幂等 key，通过 ScheduledExecutor 定时清理过期条目。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class InMemoryIdempotentStore implements IdempotentStore {

    private final ConcurrentHashMap<String, Long> store;

    private final ScheduledExecutorService scheduler;

    public InMemoryIdempotentStore() {
        this(new ConcurrentHashMap<>());
    }

    InMemoryIdempotentStore(ConcurrentHashMap<String, Long> store) {
        this.store = store;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "idempotent-cleanup");
            t.setDaemon(true);
            return t;
        });
        // 每 30 秒清理一次过期条目
        this.scheduler.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public boolean tryAcquire(String key, long timeout, TimeUnit unit) {
        long expireAt = System.currentTimeMillis() + unit.toMillis(timeout);
        while (true) {
            Long existing = store.putIfAbsent(key, expireAt);
            if (existing == null) {
                return true;
            }
            if (System.currentTimeMillis() <= existing) {
                return false;
            }
            // 过期键使用 CAS 替换，失败说明被并发更新，进入下一轮重试
            if (store.replace(key, existing, expireAt)) {
                return true;
            }
        }
    }

    @Override
    public void release(String key) {
        store.remove(key);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now > entry.getValue());
    }
}
