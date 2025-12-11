package com.basebackend.logging.config;

import com.basebackend.logging.audit.config.AuditProperties;
import com.basebackend.logging.masking.MaskingProperties;
import com.basebackend.logging.statistics.config.StatisticsProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;

/**
 * 日志模块统一配置属性
 * 
 * P1优化：统一配置管理，整合所有子模块配置
 * 
 * 配置前缀：basebackend.logging
 * 
 * 使用示例：
 * <pre>
 * basebackend:
 *   logging:
 *     enabled: true
 *     audit:
 *       enabled: true
 *       storage-path: logs/audit
 *     masking:
 *       enabled: true
 *     statistics:
 *       enabled: true
 * </pre>
 * 
 * @author basebackend team
 * @since 2025-12-08
 */
@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "basebackend.logging")
public class LoggingUnifiedProperties {

    /**
     * 是否启用日志模块
     */
    private boolean enabled = true;

    /**
     * 审计配置
     */
    @NotNull
    @NestedConfigurationProperty
    private AuditProperties audit = new AuditProperties();

    /**
     * 脱敏配置
     */
    @NotNull
    @NestedConfigurationProperty
    private MaskingProperties masking = new MaskingProperties();

    /**
     * 统计配置
     */
    @NotNull
    @NestedConfigurationProperty
    private StatisticsProperties statistics = new StatisticsProperties();

    /**
     * 全局性能配置
     */
    @NotNull
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 全局监控配置
     */
    @NotNull
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * 初始化后验证配置
     */
    @PostConstruct
    public void init() {
        validateAll();
        log.info("日志模块配置加载完成: enabled={}, audit={}, masking={}, statistics={}",
                enabled, audit.isEnabled(), masking.isEnabled(), statistics.isEnabled());
    }

    /**
     * 验证所有配置
     */
    public void validateAll() {
        if (!enabled) {
            log.warn("日志模块已禁用");
            return;
        }

        // 验证审计配置
        if (audit.isEnabled()) {
            audit.validate();
        }

        // 验证统计配置
        if (statistics.isEnabled()) {
            statistics.validate();
        }

        // 验证性能配置
        performance.validate();

        // 验证监控配置
        monitoring.validate();

        log.debug("日志模块配置验证通过");
    }

    /**
     * 检查是否启用审计
     */
    public boolean isAuditEnabled() {
        return enabled && audit.isEnabled();
    }

    /**
     * 检查是否启用脱敏
     */
    public boolean isMaskingEnabled() {
        return enabled && masking.isEnabled();
    }

    /**
     * 检查是否启用统计
     */
    public boolean isStatisticsEnabled() {
        return enabled && statistics.isEnabled();
    }

    /**
     * 全局性能配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 异步处理线程池大小
         */
        private int asyncPoolSize = Runtime.getRuntime().availableProcessors();

        /**
         * 异步队列容量
         */
        private int asyncQueueCapacity = 10000;

        /**
         * 是否启用性能监控
         */
        private boolean enableMetrics = true;

        /**
         * 慢操作阈值（毫秒）
         */
        private long slowThresholdMs = 100;

        /**
         * 内存使用率告警阈值
         */
        private double memoryAlertThreshold = 0.85;

        /**
         * 验证配置
         */
        public void validate() {
            if (asyncPoolSize < 1) {
                throw new IllegalArgumentException("异步线程池大小必须大于0");
            }
            if (asyncQueueCapacity < 100) {
                throw new IllegalArgumentException("异步队列容量必须大于100");
            }
            if (slowThresholdMs < 1) {
                throw new IllegalArgumentException("慢操作阈值必须大于0");
            }
            if (memoryAlertThreshold < 0.5 || memoryAlertThreshold > 0.99) {
                throw new IllegalArgumentException("内存告警阈值必须在0.5-0.99之间");
            }
        }
    }

    /**
     * 全局监控配置
     */
    @Data
    public static class MonitoringConfig {
        /**
         * 是否启用健康检查
         */
        private boolean enableHealthCheck = true;

        /**
         * 健康检查间隔（秒）
         */
        private int healthCheckIntervalSeconds = 30;

        /**
         * 是否启用告警
         */
        private boolean enableAlerts = true;

        /**
         * 告警Webhook URL
         */
        private String alertWebhookUrl = "";

        /**
         * 是否启用Prometheus指标导出
         */
        private boolean enablePrometheus = true;

        /**
         * 验证配置
         */
        public void validate() {
            if (healthCheckIntervalSeconds < 5) {
                throw new IllegalArgumentException("健康检查间隔必须大于5秒");
            }
        }
    }
}
