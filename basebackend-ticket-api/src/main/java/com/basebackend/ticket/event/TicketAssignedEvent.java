package com.basebackend.ticket.event;

import com.basebackend.common.event.DomainEvent;
import lombok.Getter;

/**
 * 工单分配事件
 */
@Getter
public class TicketAssignedEvent extends DomainEvent {

    private final Long ticketId;
    private final String ticketNo;
    private final Long assigneeId;
    private final String assigneeName;
    private final Long operatorId;
    private final String operatorName;

    public TicketAssignedEvent(String source, Long ticketId, String ticketNo,
                               Long assigneeId, String assigneeName,
                               Long operatorId, String operatorName) {
        super(source);
        this.ticketId = ticketId;
        this.ticketNo = ticketNo;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }
}
