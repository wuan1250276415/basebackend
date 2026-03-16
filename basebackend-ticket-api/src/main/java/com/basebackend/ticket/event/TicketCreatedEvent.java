package com.basebackend.ticket.event;

import com.basebackend.common.event.DomainEvent;
import lombok.Getter;

/**
 * 工单创建事件
 */
@Getter
public class TicketCreatedEvent extends DomainEvent {

    private final Long ticketId;
    private final String ticketNo;
    private final Long reporterId;
    private final String reporterName;
    private final Long assigneeId;
    private final String assigneeName;
    private final String title;
    private final Integer priority;

    public TicketCreatedEvent(String source, Long ticketId, String ticketNo,
                              Long reporterId, String reporterName,
                              Long assigneeId, String assigneeName,
                              String title, Integer priority) {
        super(source);
        this.ticketId = ticketId;
        this.ticketNo = ticketNo;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.title = title;
        this.priority = priority;
    }
}
