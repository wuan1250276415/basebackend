package com.basebackend.ticket.notification;

import com.basebackend.ticket.event.TicketNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单通知消息模板
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotificationTemplate {

    public String buildCreatedTitle(TicketNotificationMessage msg) {
        return "新工单通知";
    }

    public String buildCreatedContent(TicketNotificationMessage msg) {
        return String.format("您有新的工单待处理 [%s] %s，提交人: %s",
                msg.getTicketNo(), msg.getTitle() != null ? msg.getTitle() : "", msg.getOperatorName());
    }

    public String buildAssignedTitle(TicketNotificationMessage msg) {
        return "工单分配通知";
    }

    public String buildAssignedContent(TicketNotificationMessage msg) {
        return String.format("工单 [%s] 已分配给您处理，请及时跟进", msg.getTicketNo());
    }

    public String buildStatusChangedTitle(TicketNotificationMessage msg) {
        return "工单状态变更通知";
    }

    public String buildStatusChangedContent(TicketNotificationMessage msg) {
        return msg.getContent() != null ? msg.getContent()
                : String.format("工单 [%s] 状态已变更", msg.getTicketNo());
    }

    public String buildApprovalTitle(TicketNotificationMessage msg) {
        return "工单审批通知";
    }

    public String buildApprovalContent(TicketNotificationMessage msg) {
        return msg.getContent() != null ? msg.getContent()
                : String.format("工单 [%s] 审批状态已更新", msg.getTicketNo());
    }
}
