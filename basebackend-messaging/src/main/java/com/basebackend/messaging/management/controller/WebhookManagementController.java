package com.basebackend.messaging.management.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.messaging.entity.WebhookEndpointEntity;
import com.basebackend.messaging.management.dto.WebhookUpsertRequest;
import com.basebackend.messaging.management.service.WebhookManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/messaging/webhook")
@RequiredArgsConstructor
@Validated
public class WebhookManagementController {

    private final WebhookManagementService webhookManagementService;

    @GetMapping("/page")
    public Result<PageResult<WebhookEndpointEntity>> getPage(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String name) {
        try {
            return Result.success(webhookManagementService.getPage(page, size, name));
        } catch (Exception e) {
            log.error("分页查询Webhook配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<WebhookEndpointEntity> getById(@PathVariable Long id) {
        try {
            WebhookEndpointEntity entity = webhookManagementService.getById(id);
            return entity != null ? Result.success(entity) : Result.error("Webhook配置不存在");
        } catch (Exception e) {
            log.error("查询Webhook配置失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody WebhookUpsertRequest request) {
        try {
            webhookManagementService.create(request);
            return Result.success();
        } catch (Exception e) {
            log.error("创建Webhook配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WebhookUpsertRequest request) {
        try {
            webhookManagementService.update(id, request);
            return Result.success();
        } catch (Exception e) {
            log.error("更新Webhook配置失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            webhookManagementService.delete(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除Webhook配置失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        try {
            webhookManagementService.toggle(id, enabled);
            return Result.success();
        } catch (Exception e) {
            log.error("切换Webhook配置状态失败: id={}, enabled={}", id, enabled, e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/enabled")
    public Result<List<WebhookEndpointEntity>> getEnabled() {
        try {
            return Result.success(webhookManagementService.listEnabled());
        } catch (Exception e) {
            log.error("查询启用Webhook配置失败", e);
            return Result.error(e.getMessage());
        }
    }
}
