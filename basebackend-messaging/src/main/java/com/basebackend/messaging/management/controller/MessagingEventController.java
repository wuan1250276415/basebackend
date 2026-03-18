package com.basebackend.messaging.management.controller;

import com.basebackend.common.model.Result;
import com.basebackend.messaging.event.EventPublisher;
import com.basebackend.messaging.management.dto.PublishEventRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/messaging/event")
@Validated
public class MessagingEventController {

    private final ObjectProvider<EventPublisher> eventPublisherProvider;

    public MessagingEventController(ObjectProvider<EventPublisher> eventPublisherProvider) {
        this.eventPublisherProvider = eventPublisherProvider;
    }

    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody PublishEventRequest request) {
        try {
            EventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
            if (eventPublisher == null) {
                return Result.error("事件发布能力未启用");
            }

            String source = request.source() != null && !request.source().isBlank()
                    ? request.source().trim()
                    : "messaging-management";
            eventPublisher.publishEvent(request.eventType(), request.data(), source, request.metadata());
            return Result.success();
        } catch (Exception e) {
            log.error("发布事件失败: eventType={}", request.eventType(), e);
            return Result.error(e.getMessage());
        }
    }
}
