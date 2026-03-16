package com.basebackend.ticket.notification;

import com.basebackend.ticket.event.TicketNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单通知分发器
 * <p>统一负责构建通知内容并调用通知渠道发送</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotificationDispatcher {

    private final TicketNotificationTemplate template;

    /**
     * 分发工单创建通知 - 通知处理人
     */
    public void dispatchCreated(TicketNotificationMessage msg) {
        if (msg.getTargetUserId() == null) {
            log.debug("工单创建通知无目标用户，跳过: ticketNo={}", msg.getTicketNo());
            return;
        }
        String title = template.buildCreatedTitle(msg);
        String content = template.buildCreatedContent(msg);
        sendNotification(msg.getTargetUserId(), msg.getTargetUserName(), title, content, msg.getTicketId());
    }

    /**
     * 分发工单分配通知 - 通知新处理人
     */
    public void dispatchAssigned(TicketNotificationMessage msg) {
        if (msg.getTargetUserId() == null) {
            log.debug("工单分配通知无目标用户，跳过: ticketNo={}", msg.getTicketNo());
            return;
        }
        String title = template.buildAssignedTitle(msg);
        String content = template.buildAssignedContent(msg);
        sendNotification(msg.getTargetUserId(), msg.getTargetUserName(), title, content, msg.getTicketId());
    }

    /**
     * 分发状态变更通知 - 通知提交人和处理人
     */
    public void dispatchStatusChanged(TicketNotificationMessage msg) {
        String title = template.buildStatusChangedTitle(msg);
        String content = template.buildStatusChangedContent(msg);

        // 通知操作人之外的相关方（目标用户由事件处理器设定）
        if (msg.getTargetUserId() != null) {
            sendNotification(msg.getTargetUserId(), msg.getTargetUserName(), title, content, msg.getTicketId());
        }
        // 通知操作人（如果与目标用户不同）
        if (msg.getOperatorId() != null && !msg.getOperatorId().equals(msg.getTargetUserId())) {
            sendNotification(msg.getOperatorId(), msg.getOperatorName(), title, content, msg.getTicketId());
        }
    }

    /**
     * 分发审批通知 - 根据审批动作通知不同人
     */
    public void dispatchApproval(TicketNotificationMessage msg) {
        String title = template.buildApprovalTitle(msg);
        String content = template.buildApprovalContent(msg);

        if (msg.getTargetUserId() != null) {
            sendNotification(msg.getTargetUserId(), msg.getTargetUserName(), title, content, msg.getTicketId());
        }
        if (msg.getOperatorId() != null && !msg.getOperatorId().equals(msg.getTargetUserId())) {
            sendNotification(msg.getOperatorId(), msg.getOperatorName(), title, content, msg.getTicketId());
        }
    }

    private void sendNotification(Long userId, String userName, String title, String content, Long ticketId) {
        // 通过日志记录通知（实际生产环境可替换为 SSE 推送、站内信、邮件等多渠道）
        log.info("发送工单通知: userId={}, userName={}, title={}, ticketId={}", userId, userName, title, ticketId);
        // TODO: 后续集成 notification-service Feign 客户端实现多渠道通知
        // notificationServiceClient.send(NotificationMessageDTO.builder()
        //     .targetUserId(userId).title(title).content(content)
        //     .type("TICKET").bizId(String.valueOf(ticketId)).build());
    }
}
