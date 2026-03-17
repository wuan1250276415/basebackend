package com.basebackend.messaging.order;

import com.basebackend.messaging.consumer.MessageConsumer;
import com.basebackend.messaging.model.Message;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 顺序消息消费器
 * 确保同一 partitionKey 的消息按顺序消费。
 *
 * <p>每个 partitionKey 对应一个单线程执行器（平台线程），并通过 LRU-like 过期机制
 * 回收长期空闲的 executor，防止无界内存增长。</p>
 */
@Slf4j
@Component
public class OrderedMessageConsumer {

    /** executor 空闲超过此时间后将被回收（毫秒） */
    private static final long IDLE_EXPIRE_MS = 10 * 60 * 1000L; // 10 分钟

    private final Map<String, ExecutorEntry> executorMap = new ConcurrentHashMap<>();

    /**
     * 顺序消费消息
     *
     * @param message  消息对象
     * @param consumer 消费者函数
     * @param <T>      消息体类型
     */
    public <T> void consume(Message<T> message, MessageConsumer<T> consumer) {
        String partitionKey = message.getPartitionKey();
        if (partitionKey == null) {
            try {
                consumer.consume(message);
            } catch (Exception e) {
                log.error("Failed to consume message: messageId={}", message.getMessageId(), e);
            }
            return;
        }

        ExecutorEntry entry = executorMap.computeIfAbsent(partitionKey, k -> {
            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("ordered-consumer-" + k);
                thread.setDaemon(true);
                return thread;
            });
            return new ExecutorEntry(executor);
        });

        entry.touch();
        entry.executor.submit(() -> {
            try {
                consumer.consume(message);
                log.info("Ordered message consumed: messageId={}, partitionKey={}",
                        message.getMessageId(), partitionKey);
            } catch (Exception e) {
                log.error("Failed to consume ordered message: messageId={}, partitionKey={}",
                        message.getMessageId(), partitionKey, e);
            }
        });
    }

    /**
     * 定期清理长期空闲的 executor（每 5 分钟执行一次）
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void evictIdleExecutors() {
        long now = Instant.now().toEpochMilli();
        executorMap.entrySet().removeIf(entry -> {
            if (now - entry.getValue().lastUsedMs > IDLE_EXPIRE_MS) {
                entry.getValue().executor.shutdown();
                log.debug("Evicted idle ordered executor for partitionKey={}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 应用关闭时优雅停止所有执行器
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {} ordered message executors", executorMap.size());
        executorMap.values().forEach(entry -> {
            entry.executor.shutdown();
            try {
                if (!entry.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    entry.executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                entry.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
        executorMap.clear();
    }

    /**
     * executor 条目，持有执行器及最后使用时间
     */
    private static class ExecutorEntry {
        final ExecutorService executor;
        volatile long lastUsedMs;

        ExecutorEntry(ExecutorService executor) {
            this.executor = executor;
            this.lastUsedMs = Instant.now().toEpochMilli();
        }

        void touch() {
            this.lastUsedMs = Instant.now().toEpochMilli();
        }
    }
}
