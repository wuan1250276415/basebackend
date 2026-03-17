package com.basebackend.messaging.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.messaging.config.MessagingProperties;
import com.basebackend.messaging.entity.DeadLetterEntity;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 死信消费者
 *
 * <p>监听死信队列，将死信消息持久化到 {@code sys_dead_letter} 表。
 * Topic 和消费者组通过配置属性动态指定，默认值分别为
 * {@code basebackend-dlq-topic} / {@code basebackend-dlq-consumer-group}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(
        topic = "${messaging.dead-letter.topic:basebackend-dlq-topic}",
        consumerGroup = "${messaging.dead-letter.consumer-group:basebackend-dlq-consumer-group}"
)
public class DeadLetterConsumer implements RocketMQListener<String> {

    private final MessagingProperties messagingProperties;
    private final DeadLetterMapper deadLetterMapper;

    @Override
    public void onMessage(String message) {
        try {
            log.warn("收到死信消息: {}", message);

            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = JsonUtils.parseObject(message, Map.class);

            saveToDeadLetterTable(messageMap, message);

            log.info("死信消息已保存到数据库");

        } catch (Exception e) {
            log.error("死信消息处理失败: error={}", e.getMessage(), e);
            // 死信消息处理失败不再重试，避免死循环
        }
    }

    private void saveToDeadLetterTable(Map<String, Object> messageMap, String originalMessage) {
        try {
            DeadLetterEntity entity = new DeadLetterEntity();
            entity.setMessageId((String) messageMap.getOrDefault("messageId", "unknown"));
            entity.setTopic((String) messageMap.getOrDefault("topic", "unknown"));
            entity.setTags((String) messageMap.getOrDefault("tags", ""));
            entity.setMessageType((String) messageMap.getOrDefault("messageType", "unknown"));
            entity.setPayload(JsonUtils.toJsonString(messageMap.get("payload")));
            entity.setOriginalMessage(originalMessage);
            entity.setErrorMessage("达到最大重试次数，进入死信队列");
            entity.setRetryCount(messagingProperties.getRetry().getMaxAttempts());
            entity.setStatus("PENDING");

            deadLetterMapper.insert(entity);

            log.info("死信消息已保存: messageId={}", entity.getMessageId());

        } catch (Exception e) {
            log.error("保存死信消息到数据库失败: error={}", e.getMessage(), e);
        }
    }
}
