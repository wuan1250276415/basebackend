package com.basebackend.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Job configuration properties
 */
@Data
@Component
@Validated
@RefreshScope
@ConfigurationProperties(prefix = "scheduler.job")
public class JobConfigProperties {

    private boolean autoRegister = true;

    private List<String> scanPackages = new ArrayList<>(List.of("com.basebackend.scheduler.processor"));

    @NotBlank
    private String processorBeanNameSuffix = "Processor";

    private List<String> excludePackages = new ArrayList<>(List.of(
            "**/test/**",
            "**/*Test.java",
            "**/*Tests.java"
    ));

    @Valid
    private Lifecycle lifecycle = new Lifecycle();

    @Valid
    private Metrics metrics = new Metrics();

    @Valid
    private Retry retry = new Retry();

    @Valid
    private Idempotency idempotency = new Idempotency();

    @Valid
    private Logging logging = new Logging();

    public void validate() {
        retry.validate();
        metrics.validate();
        logging.validate();
        validateScanPackages();
    }

    public String getFullIdempotencyKeyPrefix(String appName) {
        return appName + ":" + idempotency.getKeyPrefix();
    }

    public boolean isEnableLifecycleManagement() {
        return lifecycle.isEnableManagement();
    }

    public void setEnableLifecycleManagement(boolean enableLifecycleManagement) {
        lifecycle.setEnableManagement(enableLifecycleManagement);
    }

    public int getShutdownTimeoutSeconds() {
        return lifecycle.getShutdownTimeoutSeconds();
    }

    public void setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        lifecycle.setShutdownTimeoutSeconds(shutdownTimeoutSeconds);
    }

    public int getShutdownCheckIntervalSeconds() {
        return lifecycle.getShutdownCheckIntervalSeconds();
    }

    public void setShutdownCheckIntervalSeconds(int shutdownCheckIntervalSeconds) {
        lifecycle.setShutdownCheckIntervalSeconds(shutdownCheckIntervalSeconds);
    }

    public boolean isEnableMetrics() {
        return metrics.isEnable();
    }

    public void setEnableMetrics(boolean enableMetrics) {
        metrics.setEnable(enableMetrics);
    }

    public int getMetricsIntervalSeconds() {
        return metrics.getIntervalSeconds();
    }

    public void setMetricsIntervalSeconds(int metricsIntervalSeconds) {
        metrics.setIntervalSeconds(metricsIntervalSeconds);
    }

    public int getMetricsRetentionDays() {
        return metrics.getRetentionDays();
    }

    public void setMetricsRetentionDays(int metricsRetentionDays) {
        metrics.setRetentionDays(metricsRetentionDays);
    }

    public int getDefaultRetryTimes() {
        return retry.getDefaultTimes();
    }

    public void setDefaultRetryTimes(int defaultRetryTimes) {
        retry.setDefaultTimes(defaultRetryTimes);
    }

    public int getRetryIntervalSeconds() {
        return retry.getIntervalSeconds();
    }

    public void setRetryIntervalSeconds(int retryIntervalSeconds) {
        retry.setIntervalSeconds(retryIntervalSeconds);
    }

    public int getMaxRetryIntervalSeconds() {
        return retry.getMaxIntervalSeconds();
    }

    public void setMaxRetryIntervalSeconds(int maxRetryIntervalSeconds) {
        retry.setMaxIntervalSeconds(maxRetryIntervalSeconds);
    }

    public boolean isEnableExponentialBackoff() {
        return retry.isEnableExponentialBackoff();
    }

    public void setEnableExponentialBackoff(boolean enableExponentialBackoff) {
        retry.setEnableExponentialBackoff(enableExponentialBackoff);
    }

    public boolean isEnableIdempotency() {
        return idempotency.isEnable();
    }

    public void setEnableIdempotency(boolean enableIdempotency) {
        idempotency.setEnable(enableIdempotency);
    }

    public int getIdempotencyTtlMinutes() {
        return idempotency.getTtlMinutes();
    }

    public void setIdempotencyTtlMinutes(int idempotencyTtlMinutes) {
        idempotency.setTtlMinutes(idempotencyTtlMinutes);
    }

    public String getIdempotencyKeyPrefix() {
        return idempotency.getKeyPrefix();
    }

    public void setIdempotencyKeyPrefix(String idempotencyKeyPrefix) {
        idempotency.setKeyPrefix(idempotencyKeyPrefix);
    }

    public int getIdempotencyTimeoutSeconds() {
        return idempotency.getTimeoutSeconds();
    }

    public void setIdempotencyTimeoutSeconds(int idempotencyTimeoutSeconds) {
        idempotency.setTimeoutSeconds(idempotencyTimeoutSeconds);
    }

    public boolean isEnableDetailedLogging() {
        return logging.isEnableDetailedLogging();
    }

    public void setEnableDetailedLogging(boolean enableDetailedLogging) {
        logging.setEnableDetailedLogging(enableDetailedLogging);
    }

    public boolean isConsoleLogging() {
        return logging.isConsoleLogging();
    }

    public void setConsoleLogging(boolean consoleLogging) {
        logging.setConsoleLogging(consoleLogging);
    }

    public int getLogRetentionDays() {
        return logging.getLogRetentionDays();
    }

    public void setLogRetentionDays(int logRetentionDays) {
        logging.setLogRetentionDays(logRetentionDays);
    }

    private void validateScanPackages() {
        if (scanPackages != null) {
            for (String pkg : scanPackages) {
                if (pkg == null || pkg.trim().isEmpty()) {
                    throw new IllegalArgumentException("scan package cannot be empty");
                }
                if (!pkg.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$")) {
                    throw new IllegalArgumentException("invalid scan package format: " + pkg);
                }
            }
        }
    }

    @Data
    public static class Lifecycle {

        private boolean enableManagement = true;

        @Min(5)
        private int shutdownTimeoutSeconds = 30;

        @Min(1)
        private int shutdownCheckIntervalSeconds = 1;
    }

    @Data
    public static class Metrics {

        private boolean enable = true;

        @Min(10)
        private int intervalSeconds = 60;

        @Min(1)
        private int retentionDays = 30;

        void validate() {
            if (retentionDays < 1) {
                throw new IllegalArgumentException("metrics retention days must be >= 1");
            }
        }
    }

    @Data
    public static class Retry {

        @Min(0)
        private int defaultTimes = 3;

        @Min(1)
        private int intervalSeconds = 60;

        @Min(60)
        private int maxIntervalSeconds = 3600;

        private boolean enableExponentialBackoff = false;

        void validate() {
            if (defaultTimes > 10) {
                throw new IllegalArgumentException("default retry times cannot exceed 10");
            }

            if (intervalSeconds > maxIntervalSeconds) {
                throw new IllegalArgumentException("retry interval cannot exceed max retry interval");
            }
        }
    }

    @Data
    public static class Idempotency {

        private boolean enable = true;

        @Min(1)
        private int ttlMinutes = 1440;

        @NotBlank
        private String keyPrefix = "powerjob";

        @Min(1)
        private int timeoutSeconds = 5;
    }

    @Data
    public static class Logging {

        private boolean enableDetailedLogging = false;

        private boolean consoleLogging = false;

        @Min(1)
        private int logRetentionDays = 7;

        void validate() {
            if (logRetentionDays < 1) {
                throw new IllegalArgumentException("log retention days must be >= 1");
            }
        }
    }
}
