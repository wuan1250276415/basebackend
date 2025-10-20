package com.basebackend.admin.service.observability;

import com.basebackend.observability.alert.AlertEngine;
import com.basebackend.observability.alert.AlertEvent;
import com.basebackend.observability.alert.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertManagementService {

    private final AlertEngine alertEngine;

    /**
     * 注册告警规则
     */
    public void registerAlertRule(AlertRule rule) {
        log.info("Registering alert rule: {}", rule.getRuleName());
        alertEngine.registerRule(rule);
    }

    /**
     * 取消注册告警规则
     */
    public void unregisterAlertRule(Long ruleId) {
        log.info("Unregistering alert rule: {}", ruleId);
        alertEngine.unregisterRule(ruleId);
    }

    /**
     * 获取所有告警规则
     */
    public List<AlertRule> getAllAlertRules() {
        return alertEngine.getAllRules().stream().collect(Collectors.toList());
    }

    /**
     * 获取最近的告警事件
     */
    public List<AlertEvent> getRecentAlerts() {
        return alertEngine.getRecentAlerts().stream().collect(Collectors.toList());
    }

    /**
     * 测试告警规则
     */
    public Object testAlertRule(AlertRule rule) {
        log.info("Testing alert rule: {}", rule.getRuleName());
        return alertEngine.testRule(rule);
    }

    /**
     * 获取告警统计
     */
    public java.util.Map<String, Object> getAlertStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        Collection<AlertEvent> recentAlerts = alertEngine.getRecentAlerts();
        stats.put("totalAlerts", recentAlerts.size());

        // 按级别统计
        java.util.Map<String, Long> severityCount = recentAlerts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                alert -> alert.getSeverity().toString(),
                java.util.stream.Collectors.counting()
            ));
        stats.put("bySeverity", severityCount);

        // 按状态统计
        java.util.Map<String, Long> statusCount = recentAlerts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                alert -> alert.getStatus().toString(),
                java.util.stream.Collectors.counting()
            ));
        stats.put("byStatus", statusCount);

        // 通知成功率
        long successCount = recentAlerts.stream()
            .filter(alert -> alert.getNotifyStatus() == AlertEvent.NotifyStatus.SUCCESS)
            .count();
        double successRate = recentAlerts.isEmpty() ? 0 : (double) successCount / recentAlerts.size() * 100;
        stats.put("notifySuccessRate", String.format("%.2f", successRate) + "%");

        return stats;
    }
}
