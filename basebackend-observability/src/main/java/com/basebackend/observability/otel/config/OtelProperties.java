package com.basebackend.observability.otel.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenTelemetry 配置属性
 * <p>
 * 支持双栈配置，允许 OpenTelemetry 与 Micrometer/Brave 共存，
 * 为平滑迁移提供灵活的配置选项。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Getter
@ConfigurationProperties(prefix = "observability.otel")
public class OtelProperties {

    /**
     * 是否启用 OpenTelemetry 集成
     */
    private boolean enabled = false;

    /**
     * 服务相关配置
     */
    private Service service = new Service();

    /**
     * OTLP 导出器配置
     */
    private Otlp otlp = new Otlp();

    /**
     * 桥接器配置
     */
    private Bridge bridge = new Bridge();

    /**
     * 采样率 (0.0 - 1.0)，默认 1.0 表示 100% 采样
     */
    private double samplingRatio = 1.0d;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public void setOtlp(Otlp otlp) {
        this.otlp = otlp;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }

    public void setSamplingRatio(double samplingRatio) {
        if (samplingRatio < 0.0 || samplingRatio > 1.0) {
            throw new IllegalArgumentException("采样率必须在 0.0 到 1.0 之间");
        }
        this.samplingRatio = samplingRatio;
    }

    /**
     * 服务配置
     */
    public static final class Service {
        /**
         * 服务名称，默认从 spring.application.name 获取
         */
        private String name;

        /**
         * 服务版本
         */
        private String version;

        /**
         * 部署环境（dev/test/staging/prod）
         */
        private String environment;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }
    }

    /**
     * OTLP 配置
     */
    public static final class Otlp {
        /**
         * OTLP 端点地址（gRPC）
         */
        private String endpoint = "http://localhost:4317";

        /**
         * 指标导出器配置
         */
        private Exporter metrics = new Exporter();

        /**
         * 追踪导出器配置
         */
        private Exporter traces = new Exporter();

        /**
         * 日志导出器配置
         */
        private Exporter logs = new Exporter();

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Exporter getMetrics() {
            return metrics;
        }

        public void setMetrics(Exporter metrics) {
            this.metrics = metrics;
        }

        public Exporter getTraces() {
            return traces;
        }

        public void setTraces(Exporter traces) {
            this.traces = traces;
        }

        public Exporter getLogs() {
            return logs;
        }

        public void setLogs(Exporter logs) {
            this.logs = logs;
        }
    }

    /**
     * 桥接器配置
     */
    public static final class Bridge {
        /**
         * 是否启用 Micrometer 桥接
         */
        private boolean micrometer = true;

        /**
         * 是否启用 Brave 桥接
         */
        private boolean brave = true;

        public boolean isMicrometer() {
            return micrometer;
        }

        public void setMicrometer(boolean micrometer) {
            this.micrometer = micrometer;
        }

        public boolean isBrave() {
            return brave;
        }

        public void setBrave(boolean brave) {
            this.brave = brave;
        }
    }

    /**
     * 导出器配置
     */
    public static final class Exporter {
        /**
         * 是否启用该导出器
         */
        private boolean enabled = true;

        public Exporter() {
        }

        public Exporter(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
