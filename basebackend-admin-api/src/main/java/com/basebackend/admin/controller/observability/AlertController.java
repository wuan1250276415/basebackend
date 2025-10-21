package com.basebackend.admin.controller.observability;

import com.basebackend.admin.service.observability.AlertManagementService;
import com.basebackend.common.model.Result;
import com.basebackend.observability.alert.AlertEvent;
import com.basebackend.observability.alert.AlertRule;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/alerts")
@RequiredArgsConstructor
@Validated
@Tag(name = "可观测性-告警", description = "告警管理相关接口")
public class AlertController {

    private final AlertManagementService alertManagementService;

    @PostMapping("/rules")
    @Operation(summary = "注册告警规则")
    public Result<Void> registerAlertRule(@RequestBody AlertRule rule) {
        log.info("Registering alert rule: {}", rule.getRuleName());
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
    public Result<List<AlertRule>> getAllAlertRules() {
        List<AlertRule> rules = alertManagementService.getAllAlertRules();
        return Result.success(rules);
    }

    @GetMapping("/events")
    @Operation(summary = "获取最近的告警事件")
    public Result<List<AlertEvent>> getRecentAlerts() {
        List<AlertEvent> alerts = alertManagementService.getRecentAlerts();
        return Result.success(alerts);
    }

    @PostMapping("/rules/test")
    @Operation(summary = "测试告警规则")
    public Result<Object> testAlertRule(@RequestBody AlertRule rule) {
        log.info("Testing alert rule: {}", rule.getRuleName());
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
