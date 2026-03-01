package com.basebackend.ticket.event;

import com.basebackend.common.event.DomainEventListener;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工单领域事件处理器
 * <p>桥接领域事件到 RocketMQ，异步推送通知消息</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventHandler {

    private static final String NOTIFICATION_TOPIC = "ticket-notification-topic";

    private final MessageProducer messageProducer;

    @DomainEventListener
    public void onTicketCreated(TicketCreatedEvent event) {
        log.info("处理工单创建事件: ticketNo={}", event.getTicketNo());

        // 通知处理人
        if (event.getAssigneeId() != null) {
            sendNotification("TICKET_CREATED", "CREATED", event.getTicketId(),
                    event.getTicketNo(), event.getTitle(),
                    "您有新的工单待处理: " + event.getTicketNo(),
                    event.getAssigneeId(), event.getAssigneeName(),
                    event.getReporterId(), event.getReporterName());
        }
    }

    @DomainEventListener
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("处理工单状态变更事件: ticketNo={}, {} -> {}",
                event.getTicketNo(), event.getFromStatus(), event.getToStatus());

        String content = String.format("工单 %s 状态已变更: %s → %s",
                event.getTicketNo(), event.getFromStatus(), event.getToStatus());

        sendNotification("STATUS_CHANGED", "STATUS_CHANGED", event.getTicketId(),
                event.getTicketNo(), null, content,
                null, null,
                event.getOperatorId(), event.getOperatorName());
    }

    @DomainEventListener
    public void onTicketAssigned(TicketAssignedEvent event) {
        log.info("处理工单分配事件: ticketNo={}, assignee={}",
                event.getTicketNo(), event.getAssigneeName());

        String content = String.format("工单 %s 已分配给您处理", event.getTicketNo());

        sendNotification("TICKET_ASSIGNED", "ASSIGNED", event.getTicketId(),
                event.getTicketNo(), null, content,
                event.getAssigneeId(), event.getAssigneeName(),
                event.getOperatorId(), event.getOperatorName());
    }

    @DomainEventListener
    public void onTicketApproved(TicketApprovedEvent event) {
        log.info("处理工单审批事件: ticketNo={}, action={}",
                event.getTicketNo(), event.getAction());

        String content = String.format("工单 %s 审批%s",
                event.getTicketNo(), event.getAction());
        if (event.getOpinion() != null && !event.getOpinion().isEmpty()) {
            content += ": " + event.getOpinion();
        }

        sendNotification("TICKET_APPROVAL", "APPROVAL_" + event.getAction(),
                event.getTicketId(), event.getTicketNo(), null, content,
                null, null,
                event.getApproverId(), event.getApproverName());
    }

    private void sendNotification(String notificationType, String tags,
                                  Long ticketId, String ticketNo,
                                  String title, String content,
                                  Long targetUserId, String targetUserName,
                                  Long operatorId, String operatorName) {
        TicketNotificationMessage notification = TicketNotificationMessage.builder()
                .notificationType(notificationType)
                .ticketId(ticketId)
                .ticketNo(ticketNo)
                .title(title)
                .content(content)
                .targetUserId(targetUserId)
                .targetUserName(targetUserName)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(LocalDateTime.now())
                .build();

        Message<TicketNotificationMessage> msg = Message.<TicketNotificationMessage>builder()
                .messageId(UUID.randomUUID().toString())
                .topic(NOTIFICATION_TOPIC)
                .tags(tags)
                .messageType(notificationType)
                .payload(notification)
                .timestamp(LocalDateTime.now())
                .build();

        messageProducer.sendAsync(msg)
                .whenComplete((msgId, ex) -> {
                    if (ex != null) {
                        log.error("发送工单通知消息失败: ticketNo={}, type={}", ticketNo, notificationType, ex);
                    } else {
                        log.debug("工单通知消息发送成功: ticketNo={}, msgId={}", ticketNo, msgId);
                    }
                });
    }
}
