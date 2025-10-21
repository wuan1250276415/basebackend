package com.basebackend.admin.controller.messaging;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.messaging.WebhookConfigDTO;
import com.basebackend.admin.entity.messaging.SysWebhookConfig;
import com.basebackend.admin.service.messaging.WebhookConfigService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Webhook配置管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/messaging/webhook")
@Validated
@Tag(name = "Webhook配置管理", description = "Webhook配置的增删改查")
public class WebhookConfigController {

    private final WebhookConfigService webhookConfigService;

    public WebhookConfigController(WebhookConfigService webhookConfigService) {
        this.webhookConfigService = webhookConfigService;
    }

    @Operation(summary = "分页查询Webhook配置")
    @GetMapping("/page")
    public Result<Page<SysWebhookConfig>> getWebhookConfigPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String name) {
        return Result.success(webhookConfigService.getWebhookConfigPage(page, size, name));
    }

    @Operation(summary = "获取Webhook配置详情")
    @GetMapping("/{id}")
    public Result<SysWebhookConfig> getWebhookConfig(@PathVariable Long id) {
        return Result.success(webhookConfigService.getWebhookConfigById(id));
    }

    @Operation(summary = "创建Webhook配置")
    @PostMapping
    public Result<Long> createWebhookConfig(@Validated @RequestBody WebhookConfigDTO dto) {
        return Result.success(webhookConfigService.createWebhookConfig(dto));
    }

    @Operation(summary = "更新Webhook配置")
    @PutMapping("/{id}")
    public Result<Void> updateWebhookConfig(@PathVariable Long id, @Validated @RequestBody WebhookConfigDTO dto) {
        webhookConfigService.updateWebhookConfig(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除Webhook配置")
    @DeleteMapping("/{id}")
    public Result<Void> deleteWebhookConfig(@PathVariable Long id) {
        webhookConfigService.deleteWebhookConfig(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用Webhook")
    @PutMapping("/{id}/toggle")
    public Result<Void> toggleWebhookConfig(@PathVariable Long id, @RequestParam Boolean enabled) {
        webhookConfigService.toggleWebhookConfig(id, enabled);
        return Result.success();
    }

    @Operation(summary = "获取所有启用的Webhook配置")
    @GetMapping("/enabled")
    public Result<List<SysWebhookConfig>> getEnabledWebhookConfigs() {
        return Result.success(webhookConfigService.getEnabledWebhookConfigs());
    }
}
