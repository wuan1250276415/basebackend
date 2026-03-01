package com.basebackend.ticket.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单通知消息（RocketMQ 消息载荷）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketNotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String notificationType;

    private Long ticketId;

    private String ticketNo;

    private String title;

    private String content;

    private Long targetUserId;

    private String targetUserName;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime eventTime;
}
