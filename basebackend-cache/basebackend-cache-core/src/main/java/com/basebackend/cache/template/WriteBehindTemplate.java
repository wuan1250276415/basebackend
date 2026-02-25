package com.basebackend.cache.template;

import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.metrics.CacheMetricsService;
import com.basebackend.cache.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Write-Behind (Write-Back) 模式模板
 * 
 * 实现 Write-Behind 缓存模式：
 * 1. 写操作立即更新缓存，异步批量更新数据源
 * 2. 提高写性能，降低数据源压力
 * 3. 定期或达到批量大小时刷新到数据源
 * 
 * 适用场景：
 * - 写操作频繁的场景
 * - 可以接受最终一致性的场景
 * - 需要高写性能的场景
 * 
 * 注意事项：
 * - 存在数据丢失风险（系统崩溃时）
 * - 需要定期刷新确保数据持久化
 */
@Slf4j
@Component
public class WriteBehindTemplate {

    private final RedisService redisService;
    private final MultiLevelCacheManager multiLevelCacheManager; // Can be null if multi-level cache is disabled
    private final CacheMetricsService metricsService;
    
    /**
     * 待写入数据源的数据队列
     */
    private final ConcurrentHashMap<String, PendingWrite> pendingWrites = new ConcurrentHashMap<>();
    
    /**
     * 批量写入阈值
     */
    private static final int BATCH_SIZE_THRESHOLD = 100;
    
    /**
     * 数据持久化处理器
     */
    private BiConsumer<String, Object> dataPersister;
    
    /**
     * 异步执行器
     */
    private final ExecutorService executorService;

    public WriteBehindTemplate(
            RedisService redisService,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager,
            CacheMetricsService metricsService) {
        this.redisService = redisService;
        this.multiLevelCacheManager = multiLevelCacheManager;
        this.metricsService = metricsService;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("write-behind-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    /**
     * 设置数据持久化处理器
     * 
     * @param dataPersister 数据持久化函数
     */
    public void setDataPersister(BiConsumer<String, Object> dataPersister) {
        this.dataPersister = dataPersister;
    }

    /**
     * 异步写入缓存和数据源
     * 
     * @param key 缓存键
     * @param value 要写入的值
     * @param ttl 缓存过期时间
     */
    public <T> void set(String key, T value, Duration ttl) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 立即更新缓存
            log.debug("Updating cache for key: {}", key);
            setCachedValue(key, value, ttl);
            
            // 2. 将写操作加入待处理队列
            pendingWrites.put(key, new PendingWrite(key, value, System.currentTimeMillis()));
            log.debug("Added pending write for key: {}, queue size: {}", key, pendingWrites.size());
            
            // 3. 检查是否需要触发批量刷新
            if (pendingWrites.size() >= BATCH_SIZE_THRESHOLD) {
                log.info("Batch size threshold reached, triggering flush");
                flushAsync();
            }
            
            metricsService.recordLatency("write-behind-set", System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Error in write-behind set operation for key: {}", key, e);
            metricsService.recordLatency("write-behind-error", System.currentTimeMillis() - startTime);
            throw new RuntimeException("Write-behind set failed for key: " + key, e);
        }
    }

    /**
     * 异步写入缓存和数据源（使用默认 TTL）
     * 
     * @param key 缓存键
     * @param value 要写入的值
     */
    public <T> void set(String key, T value) {
        set(key, value, null);
    }

    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> type) {
        long startTime = System.currentTimeMillis();
        
        try {
            T value = getCachedValue(key, type);
            
            if (value != null) {
                metricsService.recordHit("write-behind");
            } else {
                metricsService.recordMiss("write-behind");
            }
            
            metricsService.recordLatency("write-behind-get", System.currentTimeMillis() - startTime);
            return value;
            
        } catch (Exception e) {
            log.error("Error in write-behind get operation for key: {}", key, e);
            metricsService.recordLatency("write-behind-error", System.currentTimeMillis() - startTime);
            return null;
        }
    }

    /**
     * 刷新待写入的数据到数据源（同步）
     */
    public void flush() {
        if (pendingWrites.isEmpty()) {
            log.debug("No pending writes to flush");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int flushedCount = 0;
        
        log.info("Starting flush of {} pending writes", pendingWrites.size());
        
        // 获取所有待写入数据的快照
        Map<String, PendingWrite> snapshot = new ConcurrentHashMap<>(pendingWrites);
        
        // 批量写入数据源
        for (Map.Entry<String, PendingWrite> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            PendingWrite pendingWrite = entry.getValue();
            
            try {
                if (dataPersister != null) {
                    dataPersister.accept(key, pendingWrite.getValue());
                    pendingWrites.remove(key);
                    flushedCount++;
                    log.debug("Flushed data to source for key: {}", key);
                } else {
                    log.warn("Data persister not set, skipping flush for key: {}", key);
                }
            } catch (Exception e) {
                log.error("Error flushing data to source for key: {}", key, e);
                // 保留失败的写入，下次重试
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Flush completed: {} writes flushed in {} ms, {} writes remaining", 
                flushedCount, duration, pendingWrites.size());
        
        metricsService.recordLatency("write-behind-flush", duration);
    }

    /**
     * 异步刷新待写入的数据到数据源
     */
    public void flushAsync() {
        executorService.submit(() -> {
            try {
                flush();
            } catch (Exception e) {
                log.error("Error in async flush", e);
            }
        });
    }

    /**
     * 定期刷新（每 30 秒）
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void scheduledFlush() {
        if (!pendingWrites.isEmpty()) {
            log.info("Scheduled flush triggered with {} pending writes", pendingWrites.size());
            flushAsync();
        }
    }

    /**
     * 获取待写入数据的数量
     * 
     * @return 待写入数据数量
     */
    public int getPendingWriteCount() {
        return pendingWrites.size();
    }

    /**
     * 清空待写入队列（慎用）
     */
    public void clearPendingWrites() {
        int count = pendingWrites.size();
        pendingWrites.clear();
        log.warn("Cleared {} pending writes", count);
    }

    /**
     * 从缓存获取值
     */
    private <T> T getCachedValue(String key, Class<T> type) {
        if (multiLevelCacheManager != null) {
            return multiLevelCacheManager.get(key, type);
        } else {
            Object value = redisService.get(key);
            return value != null ? type.cast(value) : null;
        }
    }

    /**
     * 设置缓存值
     */
    private void setCachedValue(String key, Object value, Duration ttl) {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.set(key, value, ttl);
        } else {
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisService.set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
            } else {
                redisService.set(key, value);
            }
        }
    }

    /**
     * 关闭时刷新所有待写入数据
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down WriteBehindTemplate, flushing pending writes");
        
        // 刷新所有待写入数据
        flush();
        
        // 关闭执行器
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("WriteBehindTemplate shutdown complete");
    }

    /**
     * 待写入数据包装类
     */
    private static class PendingWrite {
        private final String key;
        private final Object value;
        private final long timestamp;

        public PendingWrite(String key, Object value, long timestamp) {
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
