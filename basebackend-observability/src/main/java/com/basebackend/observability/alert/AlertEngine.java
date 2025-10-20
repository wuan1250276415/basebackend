package com.basebackend.observability.alert;

import com.basebackend.observability.alert.notifier.AlertNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 告警引擎
 * 负责定期评估告警规则，触发告警并发送通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEngine {

    private final AlertEvaluator alertEvaluator;
    private final List<AlertNotifier> notifiers;

    // 告警规则缓存（实际应该从数据库加载）
    private final Map<Long, AlertRule> ruleCache = new ConcurrentHashMap<>();

    // 告警事件历史（最近触发的告警）
    private final Map<Long, AlertEvent> recentAlerts = new ConcurrentHashMap<>();

    // 告警抑制时间（分钟）- 防止频繁发送相同告警
    private static final long ALERT_SUPPRESSION_MINUTES = 5;

    /**
     * 定期评估告警规则（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void evaluateAlertRules() {
        log.debug("Starting alert rule evaluation - total rules: {}", ruleCache.size());

        Collection<AlertRule> rules = ruleCache.values();
        if (rules.isEmpty()) {
            log.debug("No alert rules configured");
            return;
        }

        for (AlertRule rule : rules) {
            try {
                evaluateAndNotify(rule);
            } catch (Exception e) {
                log.error("Error evaluating alert rule - ruleId: {}, ruleName: {}, error: {}",
                        rule.getId(), rule.getRuleName(), e.getMessage(), e);
            }
        }

        // 清理过期的告警事件
        cleanupOldAlerts();
    }

    /**
     * 评估规则并发送通知
     */
    private void evaluateAndNotify(AlertRule rule) {
        // 评估规则
        AlertEvaluator.EvaluationResult result = alertEvaluator.evaluate(rule);

        if (!result.isTriggered()) {
            log.debug("Alert rule not triggered - ruleId: {}, ruleName: {}, reason: {}",
                    rule.getId(), rule.getRuleName(), result.getMessage());
            return;
        }

        // 检查是否在抑制期内
        if (isInSuppressionPeriod(rule.getId())) {
            log.debug("Alert suppressed - ruleId: {}, ruleName: {}",
                    rule.getId(), rule.getRuleName());
            return;
        }

        // 创建告警事件
        AlertEvent event = AlertEvent.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .severity(rule.getSeverity())
                .message(result.getMessage())
                .triggerValue(result.getCurrentValue())
                .thresholdValue(result.getThresholdValue())
                .alertTime(LocalDateTime.now())
                .metadata(result.getMetadata())
                .notifyChannels(rule.getNotifyChannels())
                .notifyStatus(AlertEvent.NotifyStatus.PENDING)
                .status(AlertEvent.AlertStatus.TRIGGERED)
                .build();

        log.info("Alert triggered - ruleId: {}, ruleName: {}, severity: {}, message: {}",
                rule.getId(), rule.getRuleName(), rule.getSeverity(), result.getMessage());

        // 发送通知
        sendNotifications(event);

        // 记录到历史
        recentAlerts.put(rule.getId(), event);
    }

    /**
     * 发送告警通知
     */
    private void sendNotifications(AlertEvent event) {
        String[] channels = event.getNotifyChannels().split(",");
        boolean anySuccess = false;

        for (String channel : channels) {
            channel = channel.trim();
            Optional<AlertNotifier> notifier = findNotifier(channel);

            if (!notifier.isPresent()) {
                log.warn("Notifier not found for channel: {}", channel);
                continue;
            }

            AlertNotifier alertNotifier = notifier.get();
            if (!alertNotifier.isAvailable()) {
                log.warn("Notifier not available - channel: {}", channel);
                continue;
            }

            try {
                boolean success = alertNotifier.sendAlert(event);
                if (success) {
                    anySuccess = true;
                    log.info("Alert notification sent - channel: {}, ruleId: {}, ruleName: {}",
                            channel, event.getRuleId(), event.getRuleName());
                }
            } catch (Exception e) {
                log.error("Failed to send alert notification - channel: {}, ruleId: {}, error: {}",
                        channel, event.getRuleId(), e.getMessage(), e);
            }
        }

        // 更新通知状态
        event.setNotifyStatus(anySuccess ?
                AlertEvent.NotifyStatus.SUCCESS : AlertEvent.NotifyStatus.FAILED);
        event.setStatus(anySuccess ?
                AlertEvent.AlertStatus.NOTIFIED : AlertEvent.AlertStatus.TRIGGERED);
    }

    /**
     * 查找通知器
     */
    private Optional<AlertNotifier> findNotifier(String channel) {
        return notifiers.stream()
                .filter(notifier -> notifier.getNotifierType().equalsIgnoreCase(channel))
                .findFirst();
    }

    /**
     * 检查是否在抑制期内
     */
    private boolean isInSuppressionPeriod(Long ruleId) {
        AlertEvent lastAlert = recentAlerts.get(ruleId);
        if (lastAlert == null) {
            return false;
        }

        LocalDateTime lastAlertTime = lastAlert.getAlertTime();
        LocalDateTime suppressionEnd = lastAlertTime.plusMinutes(ALERT_SUPPRESSION_MINUTES);

        return LocalDateTime.now().isBefore(suppressionEnd);
    }

    /**
     * 清理过期的告警事件（保留最近24小时）
     */
    private void cleanupOldAlerts() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        List<Long> expiredKeys = recentAlerts.entrySet().stream()
                .filter(entry -> entry.getValue().getAlertTime().isBefore(cutoffTime))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        expiredKeys.forEach(recentAlerts::remove);

        if (!expiredKeys.isEmpty()) {
            log.debug("Cleaned up {} expired alert events", expiredKeys.size());
        }
    }

    /**
     * 注册告警规则
     */
    public void registerRule(AlertRule rule) {
        if (rule == null || rule.getId() == null) {
            log.warn("Cannot register null rule or rule without ID");
            return;
        }

        ruleCache.put(rule.getId(), rule);
        log.info("Alert rule registered - ruleId: {}, ruleName: {}", rule.getId(), rule.getRuleName());
    }

    /**
     * 取消注册告警规则
     */
    public void unregisterRule(Long ruleId) {
        AlertRule removed = ruleCache.remove(ruleId);
        if (removed != null) {
            log.info("Alert rule unregistered - ruleId: {}, ruleName: {}",
                    ruleId, removed.getRuleName());
        }
    }

    /**
     * 获取所有注册的规则
     */
    public Collection<AlertRule> getAllRules() {
        return new ArrayList<>(ruleCache.values());
    }

    /**
     * 获取最近的告警事件
     */
    public Collection<AlertEvent> getRecentAlerts() {
        return new ArrayList<>(recentAlerts.values());
    }

    /**
     * 手动触发规则评估（用于测试）
     */
    public AlertEvaluator.EvaluationResult testRule(AlertRule rule) {
        return alertEvaluator.evaluate(rule);
    }
}
