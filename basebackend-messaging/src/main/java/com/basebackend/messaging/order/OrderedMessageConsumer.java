package com.basebackend.messaging.order;

import com.basebackend.messaging.consumer.MessageConsumer;
import com.basebackend.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 顺序消息消费器
 * 确保同一partitionKey的消息按顺序消费
 */
@Slf4j
@Component
public class OrderedMessageConsumer {

    /**
     * 每个partitionKey对应一个单线程执行器
     */
    private final Map<String, ExecutorService> executorMap = new ConcurrentHashMap<>();

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
            // 没有分区键，直接消费
            try {
                consumer.consume(message);
            } catch (Exception e) {
                log.error("Failed to consume message: messageId={}", message.getMessageId(), e);
            }
            return;
        }

        // 获取或创建该分区键对应的单线程执行器
        ExecutorService executor = executorMap.computeIfAbsent(partitionKey,
                k -> Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r);
                    thread.setName("ordered-consumer-" + k);
                    thread.setDaemon(true);
                    return thread;
                }));

        // 提交到单线程执行器中顺序执行
        executor.submit(() -> {
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
     * 关闭所有执行器
     */
    public void shutdown() {
        executorMap.values().forEach(ExecutorService::shutdown);
        executorMap.clear();
    }
}
