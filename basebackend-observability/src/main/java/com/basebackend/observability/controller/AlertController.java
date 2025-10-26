package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.alert.AlertRule;
import com.basebackend.observability.alert.service.AlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警规则配置和管理")
public class AlertController {

    private final AlertRuleService alertRuleService;

    @GetMapping("/rules")
    @Operation(summary = "获取所有告警规则")
    public Result<List<AlertRule>> getAllRules() {
        try {
            List<AlertRule> rules = alertRuleService.getAllRules();
            return Result.success(rules);
        } catch (Exception e) {
            log.error("Failed to get alert rules", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/rules")
    @Operation(summary = "添加告警规则")
    public Result<Void> addRule(@RequestBody AlertRule rule) {
        try {
            alertRuleService.addRule(rule);
            return Result.<Void>success("告警规则添加成功", null);
        } catch (Exception e) {
            log.error("Failed to add alert rule", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "删除告警规则")
    public Result<Void> removeRule(@PathVariable String ruleId) {
        try {
            alertRuleService.removeRule(ruleId);
            return Result.<Void>success("告警规则删除成功", null);
        } catch (Exception e) {
            log.error("Failed to remove alert rule", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/rules/init-defaults")
    @Operation(summary = "初始化默认规则")
    public Result<Void> initDefaultRules() {
        try {
            alertRuleService.initDefaultRules();
            return Result.<Void>success("默认告警规则初始化成功", null);
        } catch (Exception e) {
            log.error("Failed to initialize default rules", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "清理告警历史")
    public Result<Void> cleanupHistory() {
        try {
            alertRuleService.cleanupHistory();
            return Result.<Void>success("告警历史清理成功", null);
        } catch (Exception e) {
            log.error("Failed to cleanup alert history", e);
            return Result.error(e.getMessage());
        }
    }
}
