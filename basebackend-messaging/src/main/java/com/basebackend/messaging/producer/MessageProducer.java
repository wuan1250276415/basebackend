package com.basebackend.messaging.producer;

import com.basebackend.messaging.model.Message;

/**
 * 消息生产者接口
 */
public interface MessageProducer {

    /**
     * 发送消息
     *
     * @param message 消息对象
     * @param <T>     消息体类型
     * @return 消息ID
     */
    <T> String send(Message<T> message);

    /**
     * 发送延迟消息
     *
     * @param message      消息对象
     * @param delayMillis  延迟时间（毫秒）
     * @param <T>          消息体类型
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
