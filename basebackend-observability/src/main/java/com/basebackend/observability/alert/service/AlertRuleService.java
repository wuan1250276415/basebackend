package com.basebackend.observability.alert.service;

import com.basebackend.observability.alert.AlertEvent;
import com.basebackend.observability.alert.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警规则服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    // 告警规则缓存
    private final Map<String, AlertRule> ruleCache = new ConcurrentHashMap<>();
    
    // 告警历史（用于去重和频率控制）
    private final Map<String, LocalDateTime> alertHistory = new ConcurrentHashMap<>();
    
    // 告警冷却时间（分钟）
    private static final int COOLDOWN_MINUTES = 10;

    /**
     * 添加告警规则
     */
    public void addRule(AlertRule rule) {
        ruleCache.put(rule.getId().toString(), rule);
        log.info("Alert rule added: {}", rule.getRuleName());
    }

    /**
     * 删除告警规则
     */
    public void removeRule(String ruleId) {
        ruleCache.remove(ruleId);
        log.info("Alert rule removed: {}", ruleId);
    }

    /**
     * 获取所有规则
     */
    public List<AlertRule> getAllRules() {
        return new ArrayList<>(ruleCache.values());
    }

    /**
     * 评估指标
     */
    public List<AlertEvent> evaluateMetric(String metricName, double value, Map<String, Object> context) {
        List<AlertEvent> events = new ArrayList<>();
        
        for (AlertRule rule : ruleCache.values()) {
            if (!rule.getEnabled()) {
                continue;
            }
            
            if (rule.getMetricName().equals(metricName)) {
                if (shouldTrigger(rule, value)) {
                    String alertKey = rule.getId().toString() + "-" + metricName;
                    
                    // 检查是否在冷却期内
                    if (isInCooldown(alertKey)) {
                        log.debug("Alert {} is in cooldown, skipping", alertKey);
                        continue;
                    }
                    
                    AlertEvent event = createAlertEvent(rule, metricName, value, context);
                    events.add(event);
                    
                    // 记录告警历史
                    alertHistory.put(alertKey, LocalDateTime.now());
                    
                    log.warn("Alert triggered: {} - {} {} {}", 
                            rule.getRuleName(), metricName, rule.getComparisonOperator(), rule.getThresholdValue());
                }
            }
        }
        
        return events;
    }

    /**
     * 判断是否应该触发告警
     */
    private boolean shouldTrigger(AlertRule rule, double value) {
        switch (rule.getComparisonOperator()) {
            case ">":
                return value > rule.getThresholdValue();
            case ">=":
                return value >= rule.getThresholdValue();
            case "<":
                return value < rule.getThresholdValue();
            case "<=":
                return value <= rule.getThresholdValue();
            case "==":
                return Math.abs(value - rule.getThresholdValue()) < 0.0001;
            case "!=":
                return Math.abs(value - rule.getThresholdValue()) >= 0.0001;
            default:
                return false;
        }
    }

    /**
     * 检查是否在冷却期
     */
    private boolean isInCooldown(String alertKey) {
        LocalDateTime lastAlert = alertHistory.get(alertKey);
        if (lastAlert == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return lastAlert.plusMinutes(COOLDOWN_MINUTES).isAfter(now);
    }

    /**
     * 创建告警事件
     */
    private AlertEvent createAlertEvent(AlertRule rule, String metricName, 
                                       double value, Map<String, Object> context) {
        return AlertEvent.builder()
                .id(System.currentTimeMillis())
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .severity(rule.getSeverity())
                .message(buildAlertMessage(rule, metricName, value))
                .triggerValue(String.valueOf(value))
                .thresholdValue(String.valueOf(rule.getThresholdValue()))
                .alertTime(LocalDateTime.now())
                .metadata(context)
                .build();
    }

    /**
     * 构建告警消息
     */
    private String buildAlertMessage(AlertRule rule, String metricName, double value) {
        return String.format("%s: %s 当前值 %.2f %s 阈值 %.2f",
                rule.getRuleName(),
                metricName,
                value,
                rule.getComparisonOperator(),
                rule.getThresholdValue());
    }

    /**
     * 初始化默认规则
     */
    public void initDefaultRules() {
        // JVM堆内存告警
        // JVM堆内存告警
        AlertRule heapRule = new AlertRule();
        heapRule.setId(1L);
        heapRule.setRuleName("堆内存使用率过高");
        heapRule.setMetricName("heap.usage.percent");
        heapRule.setComparisonOperator(">");
        heapRule.setThresholdValue(90.0);
        heapRule.setSeverity(AlertRule.AlertSeverity.CRITICAL);
        heapRule.setEnabled(true);
        addRule(heapRule);
        
        // Note: Additional default alert rules can be added here
        
        log.info("Default alert rules initialized: {} rules", ruleCache.size());
    }

    /**
     * 清理过期的告警历史
     */
    public void cleanupHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        alertHistory.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        log.debug("Alert history cleaned up");
    }
}
