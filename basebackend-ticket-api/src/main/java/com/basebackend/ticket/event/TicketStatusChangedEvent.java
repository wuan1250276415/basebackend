package com.basebackend.ticket.event;

import com.basebackend.common.event.DomainEvent;
import lombok.Getter;

/**
 * 工单状态变更事件
 */
@Getter
public class TicketStatusChangedEvent extends DomainEvent {

    private final Long ticketId;
    private final String ticketNo;
    private final String fromStatus;
    private final String toStatus;
    private final Long operatorId;
    private final String operatorName;
    private final String remark;

    public TicketStatusChangedEvent(String source, Long ticketId, String ticketNo,
                                    String fromStatus, String toStatus,
                                    Long operatorId, String operatorName,
                                    String remark) {
        super(source);
        this.ticketId = ticketId;
        this.ticketNo = ticketNo;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.remark = remark;
    }
}
