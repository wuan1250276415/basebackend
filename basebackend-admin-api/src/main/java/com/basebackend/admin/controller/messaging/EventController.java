package com.basebackend.admin.controller.messaging;

import com.basebackend.admin.dto.messaging.EventPublishDTO;
import com.basebackend.common.model.Result;
import com.basebackend.messaging.event.EventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 事件发布Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/messaging/event")
@Validated
@Tag(name = "事件发布", description = "事件发布和管理")
public class EventController {

    private final EventPublisher eventPublisher;

    public EventController(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "发布事件")
    @PostMapping("/publish")
    public Result<Void> publishEvent(@Validated @RequestBody EventPublishDTO dto) {
        eventPublisher.publishEvent(
                dto.getEventType(),
                dto.getData(),
                dto.getSource() != null ? dto.getSource() : "admin-api",
                dto.getMetadata()
        );
        return Result.success();
    }
}
