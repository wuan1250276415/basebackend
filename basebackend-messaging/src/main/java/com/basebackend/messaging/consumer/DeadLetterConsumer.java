package com.basebackend.messaging.consumer;

import com.alibaba.fastjson2.JSON;
import com.basebackend.messaging.config.MessagingProperties;
import com.basebackend.messaging.constants.RocketMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 死信消费者
 *
 * 监听死信队列，将死信消息持久化到数据库
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(
        topic = RocketMQConstants.DLQ_TOPIC,
        consumerGroup = RocketMQConstants.DLQ_CONSUMER_GROUP,
//        consumeThreadMin = 1,
        consumeThreadMax = 5
)
public class DeadLetterConsumer implements RocketMQListener<String> {

    private final MessagingProperties messagingProperties;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onMessage(String message) {
        try {
            log.warn("收到死信消息: {}", message);

            // 解析消息
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = JSON.parseObject(message, Map.class);

            // 持久化到死信表
            saveToDeadLetterTable(messageMap, message);

            log.info("死信消息已保存到数据库");

        } catch (Exception e) {
            log.error("死信消息处理失败: error={}", e.getMessage(), e);
            // 死信消息处理失败也不再重试，避免死循环
        }
    }

    /**
     * 保存到死信表
     */
    private void saveToDeadLetterTable(Map<String, Object> messageMap, String originalMessage) {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate 未配置，无法保存死信消息");
            return;
        }

        try {
            String sql = "INSERT INTO sys_dead_letter " +
                    "(message_id, topic, tags, message_type, payload, original_message, error_message, retry_count, create_time, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String messageId = (String) messageMap.getOrDefault("messageId", "unknown");
            String topic = (String) messageMap.getOrDefault("topic", "unknown");
            String tags = (String) messageMap.getOrDefault("tags", "");
            String messageType = (String) messageMap.getOrDefault("messageType", "unknown");
            String payload = JSON.toJSONString(messageMap.get("payload"));
            String errorMessage = "达到最大重试次数，进入死信队列";
            Integer retryCount = messagingProperties.getRetry().getMaxAttempts();

            jdbcTemplate.update(sql,
                    messageId,
                    topic,
                    tags,
                    messageType,
                    payload,
                    originalMessage,
                    errorMessage,
                    retryCount,
                    LocalDateTime.now(),
                    "PENDING"
            );

            log.info("死信消息已保存: messageId={}", messageId);

        } catch (Exception e) {
            log.error("保存死信消息到数据库失败: error={}", e.getMessage(), e);
        }
    }
}
