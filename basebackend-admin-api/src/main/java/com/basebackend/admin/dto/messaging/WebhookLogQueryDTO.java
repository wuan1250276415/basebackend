package com.basebackend.admin.dto.messaging;

import java.time.LocalDateTime;

/**
 * Webhook日志查询DTO
 */
public record WebhookLogQueryDTO(
    Long webhookId,
    String eventType,
    Boolean success,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer page,
    Integer size
) {
    public WebhookLogQueryDTO {
        if (page == null) page = 1;
        if (size == null) size = 20;
    }
}
