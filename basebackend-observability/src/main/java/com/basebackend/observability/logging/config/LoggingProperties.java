package com.basebackend.observability.logging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志系统配置属性
 * <p>
 * 统一管理结构化日志、脱敏、采样、路由等配置。
 * </p>
 * <p>
 * 配置示例：
 * <pre>{@code
 * observability:
 *   logging:
 *     enabled: true
 *     format:
 *       type: json                        # json/text
 *       include-trace-context: true       # 包含 traceId/spanId
 *       include-business-context: true    # 包含租户ID/用户ID
 *       timestamp-format: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
 *     masking:
 *       enabled: true
 *       rules:
 *         - field-pattern: "mobile|phone"
 *           strategy: PARTIAL              # PARTIAL/HIDE/HASH
 *         - field-pattern: "password|pwd"
 *           strategy: HIDE
 *     sampling:
 *       enabled: true
 *       rules:
 *         - level: ERROR
 *           rate: 1.0                     # ERROR 100%
 *         - level: INFO
 *           rate: 0.1                     # INFO 10%
 *     routing:
 *       enabled: true
 *       destinations:
 *         - name: console
 *           enabled: true
 *           level: INFO
 *         - name: loki
 *           enabled: true
 *           level: INFO
 *           url: http://localhost:3100
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "observability.logging")
@Validated
public class LoggingProperties {

    /**
     * 是否启用日志增强功能
     */
    private boolean enabled = true;

    /**
     * 日志格式配置
     */
    @Valid
    @NotNull
    private Format format = new Format();

    /**
     * 敏感信息脱敏配置
     */
    @Valid
    @NotNull
    private Masking masking = new Masking();

    /**
     * 日志采样配置
     */
    @Valid
    @NotNull
    private Sampling sampling = new Sampling();

    /**
     * 日志路由配置
     */
    @Valid
    @NotNull
    private Routing routing = new Routing();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public Masking getMasking() {
        return masking;
    }

    public void setMasking(Masking masking) {
        this.masking = masking;
    }

    public Sampling getSampling() {
        return sampling;
    }

    public void setSampling(Sampling sampling) {
        this.sampling = sampling;
    }

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    /**
     * 日志格式配置
     */
    public static class Format {

        /**
         * 日志格式类型
         */
        @NotBlank
        @Pattern(regexp = "json|text", message = "日志格式类型必须是 json 或 text")
        private String type = "json";  // json/text

        /**
         * 是否包含追踪上下文（traceId、spanId）
         */
        private boolean includeTraceContext = true;

        /**
         * 是否包含业务上下文（tenantId、userId等）
         */
        private boolean includeBusinessContext = true;

        /**
         * 时间戳格式
         */
        @NotBlank
        private String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

        /**
         * 自定义字段映射
         * <p>
         * key: 日志字段名，value: MDC key
         * </p>
         */
        private Map<String, String> customFields = new HashMap<>();

        // Getters and Setters

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isIncludeTraceContext() {
            return includeTraceContext;
        }

        public void setIncludeTraceContext(boolean includeTraceContext) {
            this.includeTraceContext = includeTraceContext;
        }

        public boolean isIncludeBusinessContext() {
            return includeBusinessContext;
        }

        public void setIncludeBusinessContext(boolean includeBusinessContext) {
            this.includeBusinessContext = includeBusinessContext;
        }

        public String getTimestampFormat() {
            return timestampFormat;
        }

        public void setTimestampFormat(String timestampFormat) {
            this.timestampFormat = timestampFormat;
        }

        public Map<String, String> getCustomFields() {
            return customFields;
        }

        public void setCustomFields(Map<String, String> customFields) {
            this.customFields = (customFields == null) ? new HashMap<>() : new HashMap<>(customFields);
        }
    }

    /**
     * 敏感信息脱敏配置
     */
    public static class Masking {

        /**
         * 是否启用脱敏
         */
        private boolean enabled = true;

        /**
         * 脱敏规则列表
         */
        @Valid
        private List<MaskingRule> rules = new ArrayList<>();

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<MaskingRule> getRules() {
            return rules;
        }

        public void setRules(List<MaskingRule> rules) {
            this.rules = (rules == null) ? new ArrayList<>() : new ArrayList<>(rules);
        }
    }

    /**
     * 脱敏规则
     */
    public static class MaskingRule {

        /**
         * 字段名匹配模式（正则表达式）
         * <p>
         * 例如：mobile|phone, password|pwd, idCard
         * </p>
         */
        @NotBlank
        private String fieldPattern;

        /**
         * 脱敏策略
         * <p>
         * PARTIAL: 部分显示（如手机号显示前3后4位）<br>
         * HIDE: 完全隐藏（替换为 ******）<br>
         * HASH: 哈希化（SHA-256）
         * </p>
         */
        @NotBlank
        @Pattern(regexp = "PARTIAL|HIDE|HASH", message = "脱敏策略必须是 PARTIAL、HIDE 或 HASH")
        private String strategy = "PARTIAL";  // PARTIAL/HIDE/HASH

        /**
         * 部分显示策略的保留位数配置
         * <p>
         * 格式：prefix-suffix（如 "3-4" 表示保留前3位和后4位）
         * </p>
         */
        @Pattern(regexp = "\\d+-\\d+", message = "部分显示模式必须是 'prefix-suffix' 格式，如 '3-4'")
        private String partialPattern = "3-4";

        // Getters and Setters

        public String getFieldPattern() {
            return fieldPattern;
        }

        public void setFieldPattern(String fieldPattern) {
            this.fieldPattern = fieldPattern;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getPartialPattern() {
            return partialPattern;
        }

        public void setPartialPattern(String partialPattern) {
            this.partialPattern = partialPattern;
        }
    }

    /**
     * 日志采样配置
     */
    public static class Sampling {

        /**
         * 是否启用采样
         */
        private boolean enabled = true;

        /**
         * 采样规则列表
         */
        @Valid
        private List<SamplingRule> rules = new ArrayList<>();

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<SamplingRule> getRules() {
            return rules;
        }

        public void setRules(List<SamplingRule> rules) {
            this.rules = (rules == null) ? new ArrayList<>() : new ArrayList<>(rules);
        }
    }

    /**
     * 采样规则
     */
    public static class SamplingRule {

        /**
         * 日志级别
         * <p>
         * 可选值：ERROR, WARN, INFO, DEBUG, TRACE
         * </p>
         */
        @NotBlank
        @Pattern(regexp = "ERROR|WARN|INFO|DEBUG|TRACE", message = "日志级别必须是 ERROR、WARN、INFO、DEBUG 或 TRACE")
        private String level;

        /**
         * 采样率（0.0-1.0）
         * <p>
         * 1.0 表示 100% 采样，0.1 表示 10% 采样
         * </p>
         */
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double rate = 1.0;

        /**
         * 包名过滤（可选）
         * <p>
         * 例如：com.basebackend.admin
         * </p>
         */
        private String packageName;

        // Getters and Setters

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }

    /**
     * 日志路由配置
     */
    public static class Routing {

        /**
         * 是否启用路由
         */
        private boolean enabled = true;

        /**
         * 目标出口列表
         */
        @Valid
        private List<Destination> destinations = new ArrayList<>();

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<Destination> getDestinations() {
            return destinations;
        }

        public void setDestinations(List<Destination> destinations) {
            this.destinations = (destinations == null) ? new ArrayList<>() : new ArrayList<>(destinations);
        }
    }

    /**
     * 日志目标配置
     */
    public static class Destination {

        /**
         * 目标名称
         */
        @NotBlank
        private String name;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 最低日志级别
         */
        @NotBlank
        @Pattern(regexp = "ERROR|WARN|INFO|DEBUG|TRACE", message = "日志级别必须是 ERROR、WARN、INFO、DEBUG 或 TRACE")
        private String level = "INFO";

        /**
         * 目标类型
         * <p>
         * console: 控制台输出<br>
         * loki: Grafana Loki<br>
         * file: 文件输出
         * </p>
         */
        @NotBlank
        @Pattern(regexp = "console|loki|file", message = "目标类型必须是 console、loki 或 file")
        private String type = "console";

        /**
         * 目标 URL（Loki 等远程目标需要）
         */
        private String url;

        /**
         * 文件路径（文件输出需要）
         */
        private String path;

        /**
         * 业务类别过滤（可选）
         * <p>
         * 例如：AUDIT（仅接收审计日志）
         * </p>
         */
        private String category;

        /**
         * 批量发送大小（远程目标）
         */
        @Positive(message = "批量发送大小必须大于 0")
        private int batchSize = 100;

        /**
         * 批量发送超时（远程目标）
         */
        private Duration batchTimeout = Duration.ofSeconds(1);

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getBatchTimeout() {
            return batchTimeout;
        }

        public void setBatchTimeout(Duration batchTimeout) {
            this.batchTimeout = batchTimeout;
        }
    }
}
