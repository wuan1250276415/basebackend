package com.basebackend.admin.controller.messaging;

import com.basebackend.admin.service.messaging.MessageMonitorService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息监控Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/messaging/monitor")
@Validated
@Tag(name = "消息监控", description = "消息队列监控和统计")
public class MessageMonitorController {

    private final MessageMonitorService messageMonitorService;

    public MessageMonitorController(MessageMonitorService messageMonitorService) {
        this.messageMonitorService = messageMonitorService;
    }

    @Operation(summary = "获取消息统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getMessageStatistics() {
        return Result.success(messageMonitorService.getMessageStatistics());
    }

    @Operation(summary = "获取队列监控信息")
    @GetMapping("/queue")
    public Result<Map<String, Object>> getQueueMonitor() {
        return Result.success(messageMonitorService.getQueueMonitor());
    }
}
