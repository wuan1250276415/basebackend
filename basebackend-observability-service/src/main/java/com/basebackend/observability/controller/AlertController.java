package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.service.AlertManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警管理控制器
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Validated
@Tag(name = "告警管理", description = "告警管理相关接口")
public class AlertController {

    private final AlertManagementService alertManagementService;

    @PostMapping("/rules")
    @Operation(summary = "注册告警规则")
    public Result<Void> registerAlertRule(@RequestBody Map<String, Object> rule) {
        log.info("Registering alert rule: {}", rule.get("ruleName"));
        alertManagementService.registerAlertRule(rule);
        return Result.success();
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "删除告警规则")
    public Result<Void> deleteAlertRule(@PathVariable Long ruleId) {
        log.info("Deleting alert rule: {}", ruleId);
        alertManagementService.unregisterAlertRule(ruleId);
        return Result.success();
    }

    @GetMapping("/rules")
    @Operation(summary = "获取所有告警规则")
    public Result<List<Map<String, Object>>> getAllAlertRules() {
        List<Map<String, Object>> rules = alertManagementService.getAllAlertRules();
        return Result.success(rules);
    }

    @GetMapping("/events")
    @Operation(summary = "获取最近的告警事件")
    public Result<List<Map<String, Object>>> getRecentAlerts() {
        List<Map<String, Object>> alerts = alertManagementService.getRecentAlerts();
        return Result.success(alerts);
    }

    @PostMapping("/rules/test")
    @Operation(summary = "测试告警规则")
    public Result<Object> testAlertRule(@RequestBody Map<String, Object> rule) {
        log.info("Testing alert rule: {}", rule.get("ruleName"));
        Object result = alertManagementService.testAlertRule(rule);
        return Result.success(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取告警统计")
    public Result<Map<String, Object>> getAlertStats() {
        Map<String, Object> stats = alertManagementService.getAlertStats();
        return Result.success(stats);
    }
}
