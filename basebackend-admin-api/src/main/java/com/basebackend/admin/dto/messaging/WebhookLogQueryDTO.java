package com.basebackend.admin.dto.messaging;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook日志查询DTO
 */
@Data
public class WebhookLogQueryDTO {

    private Long webhookId;

    private String eventType;

    private Boolean success;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer page = 1;

    private Integer size = 20;
}
