package com.basebackend.ticket.event;

import com.basebackend.common.event.DomainEventListener;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.ticket.realtime.TicketRealtimeService;
import com.basebackend.ticket.search.TicketSearchService;
import com.basebackend.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 工单领域事件处理器
 * <p>桥接领域事件到 RocketMQ，异步推送通知消息；
 * Phase 2: 集成搜索索引更新和 WebSocket 实时推送</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventHandler {

    private static final String NOTIFICATION_TOPIC = "ticket-notification-topic";

    private final MessageProducer messageProducer;
    private final TicketMapper ticketMapper;

    @Autowired(required = false)
    private TicketSearchService ticketSearchService;

    @Autowired(required = false)
    private TicketRealtimeService ticketRealtimeService;

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

        // Phase 2: 索引到 ES
        indexTicketAsync(event.getTicketId());

        // Phase 2: 实时推送
        pushRealtimeEvent(event.getTicketId(), "TICKET_CREATED", Map.of(
                "ticketNo", event.getTicketNo(),
                "title", event.getTitle() != null ? event.getTitle() : "",
                "priority", event.getPriority() != null ? event.getPriority() : 3));
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

        // Phase 2: 更新 ES 索引
        indexTicketAsync(event.getTicketId());

        // Phase 2: 实时推送
        pushRealtimeEvent(event.getTicketId(), "STATUS_CHANGED", Map.of(
                "ticketNo", event.getTicketNo(),
                "fromStatus", event.getFromStatus(),
                "toStatus", event.getToStatus()));
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

        // Phase 2: 更新 ES 索引
        indexTicketAsync(event.getTicketId());

        // Phase 2: 实时推送
        pushRealtimeEvent(event.getTicketId(), "TICKET_ASSIGNED", Map.of(
                "ticketNo", event.getTicketNo(),
                "assigneeName", event.getAssigneeName() != null ? event.getAssigneeName() : ""));
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

        // Phase 2: 更新 ES 索引
        indexTicketAsync(event.getTicketId());

        // Phase 2: 实时推送
        pushRealtimeEvent(event.getTicketId(), "TICKET_APPROVAL", Map.of(
                "ticketNo", event.getTicketNo(),
                "action", event.getAction()));
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

    private void indexTicketAsync(Long ticketId) {
        if (ticketSearchService == null) {
            return;
        }
        try {
            var ticket = ticketMapper.selectById(ticketId);
            if (ticket != null) {
                ticketSearchService.indexTicket(ticket);
            }
        } catch (Exception e) {
            log.warn("异步索引工单失败: ticketId={}", ticketId, e);
        }
    }

    private void pushRealtimeEvent(Long ticketId, String eventType, Map<String, Object> payload) {
        if (ticketRealtimeService == null) {
            return;
        }
        try {
            ticketRealtimeService.notifyTicketUpdate(ticketId, eventType, payload);
        } catch (Exception e) {
            log.warn("实时推送工单事件失败: ticketId={}, eventType={}", ticketId, eventType, e);
        }
    }
}
