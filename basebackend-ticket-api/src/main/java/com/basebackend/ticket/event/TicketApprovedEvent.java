package com.basebackend.ticket.event;

import com.basebackend.common.event.DomainEvent;
import lombok.Getter;

/**
 * 工单审批事件
 */
@Getter
public class TicketApprovedEvent extends DomainEvent {

    private final Long ticketId;
    private final String ticketNo;
    private final String action;
    private final Long approverId;
    private final String approverName;
    private final String opinion;

    public TicketApprovedEvent(String source, Long ticketId, String ticketNo,
                               String action, Long approverId, String approverName,
                               String opinion) {
        super(source);
        this.ticketId = ticketId;
        this.ticketNo = ticketNo;
        this.action = action;
        this.approverId = approverId;
        this.approverName = approverName;
        this.opinion = opinion;
    }
}
