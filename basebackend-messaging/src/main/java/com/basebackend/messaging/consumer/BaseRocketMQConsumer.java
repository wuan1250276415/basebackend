package com.basebackend.messaging.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.messaging.handler.MessageHandler;
import com.basebackend.messaging.idempotency.IdempotencyService;
import com.basebackend.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RocketMQ 消息消费者基类
 *
 * <p>提供统一的消息处理框架：
 * <ul>
 *   <li>幂等性检查（可选，需 Redis 支持）</li>
 *   <li>分布式锁防并发重复消费（可选）</li>
 *   <li>异常处理与自动重试</li>
 * </ul>
 * </p>
 *
 * <p>{@link IdempotencyService} 为可选依赖，当 Redis 未配置时自动降级为
 * "无幂等性保护"模式，消费者仍可正常工作。</p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseRocketMQConsumer<T> implements org.apache.rocketmq.spring.core.RocketMQListener<String> {

    @Autowired(required = false)
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
            message = parseMessage(messageJson);
            String messageId = message.getMessageId();

            log.info("收到消息: topic={}, messageId={}, type={}",
                    message.getTopic(), messageId, message.getMessageType());

            if (idempotencyService != null) {
                // 1. 幂等性检查
                if (idempotencyService.isDuplicate(messageId)) {
                    log.warn("消息已处理，跳过: messageId={}", messageId);
                    return;
                }

                // 2. 尝试获取分布式锁
                boolean locked = idempotencyService.tryLock(messageId);
                if (!locked) {
                    log.warn("获取分布式锁失败，消息可能正在被处理: messageId={}", messageId);
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
            } else {
                // 无幂等性保护，直接执行业务逻辑
                log.debug("IdempotencyService 未配置，跳过幂等性检查: messageId={}", messageId);
                getMessageHandler().handle(message);
                log.info("消息处理成功: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("消息处理失败: messageId={}, error={}",
                    message != null ? message.getMessageId() : "unknown",
                    e.getMessage(), e);
            throw new RuntimeException("消息处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析消息
     */
    private Message<T> parseMessage(String messageJson) {
        try {
            @SuppressWarnings("unchecked")
            Message<Object> genericMessage = JsonUtils.parseObject(messageJson, Message.class);

            String payloadJson = JsonUtils.toJsonString(genericMessage.getPayload());
            T payload = JsonUtils.parseObject(payloadJson, getPayloadClass());

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
