package com.basebackend.messaging.consumer;

import com.alibaba.fastjson2.JSON;
import com.basebackend.messaging.handler.MessageHandler;
import com.basebackend.messaging.idempotency.IdempotencyService;
import com.basebackend.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RocketMQ 消息消费者基类
 *
 * 提供统一的消息处理框架：
 * - 幂等性检查
 * - 分布式锁
 * - 异常处理
 * - 重试机制
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
public abstract class BaseRocketMQConsumer<T> implements org.apache.rocketmq.spring.core.RocketMQListener<String> {

    @Autowired
    private IdempotencyService idempotencyService;

    /**
     * 获取消息处理器
     */
    protected abstract MessageHandler<T> getMessageHandler();

    /**
     * 获取消息类型的 Class
     */
    protected abstract Class<T> getPayloadClass();

    /**
     * RocketMQ 消息监听方法
     */
    @Override
    public void onMessage(String messageJson) {
        Message<T> message = null;
        try {
            // 解析消息
            message = parseMessage(messageJson);
            String messageId = message.getMessageId();

            log.info("收到消息: topic={}, messageId={}, type={}",
                    message.getTopic(), messageId, message.getMessageType());

            // 1. 幂等性检查
            if (idempotencyService.isDuplicate(messageId)) {
                log.warn("消息已处理，跳过: messageId={}", messageId);
                return; // 返回成功，不再重试
            }

            // 2. 尝试获取分布式锁
            boolean locked = idempotencyService.tryLock(messageId);
            if (!locked) {
                log.warn("获取分布式锁失败，消息可能正在被处理: messageId={}", messageId);
                // 返回 RECONSUME_LATER 会触发重试
                throw new RuntimeException("获取分布式锁失败");
            }

            try {
                // 3. 执行业务逻辑
                getMessageHandler().handle(message);

                // 4. 标记消息已处理
                idempotencyService.markAsProcessed(messageId);

                log.info("消息处理成功: messageId={}", messageId);

            } finally {
                // 5. 释放锁
                idempotencyService.unlock(messageId);
            }

        } catch (Exception e) {
            log.error("消息处理失败: messageId={}, error={}",
                    message != null ? message.getMessageId() : "unknown",
                    e.getMessage(), e);

            // 抛出异常会导致 RocketMQ 自动重试
            // 达到最大重试次数后会进入死信队列
            throw new RuntimeException("消息处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析消息
     */
    private Message<T> parseMessage(String messageJson) {
        try {
            // 首先解析为通用 Message
            @SuppressWarnings("unchecked")
            Message<Object> genericMessage = JSON.parseObject(messageJson, Message.class);

            // 然后解析 payload
            String payloadJson = JSON.toJSONString(genericMessage.getPayload());
            T payload = JSON.parseObject(payloadJson, getPayloadClass());

            // 创建具体类型的 Message
            Message<T> message = new Message<>();
            message.setMessageId(genericMessage.getMessageId());
            message.setTopic(genericMessage.getTopic());
            message.setTags(genericMessage.getTags());
            message.setMessageType(genericMessage.getMessageType());
            message.setPayload(payload);
            message.setTimestamp(genericMessage.getTimestamp());

            return message;

        } catch (Exception e) {
            log.error("消息解析失败: messageJson={}, error={}", messageJson, e.getMessage(), e);
            throw new RuntimeException("消息解析失败: " + e.getMessage(), e);
        }
    }
}
