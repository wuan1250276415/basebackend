package com.basebackend.admin.controller.messaging;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.messaging.WebhookLogQueryDTO;
import com.basebackend.admin.entity.messaging.SysWebhookLog;
import com.basebackend.admin.service.messaging.WebhookLogService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook调用日志Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/messaging/webhook-log")
@Validated
@Tag(name = "Webhook调用日志", description = "Webhook调用日志查询")
public class WebhookLogController {

    private final WebhookLogService webhookLogService;

    public WebhookLogController(WebhookLogService webhookLogService) {
        this.webhookLogService = webhookLogService;
    }

    @Operation(summary = "分页查询Webhook调用日志")
    @GetMapping("/page")
    public Result<Page<SysWebhookLog>> getWebhookLogPage(WebhookLogQueryDTO dto) {
        return Result.success(webhookLogService.getWebhookLogPage(dto));
    }

    @Operation(summary = "获取Webhook调用日志详情")
    @GetMapping("/{id}")
    public Result<SysWebhookLog> getWebhookLog(@PathVariable Long id) {
        return Result.success(webhookLogService.getWebhookLogById(id));
    }
}
