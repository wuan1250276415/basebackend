package com.basebackend.messaging.producer;

import com.basebackend.messaging.model.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 消息生产者接口
 * <p>
 * 定义消息发送的统一接口，支持同步、异步、批量、延迟、事务、顺序等多种发送方式。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface MessageProducer {

    /**
     * 同步发送消息
     *
     * @param message 消息对象
     * @param <T>     消息体类型
     * @return 消息ID
     */
    <T> String send(Message<T> message);

    /**
     * 异步发送消息
     *
     * @param message 消息对象
     * @param <T>     消息体类型
     * @return 消息ID的Future
     */
    <T> CompletableFuture<String> sendAsync(Message<T> message);

    /**
     * 批量发送消息
     *
     * @param messages 消息列表
     * @param <T>      消息体类型
     * @return 消息ID列表
     */
    <T> List<String> sendBatch(List<Message<T>> messages);

    /**
     * 批量异步发送消息
     *
     * @param messages 消息列表
     * @param <T>      消息体类型
     * @return 消息ID列表的Future
     */
    <T> CompletableFuture<List<String>> sendBatchAsync(List<Message<T>> messages);

    /**
     * 发送延迟消息
     *
     * @param message     消息对象
     * @param delayMillis 延迟时间（毫秒）
     * @param <T>         消息体类型
     * @return 消息ID
     */
    <T> String sendDelay(Message<T> message, long delayMillis);

    /**
     * 发送事务消息
     *
     * @param message 消息对象
     * @param <T>     消息体类型
     * @return 消息ID
     */
    <T> String sendTransactional(Message<T> message);

    /**
     * 发送顺序消息
     *
     * @param message      消息对象
     * @param partitionKey 分区键
     * @param <T>          消息体类型
     * @return 消息ID
     */
    <T> String sendOrdered(Message<T> message, String partitionKey);
}
