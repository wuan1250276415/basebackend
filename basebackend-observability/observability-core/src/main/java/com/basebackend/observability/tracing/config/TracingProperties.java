package com.basebackend.observability.tracing.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式追踪配置属性（Phase 3）
 * <p>
 * 配置前缀：{@code observability.tracing}
 * </p>
 * <p>
 * 配置示例：
 * <pre>{@code
 * observability:
 *   tracing:
 *     enabled: true
 *     propagation:
 *       enabled: true
 *       business-keys:
 *         - X-Tenant-Id
 *         - X-Channel-Id
 *     sampler:
 *       default-rate: 1.0
 *       always-sample-errors: true
 *       latency-threshold-ms: 1000
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "observability.tracing")
public class TracingProperties {

    /**
     * 追踪功能总开关
     */
    private boolean enabled = true;

    @Valid
    private Propagation propagation = new Propagation();

    @Valid
    private Http http = new Http();

    @Valid
    private Sampler sampler = new Sampler();

    @Valid
    private Export export = new Export();

    @Valid
    private Span span = new Span();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public Sampler getSampler() {
        return sampler;
    }

    public void setSampler(Sampler sampler) {
        this.sampler = sampler;
    }

    public Export getExport() {
        return export;
    }

    public void setExport(Export export) {
        this.export = export;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    /**
     * 上下文传播配置
     */
    public static class Propagation {

        /**
         * 是否启用上下文传播
         */
        private boolean enabled = true;

        /**
         * 业务上下文字段白名单
         * <p>
         * 只有在白名单中的 header 字段才会被传播。
         * 默认包含常用的业务上下文字段。
         * </p>
         */
        private List<String> businessKeys = new ArrayList<>(
                List.of("X-Tenant-Id", "X-Channel-Id", "X-Request-Id", "X-User-Id"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getBusinessKeys() {
            return businessKeys;
        }

        public void setBusinessKeys(List<String> businessKeys) {
            this.businessKeys = (businessKeys == null) ? new ArrayList<>() : new ArrayList<>(businessKeys);
        }
    }

    /**
     * HTTP 追踪配置
     */
    public static class Http {

        @Valid
        private Server server = new Server();

        @Valid
        private Client client = new Client();

        public Server getServer() {
            return server;
        }

        public void setServer(Server server) {
            this.server = server;
        }

        public Client getClient() {
            return client;
        }

        public void setClient(Client client) {
            this.client = client;
        }

        /**
         * HTTP 服务端追踪配置
         */
        public static class Server {
            /**
             * 是否启用 HTTP 服务端追踪
             */
            private boolean enabled = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        /**
         * HTTP 客户端追踪配置
         */
        public static class Client {
            /**
             * 是否启用 HTTP 客户端追踪
             */
            private boolean enabled = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    /**
     * 采样配置
     */
    public static class Sampler {

        /**
         * 默认采样率（0.0-1.0）
         * <p>
         * 1.0 表示 100% 采样（开发环境推荐）<br>
         * 0.01 表示 1% 采样（生产环境推荐）
         * </p>
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private double defaultRate = 1.0d;

        /**
         * 是否总是采样错误请求
         * <p>
         * 启用后，所有产生异常的请求都会被采样，无论采样率如何。
         * </p>
         */
        private boolean alwaysSampleErrors = true;

        /**
         * 是否总是采样慢请求
         * <p>
         * 启用后，所有超过延迟阈值的请求都会被采样，用于捕获性能问题。
         * </p>
         */
        private boolean alwaysSampleSlow = true;

        /**
         * 延迟阈值（毫秒）
         * <p>
         * 超过此阈值的请求将被强制采样，用于捕获慢请求。
         * </p>
         */
        @Min(0)
        private int latencyThresholdMs = 1000;

        /**
         * 采样规则列表
         * <p>
         * 支持基于 URL、HTTP 方法、用户 ID 的细粒度采样控制。
         * </p>
         */
        @Valid
        private List<SamplingRule> rules = new ArrayList<>();

        /**
         * 动态采样配置
         */
        @Valid
        private Dynamic dynamic = new Dynamic();

        public double getDefaultRate() {
            return defaultRate;
        }

        public void setDefaultRate(double defaultRate) {
            this.defaultRate = defaultRate;
        }

        public boolean isAlwaysSampleErrors() {
            return alwaysSampleErrors;
        }

        public void setAlwaysSampleErrors(boolean alwaysSampleErrors) {
            this.alwaysSampleErrors = alwaysSampleErrors;
        }

        public boolean isAlwaysSampleSlow() {
            return alwaysSampleSlow;
        }

        public void setAlwaysSampleSlow(boolean alwaysSampleSlow) {
            this.alwaysSampleSlow = alwaysSampleSlow;
        }

        public int getLatencyThresholdMs() {
            return latencyThresholdMs;
        }

        public void setLatencyThresholdMs(int latencyThresholdMs) {
            this.latencyThresholdMs = latencyThresholdMs;
        }

        public List<SamplingRule> getRules() {
            return rules;
        }

        public void setRules(List<SamplingRule> rules) {
            this.rules = (rules == null) ? new ArrayList<>() : new ArrayList<>(rules);
        }

        public Dynamic getDynamic() {
            return dynamic;
        }

        public void setDynamic(Dynamic dynamic) {
            this.dynamic = dynamic;
        }

        /**
         * 采样规则
         * <p>
         * 支持基于 URL 模式、HTTP 方法、用户 ID 模式的采样控制。
         * 规则按顺序匹配，第一个匹配的规则生效。
         * </p>
         */
        public static class SamplingRule {

            /**
             * URL 正则表达式模式
             * <p>
             * 示例：{@code "/api/users/.*"}
             * </p>
             */
            private String urlPattern;

            /**
             * HTTP 方法
             * <p>
             * 示例：GET, POST, PUT, DELETE
             * </p>
             */
            private String httpMethod;

            /**
             * 用户 ID 正则表达式模式
             * <p>
             * 示例：{@code "user-.*"} 匹配所有以 "user-" 开头的用户 ID
             * </p>
             */
            private String userIdPattern;

            /**
             * 采样率（0.0-1.0）
             */
            @DecimalMin(value = "0.0")
            @DecimalMax(value = "1.0")
            private double rate = 1.0d;

            public String getUrlPattern() {
                return urlPattern;
            }

            public void setUrlPattern(String urlPattern) {
                this.urlPattern = urlPattern;
            }

            public String getHttpMethod() {
                return httpMethod;
            }

            public void setHttpMethod(String httpMethod) {
                this.httpMethod = httpMethod;
            }

            public String getUserIdPattern() {
                return userIdPattern;
            }

            public void setUserIdPattern(String userIdPattern) {
                this.userIdPattern = userIdPattern;
            }

            public double getRate() {
                return rate;
            }

            public void setRate(double rate) {
                this.rate = rate;
            }
        }

        /**
         * 动态采样配置
         * <p>
         * 根据系统负载自动调整采样率，在保持监控覆盖的同时控制追踪数据量。
         * </p>
         */
        public static class Dynamic {

            /**
             * 是否启用动态采样
             */
            private boolean enabled = false;

            /**
             * 初始采样率（0.0-1.0）
             * <p>
             * 动态采样管理器启动时的初始采样率。
             * 如果未配置或无效，将使用 minRate 作为初始值。
             * </p>
             */
            @DecimalMin(value = "0.0")
            @DecimalMax(value = "1.0")
            private double initialRate = 0.1d;

            /**
             * 最小采样率（0.0-1.0）
             * <p>
             * 即使在高负载情况下，也不会低于此采样率。
             * </p>
             */
            @DecimalMin(value = "0.0")
            @DecimalMax(value = "1.0")
            private double minRate = 0.01d;

            /**
             * 最大采样率（0.0-1.0）
             * <p>
             * 即使在低负载情况下，也不会超过此采样率。
             * </p>
             */
            @DecimalMin(value = "0.0")
            @DecimalMax(value = "1.0")
            private double maxRate = 1.0d;

            /**
             * 目标 Span 数量（每分钟）
             * <p>
             * 动态采样器会调整采样率，尽量使每分钟生成的 Span 数量接近此目标值。
             * </p>
             */
            @Min(1)
            private int targetSpansPerMinute = 1000;

            /**
             * 采样率调整间隔
             * <p>
             * 多久检查一次并调整采样率。
             * </p>
             */
            private Duration adjustInterval = Duration.ofSeconds(30);

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public double getInitialRate() {
                return initialRate;
            }

            public void setInitialRate(double initialRate) {
                this.initialRate = initialRate;
            }

            public double getMinRate() {
                return minRate;
            }

            public void setMinRate(double minRate) {
                this.minRate = minRate;
            }

            public double getMaxRate() {
                return maxRate;
            }

            public void setMaxRate(double maxRate) {
                this.maxRate = maxRate;
            }

            public int getTargetSpansPerMinute() {
                return targetSpansPerMinute;
            }

            public void setTargetSpansPerMinute(int targetSpansPerMinute) {
                this.targetSpansPerMinute = targetSpansPerMinute;
            }

            public Duration getAdjustInterval() {
                return adjustInterval;
            }

            public void setAdjustInterval(Duration adjustInterval) {
                this.adjustInterval = adjustInterval;
            }
        }
    }

    /**
     * 追踪数据导出配置
     */
    public static class Export {

        @Valid
        private Batch batch = new Batch();

        @Valid
        private Retry retry = new Retry();

        public Batch getBatch() {
            return batch;
        }

        public void setBatch(Batch batch) {
            this.batch = batch;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        /**
         * 批量导出配置
         * <p>
         * 批量导出可以提高吞吐量，减少网络开销。
         * </p>
         */
        public static class Batch {

            /**
             * 最大队列大小
             * <p>
             * 达到此大小后，新的 Span 将被丢弃。
             * </p>
             */
            @Min(1)
            private int maxQueueSize = 2048;

            /**
             * 最大批次大小
             * <p>
             * 每次导出的最大 Span 数量。
             * </p>
             */
            @Min(1)
            private int maxBatchSize = 512;

            /**
             * 调度延迟
             * <p>
             * 多久触发一次批量导出。
             * </p>
             */
            private Duration scheduleDelay = Duration.ofMillis(200);

            /**
             * 导出超时
             * <p>
             * 导出操作的最大等待时间。
             * </p>
             */
            private Duration exportTimeout = Duration.ofSeconds(30);

            public int getMaxQueueSize() {
                return maxQueueSize;
            }

            public void setMaxQueueSize(int maxQueueSize) {
                this.maxQueueSize = maxQueueSize;
            }

            public int getMaxBatchSize() {
                return maxBatchSize;
            }

            public void setMaxBatchSize(int maxBatchSize) {
                this.maxBatchSize = maxBatchSize;
            }

            public Duration getScheduleDelay() {
                return scheduleDelay;
            }

            public void setScheduleDelay(Duration scheduleDelay) {
                this.scheduleDelay = scheduleDelay;
            }

            public Duration getExportTimeout() {
                return exportTimeout;
            }

            public void setExportTimeout(Duration exportTimeout) {
                this.exportTimeout = exportTimeout;
            }
        }

        /**
         * 导出重试配置
         * <p>
         * 导出失败时的重试策略。
         * </p>
         */
        public static class Retry {

            /**
             * 是否启用重试
             */
            private boolean enabled = true;

            /**
             * 最大重试次数
             */
            @Min(0)
            private int maxRetries = 5;

            /**
             * 初始重试间隔
             * <p>
             * 使用指数退避策略，每次重试间隔翻倍。
             * </p>
             */
            private Duration initialInterval = Duration.ofSeconds(1);

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getMaxRetries() {
                return maxRetries;
            }

            public void setMaxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
            }

            public Duration getInitialInterval() {
                return initialInterval;
            }

            public void setInitialInterval(Duration initialInterval) {
                this.initialInterval = initialInterval;
            }
        }
    }

    /**
     * Span 配置
     */
    public static class Span {

        /**
         * 自定义标签映射
         * <p>
         * 从 HTTP header 或其他来源提取值，并作为 Span 属性添加。
         * </p>
         * <p>
         * 示例：
         * <pre>{@code
         * custom-tags:
         *   user.id: X-User-Id
         *   tenant.id: X-Tenant-Id
         * }</pre>
         * </p>
         */
        private Map<String, String> customTags = new HashMap<>();

        public Map<String, String> getCustomTags() {
            return customTags;
        }

        public void setCustomTags(Map<String, String> customTags) {
            this.customTags = (customTags == null) ? new HashMap<>() : new HashMap<>(customTags);
        }
    }
}
