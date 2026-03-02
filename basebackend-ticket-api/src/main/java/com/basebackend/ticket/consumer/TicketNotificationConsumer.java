package com.basebackend.ticket.consumer;

import com.basebackend.messaging.consumer.BaseRocketMQConsumer;
import com.basebackend.messaging.handler.MessageHandler;
import com.basebackend.ticket.event.TicketNotificationMessage;
import com.basebackend.ticket.notification.TicketNotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 工单通知消息消费者
 * <p>消费来自 ticket-notification-topic 的通知消息，执行实际通知分发</p>
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "ticket-notification-topic",
        consumerGroup = "ticket-notification-consumer-group"
)
public class TicketNotificationConsumer extends BaseRocketMQConsumer<TicketNotificationMessage> {

    private static final Logger log = LoggerFactory.getLogger(TicketNotificationConsumer.class);

    private final TicketNotificationDispatcher notificationDispatcher;

    @Override
    protected MessageHandler<TicketNotificationMessage> getMessageHandler() {
        return message -> {
            TicketNotificationMessage payload = message.getPayload();
            log.info("处理工单通知: type={}, ticketNo={}, target={}",
                    payload.getNotificationType(),
                    payload.getTicketNo(),
                    payload.getTargetUserName());

            // 根据通知类型分发
            switch (payload.getNotificationType()) {
                case "TICKET_CREATED" -> handleTicketCreated(payload);
                case "TICKET_ASSIGNED" -> handleTicketAssigned(payload);
                case "STATUS_CHANGED" -> handleStatusChanged(payload);
                case "TICKET_APPROVAL" -> handleTicketApproval(payload);
                default -> log.warn("未知通知类型: {}", payload.getNotificationType());
            }
        };
    }

    @Override
    protected Class<TicketNotificationMessage> getPayloadClass() {
        return TicketNotificationMessage.class;
    }

    private void handleTicketCreated(TicketNotificationMessage payload) {
        log.info("工单创建通知: ticketNo={}, 通知目标={}",
                payload.getTicketNo(), payload.getTargetUserName());
        notificationDispatcher.dispatchCreated(payload);
    }

    private void handleTicketAssigned(TicketNotificationMessage payload) {
        log.info("工单分配通知: ticketNo={}, 分配给={}",
                payload.getTicketNo(), payload.getTargetUserName());
        notificationDispatcher.dispatchAssigned(payload);
    }

    private void handleStatusChanged(TicketNotificationMessage payload) {
        log.info("工单状态变更通知: ticketNo={}", payload.getTicketNo());
        notificationDispatcher.dispatchStatusChanged(payload);
    }

    private void handleTicketApproval(TicketNotificationMessage payload) {
        log.info("工单审批通知: ticketNo={}", payload.getTicketNo());
        notificationDispatcher.dispatchApproval(payload);
    }
}
