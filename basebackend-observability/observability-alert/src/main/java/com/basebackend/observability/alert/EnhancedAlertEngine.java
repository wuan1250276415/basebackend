package com.basebackend.observability.alert;

import com.basebackend.observability.alert.notifier.AlertNotifier;
import com.basebackend.observability.alert.repository.AlertRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 增强版告警引擎
 * <p>
 * 改进点：
 * - 支持从仓储加载规则（持久化）
 * - 并行评估告警规则
 * - 可配置的抑制时间
 * - 更好的错误处理
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class EnhancedAlertEngine {

    private final AlertEvaluator alertEvaluator;
    private final List<AlertNotifier> notifiers;
    private final AlertRuleRepository ruleRepository;
    private final ExecutorService alertExecutor;

    // 告警规则缓存
    private final Map<Long, AlertRule> ruleCache = new ConcurrentHashMap<>();

    // 告警事件历史（最近触发的告警）
    private final Map<Long, AlertEvent> recentAlerts = new ConcurrentHashMap<>();

    // 配置参数
    @Value("${observability.alert.suppression-minutes:5}")
    private long suppressionMinutes;

    @Value("${observability.alert.parallel-enabled:true}")
    private boolean parallelEnabled;

    @Value("${observability.alert.thread-pool-size:4}")
    private int threadPoolSize;

    @Autowired
    public EnhancedAlertEngine(AlertEvaluator alertEvaluator,
            List<AlertNotifier> notifiers,
            @Autowired(required = false) AlertRuleRepository ruleRepository) {
        this.alertEvaluator = alertEvaluator;
        this.notifiers = notifiers != null ? notifiers : Collections.emptyList();
        this.ruleRepository = ruleRepository;

        // 创建告警评估线程池
        this.alertExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "alert-executor");
            t.setDaemon(true);
            return t;
        });

        log.info("EnhancedAlertEngine initialized with {} notifiers", this.notifiers.size());
    }

    /**
     * 初始化时从仓储加载规则
     */
    @PostConstruct
    public void loadRulesFromRepository() {
        if (ruleRepository == null) {
            log.info("No AlertRuleRepository configured, using in-memory rules only");
            return;
        }

        try {
            List<AlertRule> rules = ruleRepository.findByEnabledTrue();
            rules.forEach(rule -> ruleCache.put(rule.getId(), rule));
            log.info("Loaded {} enabled alert rules from repository", rules.size());
        } catch (Exception e) {
            log.error("Failed to load alert rules from repository", e);
        }
    }

    /**
     * 定期评估告警规则（每分钟执行一次）
     */
    @Scheduled(fixedRateString = "${observability.alert.evaluation-interval:60000}", initialDelayString = "${observability.alert.initial-delay:10000}")
    public void evaluateAlertRules() {
        log.debug("Starting alert rule evaluation - total rules: {}", ruleCache.size());

        Collection<AlertRule> rules = ruleCache.values();
        if (rules.isEmpty()) {
            log.debug("No alert rules configured");
            return;
        }

        if (parallelEnabled) {
            evaluateRulesInParallel(rules);
        } else {
            evaluateRulesSequentially(rules);
        }

        // 清理过期的告警事件
        cleanupOldAlerts();
    }

    /**
     * 并行评估规则（P1改进）
     */
    private void evaluateRulesInParallel(Collection<AlertRule> rules) {
        List<CompletableFuture<Void>> futures = rules.stream()
                .map(rule -> CompletableFuture.runAsync(() -> {
                    try {
                        evaluateAndNotify(rule);
                    } catch (Exception e) {
                        log.error("Error evaluating alert rule - ruleId: {}, ruleName: {}, error: {}",
                                rule.getId(), rule.getRuleName(), e.getMessage(), e);
                    }
                }, alertExecutor))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Alert evaluation timeout - some rules may not have been evaluated");
        } catch (Exception e) {
            log.error("Error during parallel alert evaluation", e);
        }
    }

    /**
     * 顺序评估规则
     */
    private void evaluateRulesSequentially(Collection<AlertRule> rules) {
        for (AlertRule rule : rules) {
            try {
                evaluateAndNotify(rule);
            } catch (Exception e) {
                log.error("Error evaluating alert rule - ruleId: {}, ruleName: {}, error: {}",
                        rule.getId(), rule.getRuleName(), e.getMessage(), e);
            }
        }
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

        // 异步发送通知
        CompletableFuture.runAsync(() -> sendNotifications(event), alertExecutor);

        // 记录到历史
        recentAlerts.put(rule.getId(), event);
    }

    /**
     * 发送告警通知
     */
    public void sendNotifications(AlertEvent event) {
        if (event.getNotifyChannels() == null || event.getNotifyChannels().isEmpty()) {
            log.warn("No notify channels configured for alert - ruleId: {}", event.getRuleId());
            return;
        }

        String[] channels = event.getNotifyChannels().split(",");
        boolean anySuccess = false;

        for (String channel : channels) {
            channel = channel.trim();
            Optional<AlertNotifier> notifier = findNotifier(channel);

            if (notifier.isEmpty()) {
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
        event.setNotifyStatus(anySuccess ? AlertEvent.NotifyStatus.SUCCESS : AlertEvent.NotifyStatus.FAILED);
        event.setStatus(anySuccess ? AlertEvent.AlertStatus.NOTIFIED : AlertEvent.AlertStatus.TRIGGERED);
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
        LocalDateTime suppressionEnd = lastAlertTime.plusMinutes(suppressionMinutes);

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
     * 注册告警规则并持久化
     */
    public void registerRule(AlertRule rule) {
        if (rule == null) {
            log.warn("Cannot register null rule");
            return;
        }

        // 持久化到仓储
        if (ruleRepository != null) {
            rule = ruleRepository.save(rule);
        }

        ruleCache.put(rule.getId(), rule);
        log.info("Alert rule registered - ruleId: {}, ruleName: {}", rule.getId(), rule.getRuleName());
    }

    /**
     * 取消注册告警规则
     */
    public void unregisterRule(Long ruleId) {
        AlertRule removed = ruleCache.remove(ruleId);

        // 从仓储删除
        if (ruleRepository != null) {
            ruleRepository.deleteById(ruleId);
        }

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
     * 刷新规则缓存
     */
    public void refreshRuleCache() {
        if (ruleRepository != null) {
            loadRulesFromRepository();
        }
    }

    /**
     * 手动触发规则评估（用于测试）
     */
    public AlertEvaluator.EvaluationResult testRule(AlertRule rule) {
        return alertEvaluator.evaluate(rule);
    }
}
