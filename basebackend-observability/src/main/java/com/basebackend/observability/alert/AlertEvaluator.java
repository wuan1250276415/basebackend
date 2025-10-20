package com.basebackend.observability.alert;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 告警评估器
 * 评估告警规则是否触发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluator {

    private final MeterRegistry meterRegistry;

    /**
     * 评估告警规则
     *
     * @param rule 告警规则
     * @return 评估结果，包含是否触发、当前值、阈值等信息
     */
    public EvaluationResult evaluate(AlertRule rule) {
        if (!rule.getEnabled()) {
            return EvaluationResult.notTriggered("规则未启用");
        }

        try {
            switch (rule.getRuleType()) {
                case THRESHOLD:
                    return evaluateThresholdRule(rule);
                case LOG:
                    return evaluateLogRule(rule);
                case CUSTOM:
                    return evaluateCustomRule(rule);
                default:
                    return EvaluationResult.notTriggered("未知规则类型");
            }
        } catch (Exception e) {
            log.error("Failed to evaluate alert rule - ruleId: {}, ruleName: {}, error: {}",
                    rule.getId(), rule.getRuleName(), e.getMessage(), e);
            return EvaluationResult.error("评估失败: " + e.getMessage());
        }
    }

    /**
     * 评估阈值类型告警
     */
    private EvaluationResult evaluateThresholdRule(AlertRule rule) {
        String metricName = rule.getMetricName();
        if (metricName == null || metricName.isEmpty()) {
            return EvaluationResult.error("指标名称为空");
        }

        // 从 MeterRegistry 中查询指标值
        Double currentValue = getMetricValue(metricName);
        if (currentValue == null) {
            return EvaluationResult.notTriggered("指标不存在或无数据");
        }

        Double thresholdValue = rule.getThresholdValue();
        String operator = rule.getComparisonOperator();

        boolean triggered = compareValues(currentValue, thresholdValue, operator);

        if (triggered) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("metricName", metricName);
            metadata.put("operator", operator);

            return EvaluationResult.triggered(
                currentValue.toString(),
                thresholdValue.toString(),
                String.format("指标 %s 当前值 %s %s 阈值 %s",
                    metricName, currentValue, operator, thresholdValue),
                metadata
            );
        }

        return EvaluationResult.notTriggered("未达到阈值");
    }

    /**
     * 评估日志类型告警
     */
    private EvaluationResult evaluateLogRule(AlertRule rule) {
        // 日志告警需要查询日志系统（Loki）
        // 这里简化实现，实际应该查询 Loki API

        // 检查错误日志计数
        String metricName = "log.error.count";
        Double errorCount = getMetricValue(metricName);

        if (errorCount == null) {
            return EvaluationResult.notTriggered("日志指标不存在");
        }

        Double thresholdValue = rule.getThresholdValue() != null ? rule.getThresholdValue() : 10.0;

        if (errorCount > thresholdValue) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("errorCount", errorCount);

            return EvaluationResult.triggered(
                errorCount.toString(),
                thresholdValue.toString(),
                String.format("错误日志数量 %s 超过阈值 %s", errorCount, thresholdValue),
                metadata
            );
        }

        return EvaluationResult.notTriggered("日志告警未触发");
    }

    /**
     * 评估自定义告警
     */
    private EvaluationResult evaluateCustomRule(AlertRule rule) {
        // 自定义告警规则的实现
        // 这里可以根据具体需求实现复杂的告警逻辑
        log.warn("Custom alert rule evaluation not implemented - ruleId: {}", rule.getId());
        return EvaluationResult.notTriggered("自定义规则未实现");
    }

    /**
     * 从 MeterRegistry 获取指标值
     */
    private Double getMetricValue(String metricName) {
        try {
            return Search.in(meterRegistry)
                    .name(metricName)
                    .meters()
                    .stream()
                    .findFirst()
                    .map(meter -> {
                        // 根据 Meter 类型获取值
                        switch (meter.getId().getType()) {
                            case COUNTER:
                                return meterRegistry.counter(metricName).count();
                            case GAUGE:
                                return meterRegistry.gauge(metricName, 0.0);
                            case TIMER:
                                return meterRegistry.timer(metricName).mean(java.util.concurrent.TimeUnit.MILLISECONDS);
                            default:
                                return null;
                        }
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to get metric value - metricName: {}, error: {}",
                    metricName, e.getMessage());
            return null;
        }
    }

    /**
     * 比较数值
     */
    private boolean compareValues(Double current, Double threshold, String operator) {
        if (current == null || threshold == null || operator == null) {
            return false;
        }

        switch (operator) {
            case ">":
                return current > threshold;
            case ">=":
                return current >= threshold;
            case "<":
                return current < threshold;
            case "<=":
                return current <= threshold;
            case "==":
            case "=":
                return Math.abs(current - threshold) < 0.0001;
            default:
                log.warn("Unknown comparison operator: {}", operator);
                return false;
        }
    }

    /**
     * 评估结果
     */
    public static class EvaluationResult {
        private final boolean triggered;
        private final String currentValue;
        private final String thresholdValue;
        private final String message;
        private final Map<String, Object> metadata;

        private EvaluationResult(boolean triggered, String currentValue, String thresholdValue,
                                String message, Map<String, Object> metadata) {
            this.triggered = triggered;
            this.currentValue = currentValue;
            this.thresholdValue = thresholdValue;
            this.message = message;
            this.metadata = metadata;
        }

        public static EvaluationResult triggered(String currentValue, String thresholdValue,
                                                 String message, Map<String, Object> metadata) {
            return new EvaluationResult(true, currentValue, thresholdValue, message, metadata);
        }

        public static EvaluationResult notTriggered(String message) {
            return new EvaluationResult(false, null, null, message, null);
        }

        public static EvaluationResult error(String message) {
            return new EvaluationResult(false, null, null, message, null);
        }

        public boolean isTriggered() {
            return triggered;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public String getThresholdValue() {
            return thresholdValue;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
