package com.basebackend.database.health.alert;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警通知服务
 * 负责发送各类数据库告警通知
 */
@Slf4j
@Service
public class AlertNotificationService {

    private final DatabaseEnhancedProperties properties;

    /**
     * 告警历史记录（用于防止重复告警）
     */
    private final Map<String, AlertRecord> alertHistory = new ConcurrentHashMap<>();

    /**
     * 告警冷却时间（毫秒）- 同一告警在此时间内不会重复发送
     */
    private static final long ALERT_COOLDOWN_MS = 5 * 60 * 1000; // 5分钟

    public AlertNotificationService(DatabaseEnhancedProperties properties) {
        this.properties = properties;
    }

    /**
     * 发送慢查询告警
     * 
     * @param sql SQL语句
     * @param executionTime 执行时间（毫秒）
     * @param threshold 阈值
     */
    public void sendSlowQueryAlert(String sql, long executionTime, long threshold) {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        String alertKey = "slow_query:" + sql.hashCode();
        
        if (shouldSendAlert(alertKey)) {
            String message = String.format(
                "Slow query detected!\n" +
                "Execution time: %dms (threshold: %dms)\n" +
                "SQL: %s",
                executionTime, threshold, sql
            );

            sendAlert(AlertLevel.WARNING, "Slow Query Alert", message);
            recordAlert(alertKey);
        }
    }

    /**
     * 发送连接池使用率告警
     * 
     * @param usageRate 使用率（百分比）
     * @param activeCount 活跃连接数
     * @param maxCount 最大连接数
     * @param threshold 阈值
     */
    public void sendConnectionPoolAlert(double usageRate, int activeCount, int maxCount, int threshold) {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        String alertKey = "connection_pool_usage";
        
        if (shouldSendAlert(alertKey)) {
            String message = String.format(
                "Connection pool usage is high!\n" +
                "Usage rate: %.2f%% (threshold: %d%%)\n" +
                "Active connections: %d / %d",
                usageRate, threshold, activeCount, maxCount
            );

            sendAlert(AlertLevel.WARNING, "Connection Pool Alert", message);
            recordAlert(alertKey);
        }
    }

    /**
     * 发送数据源连接失败告警
     * 
     * @param dataSourceName 数据源名称
     * @param errorMessage 错误信息
     */
    public void sendDataSourceFailureAlert(String dataSourceName, String errorMessage) {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        String alertKey = "datasource_failure:" + dataSourceName;
        
        if (shouldSendAlert(alertKey)) {
            String message = String.format(
                "Data source connection failed!\n" +
                "Data source: %s\n" +
                "Error: %s",
                dataSourceName, errorMessage
            );

            sendAlert(AlertLevel.ERROR, "Data Source Failure Alert", message);
            recordAlert(alertKey);
        }
    }

    /**
     * 发送数据源健康状态变化告警
     * 
     * @param dataSourceName 数据源名称
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    public void sendHealthStatusChangeAlert(String dataSourceName, String oldStatus, String newStatus) {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        String alertKey = "health_status_change:" + dataSourceName;
        
        if (shouldSendAlert(alertKey)) {
            String message = String.format(
                "Data source health status changed!\n" +
                "Data source: %s\n" +
                "Status: %s -> %s",
                dataSourceName, oldStatus, newStatus
            );

            AlertLevel level = "DOWN".equals(newStatus) ? AlertLevel.ERROR : AlertLevel.WARNING;
            sendAlert(level, "Health Status Change Alert", message);
            recordAlert(alertKey);
        }
    }

    /**
     * 发送通用告警
     * 
     * @param level 告警级别
     * @param title 告警标题
     * @param message 告警消息
     */
    public void sendAlert(AlertLevel level, String title, String message) {
        // 记录告警日志
        switch (level) {
            case ERROR:
                log.error("ALERT [{}]: {}\n{}", level, title, message);
                break;
            case WARNING:
                log.warn("ALERT [{}]: {}\n{}", level, title, message);
                break;
            case INFO:
                log.info("ALERT [{}]: {}\n{}", level, title, message);
                break;
        }

        // TODO: 集成实际的告警通知渠道
        // 1. 邮件通知
        // emailService.sendAlert(level, title, message);
        
        // 2. 短信通知
        // smsService.sendAlert(level, title, message);
        
        // 3. 钉钉/企业微信通知
        // dingTalkService.sendAlert(level, title, message);
        
        // 4. Webhook通知
        // webhookService.sendAlert(level, title, message);
    }

    /**
     * 检查是否应该发送告警（防止重复告警）
     */
    private boolean shouldSendAlert(String alertKey) {
        AlertRecord record = alertHistory.get(alertKey);
        
        if (record == null) {
            return true;
        }

        long timeSinceLastAlert = System.currentTimeMillis() - record.getTimestamp();
        return timeSinceLastAlert >= ALERT_COOLDOWN_MS;
    }

    /**
     * 记录告警
     */
    private void recordAlert(String alertKey) {
        alertHistory.put(alertKey, new AlertRecord(alertKey, System.currentTimeMillis()));
    }

    /**
     * 获取告警历史
     */
    public Map<String, AlertRecord> getAlertHistory() {
        return new ConcurrentHashMap<>(alertHistory);
    }

    /**
     * 清除告警历史
     */
    public void clearAlertHistory() {
        alertHistory.clear();
        log.info("Alert history cleared");
    }

    /**
     * 清除过期的告警记录
     */
    public void cleanupExpiredAlerts() {
        long now = System.currentTimeMillis();
        alertHistory.entrySet().removeIf(entry -> 
            now - entry.getValue().getTimestamp() > ALERT_COOLDOWN_MS * 2
        );
    }

    /**
     * 告警级别
     */
    public enum AlertLevel {
        INFO,
        WARNING,
        ERROR
    }

    /**
     * 告警记录
     */
    public static class AlertRecord {
        private final String alertKey;
        private final long timestamp;
        private final LocalDateTime alertTime;

        public AlertRecord(String alertKey, long timestamp) {
            this.alertKey = alertKey;
            this.timestamp = timestamp;
            this.alertTime = LocalDateTime.now();
        }

        public String getAlertKey() {
            return alertKey;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public LocalDateTime getAlertTime() {
            return alertTime;
        }

        @Override
        public String toString() {
            return String.format("AlertRecord{alertKey='%s', alertTime=%s}", alertKey, alertTime);
        }
    }
}
