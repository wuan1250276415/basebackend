package com.basebackend.messaging.management.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.messaging.webhook.WebhookLog;

import java.time.LocalDateTime;

public interface WebhookLogStore {

    WebhookLog save(WebhookLog webhookLog);

    WebhookLog findById(Long id);

    PageResult<WebhookLog> page(long current, long size,
                                Long webhookId,
                                String eventType,
                                Boolean success,
                                LocalDateTime startTime,
                                LocalDateTime endTime);
}
