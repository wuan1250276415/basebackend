package com.basebackend.logging.monitoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 监控系统配置属性
 *
 * 从 application.yml 中读取监控系统相关配置。
 * 支持 Prometheus、Grafana、告警阈值等配置。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Data
@Validated
@ConfigurationProperties(prefix = "basebackend.logging.monitoring")
public class MonitoringProperties {

    /**
     * 是否启用监控功能
     */
    private boolean enabled = true;

    /**
     * Prometheus 配置
     */
    private Prometheus prometheus = new Prometheus();

    /**
     * Grafana 配置
     */
    private Grafana grafana = new Grafana();

    /**
     * 数据采集间隔
     */
    @NotNull
    private Duration scrapeInterval = Duration.ofSeconds(10);

    /**
     * 告警阈值配置
     */
    private Thresholds thresholds = new Thresholds();

    /**
     * 功能开关
     */
    private Toggles toggles = new Toggles();

    /**
     * 数据源配置
     */
    private DataSource dataSource = new DataSource();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    /**
     * Prometheus 配置
     */
    @Data
    public static class Prometheus {
        /**
         * Prometheus 端口
         */
        @Min(1)
        private int port = 9090;

        /**
         * 指标路径
         */
        @NotBlank
        private String path = "/actuator/prometheus";

        /**
         * 命名空间标签
         */
        private String namespace = "basebackend";

        /**
         * 服务名称标签
         */
        private String serviceName = "logging";

        /**
         * 是否启用原生 Prometheus 端点
         */
        private boolean nativeEndpointEnabled = true;
    }

    /**
     * Grafana 配置
     */
    @Data
    public static class Grafana {
        /**
         * 管理员用户名
         */
        @NotBlank
        private String adminUser = "admin";

        /**
         * 管理员密码
         */
        @NotBlank
        private String adminPassword = "admin";

        /**
         * 数据源名称
         */
        @NotBlank
        private String datasourceName = "Prometheus";

        /**
         * 数据源 URL
         */
        @NotBlank
        private String datasourceUrl = "http://prometheus:9090";

        /**
         * 仪表板自动导入
         */
        private boolean autoImport = true;

        /**
         * API 令牌
         */
        private String apiToken = "";

        /**
         * 仪表板刷新间隔
         */
        private String refreshInterval = "10s";
    }

    /**
     * 告警阈值配置
     */
    @Data
    public static class Thresholds {
        /**
         * 错误率阈值（百分比）
         */
        @Min(0)
        private double errorRate = 5.0;

        /**
         * P95 延迟阈值（毫秒）
         */
        @Min(0)
        private Duration latencyP95 = Duration.ofMillis(500);

        /**
         * 队列深度阈值
         */
        @Min(0)
        private int queueDepth = 1000;

        /**
         * 磁盘使用率阈值（百分比）
         */
        @Min(0)
        private double diskUsagePercent = 85.0;

        /**
         * 缓存命中率阈值（百分比）
         */
        @Min(0)
        private double cacheHitRatio = 80.0;

        /**
         * 内存使用率阈值（百分比）
         */
        @Min(0)
        private double memoryUsagePercent = 80.0;

        /**
         * CPU 使用率阈值（百分比）
         */
        @Min(0)
        private double cpuUsagePercent = 80.0;
    }

    /**
     * 功能开关
     */
    @Data
    public static class Toggles {
        /**
         * 是否启用性能指标
         */
        private boolean performanceMetrics = true;

        /**
         * 是否启用业务指标
         */
        private boolean businessMetrics = true;

        /**
         * 是否启用导出器
         */
        private boolean exporterEnabled = true;

        /**
         * 是否启用健康检查
         */
        private boolean healthCheckEnabled = true;

        /**
         * 是否启用仪表板
         */
        private boolean dashboardEnabled = true;
    }

    /**
     * 数据源配置
     */
    @Data
    public static class DataSource {
        /**
         * 自定义标签
         */
        private Map<String, String> labels;

        /**
         * 实例列表
         */
        private List<String> instances;

        /**
         * 环境标签
         */
        private String environment = "prod";

        /**
         * 版本标签
         */
        private String version = "1.0.0";
    }

    /**
     * 存储配置
     */
    @Data
    public static class Storage {
        /**
         * 数据保留期
         */
        @NotBlank
        private String retention = "15d";

        /**
         * 数据卷路径
         */
        @NotBlank
        private String volume = "/var/lib/prometheus";

        /**
         * 数据压缩
         */
        private boolean compression = true;

        /**
         * 存储类型
         */
        private String type = "tsdb";
    }

    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (enabled) {
            if (scrapeInterval == null || scrapeInterval.isNegative() || scrapeInterval.isZero()) {
                throw new IllegalArgumentException("采集间隔必须大于0");
            }

            if (thresholds.getErrorRate() < 0 || thresholds.getErrorRate() > 100) {
                throw new IllegalArgumentException("错误率阈值必须在0-100之间");
            }

            if (thresholds.getCacheHitRatio() < 0 || thresholds.getCacheHitRatio() > 100) {
                throw new IllegalArgumentException("缓存命中率阈值必须在0-100之间");
            }

            if (prometheus.getPort() < 1 || prometheus.getPort() > 65535) {
                throw new IllegalArgumentException("端口号必须在1-65535之间");
            }
        }
    }
}
