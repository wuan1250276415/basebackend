package com.basebackend.messaging.management.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.messaging.management.service.WebhookLogStore;
import com.basebackend.messaging.webhook.WebhookLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/messaging/webhook-log")
public class WebhookLogController {

    private final ObjectProvider<WebhookLogStore> webhookLogStoreProvider;

    public WebhookLogController(ObjectProvider<WebhookLogStore> webhookLogStoreProvider) {
        this.webhookLogStoreProvider = webhookLogStoreProvider;
    }

    @GetMapping("/page")
    public Result<PageResult<WebhookLog>> getPage(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Long webhookId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            WebhookLogStore store = webhookLogStoreProvider.getIfAvailable();
            if (store == null) {
                return Result.success(PageResult.empty(page, size));
            }
            return Result.success(store.page(page, size, webhookId, eventType, success, startTime, endTime));
        } catch (Exception e) {
            log.error("分页查询Webhook日志失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<WebhookLog> getById(@PathVariable Long id) {
        try {
            WebhookLogStore store = webhookLogStoreProvider.getIfAvailable();
            if (store == null) {
                return Result.error("Webhook日志存储未启用");
            }
            WebhookLog logItem = store.findById(id);
            return logItem != null ? Result.success(logItem) : Result.error("Webhook日志不存在");
        } catch (Exception e) {
            log.error("查询Webhook日志失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }
}
