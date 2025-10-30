package com.basebackend.messaging.handler;

import com.basebackend.messaging.exception.MessageConsumeException;
import com.basebackend.messaging.model.Message;

/**
 * 消息处理器接口
 *
 * 用于 RocketMQ 消费者，封装业务处理逻辑
 *
 * @param <T> 消息负载类型
 * @author Claude Code
 * @since 2025-10-30
 */
@FunctionalInterface
public interface MessageHandler<T> {

    /**
     * 处理消息
     *
     * @param message 消息对象
     * @throws MessageConsumeException 消息消费异常
     */
    void handle(Message<T> message) throws MessageConsumeException;
}
