package com.basebackend.messaging.consumer;

import com.basebackend.messaging.model.Message;

/**
 * 消息消费者接口
 */
@FunctionalInterface
public interface MessageConsumer<T> {

    /**
     * 消费消息
     *
     * @param message 消息对象
     * @throws Exception 消费异常
     */
    void consume(Message<T> message) throws Exception;
}
