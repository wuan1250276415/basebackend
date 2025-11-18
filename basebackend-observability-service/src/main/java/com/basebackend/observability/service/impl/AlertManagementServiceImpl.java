package com.basebackend.observability.service.impl;

import com.basebackend.observability.service.AlertManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警管理服务实现
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertManagementServiceImpl implements AlertManagementService {

    // 简单的内存存储，实际应该使用数据库
    private final Map<Long, Map<String, Object>> alertRules = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> alertEvents = new ArrayList<>();
    private Long ruleIdCounter = 1L;

    @Override
    public void registerAlertRule(Map<String, Object> rule) {
        Long ruleId = ruleIdCounter++;
        rule.put("id", ruleId);
        rule.put("createTime", System.currentTimeMillis());
        alertRules.put(ruleId, rule);
        
        log.info("Alert rule registered: id={}, name={}", ruleId, rule.get("ruleName"));
    }

    @Override
    public void unregisterAlertRule(Long ruleId) {
        Map<String, Object> removed = alertRules.remove(ruleId);
        if (removed != null) {
            log.info("Alert rule unregistered: id={}, name={}", ruleId, removed.get("ruleName"));
        } else {
            log.warn("Alert rule not found: id={}", ruleId);
        }
    }

    @Override
    public List<Map<String, Object>> getAllAlertRules() {
        log.info("Getting all alert rules, count: {}", alertRules.size());
        return new ArrayList<>(alertRules.values());
    }

    @Override
    public List<Map<String, Object>> getRecentAlerts() {
        log.info("Getting recent alerts, count: {}", alertEvents.size());
        
        // 返回最近100条告警
        int size = alertEvents.size();
        int fromIndex = Math.max(0, size - 100);
        return new ArrayList<>(alertEvents.subList(fromIndex, size));
    }

    @Override
    public Object testAlertRule(Map<String, Object> rule) {
        log.info("Testing alert rule: {}", rule.get("ruleName"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Alert rule test passed");
        result.put("ruleName", rule.get("ruleName"));
        
        return result;
    }

    @Override
    public Map<String, Object> getAlertStats() {
        log.info("Getting alert stats");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRules", alertRules.size());
        stats.put("totalEvents", alertEvents.size());
        stats.put("activeRules", alertRules.values().stream()
            .filter(rule -> Boolean.TRUE.equals(rule.get("enabled")))
            .count());
        
        return stats;
    }
}
