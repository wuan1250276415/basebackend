package com.basebackend.admin.consumer;

import com.alibaba.fastjson2.JSON;
import com.basebackend.admin.constants.NotificationConstants;
import com.basebackend.admin.dto.notification.NotificationMessageDTO;
import com.basebackend.admin.service.SSENotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者
 * 消费来自 RocketMQ 的通知消息，并通过 SSE 推送给用户
 *
 * @author Claude Code
 * @since 2025-11-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = NotificationConstants.NOTIFICATION_TOPIC,
        consumerGroup = NotificationConstants.NOTIFICATION_CONSUMER_GROUP,
        selectorExpression = NotificationConstants.TAG_SYSTEM + " || " +
                NotificationConstants.TAG_ANNOUNCEMENT + " || " +
                NotificationConstants.TAG_REMINDER
)
public class NotificationConsumer implements RocketMQListener<String> {

    private final SSENotificationService sseNotificationService;

    @Override
    public void onMessage(String message) {
        log.info("[NotificationConsumer] 收到通知消息: {}", message);

        try {
            // 解析消息
            NotificationMessageDTO notification = JSON.parseObject(message, NotificationMessageDTO.class);

            if (notification == null || notification.getUserId() == null) {
                log.warn("[NotificationConsumer] 消息格式错误或用户ID为空: {}", message);
                return;
            }

            log.info("[NotificationConsumer] 处理通知: userId={}, notificationId={}, title={}",
                    notification.getUserId(), notification.getId(), notification.getTitle());

            // 通过 SSE 推送给用户
            sseNotificationService.pushNotificationToUser(notification.getUserId(), notification);

            log.info("[NotificationConsumer] 通知处理完成: userId={}, notificationId={}",
                    notification.getUserId(), notification.getId());

        } catch (Exception e) {
            log.error("[NotificationConsumer] 消息处理失败: message={}, error={}",
                    message, e.getMessage(), e);
            // 注意：这里如果抛异常，RocketMQ会重试
            // 根据业务需要决定是否抛出异常
        }
    }
}
