package com.basebackend.observability.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 可观测性服务配置属性
 * <p>
 * 统一管理所有外部服务地址和配置参数，避免硬编码。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /**
     * 追踪服务配置
     */
    @Valid
    private Trace trace = new Trace();

    /**
     * 指标服务配置
     */
    @Valid
    private Metrics metrics = new Metrics();

    /**
     * 日志服务配置
     */
    @Valid
    private Logs logs = new Logs();

    /**
     * 缓存配置
     */
    @Valid
    private Cache cache = new Cache();

    /**
     * HTTP客户端配置
     */
    @Valid
    private HttpClient httpClient = new HttpClient();

    @Data
    public static class Trace {
        /**
         * 追踪服务端点
         */
        @NotBlank(message = "追踪服务端点不能为空")
        private String endpoint = "http://localhost:9411";

        /**
         * 追踪数据格式 (zipkin/tempo)
         */
        private String format = "zipkin";

        /**
         * 默认查询时间范围（毫秒）
         */
        @Min(value = 60000, message = "查询时间范围至少1分钟")
        private long defaultLookbackMs = 3600000; // 1小时

        /**
         * 默认查询限制
         */
        @Min(value = 1, message = "查询限制至少为1")
        private int defaultLimit = 100;
    }

    @Data
    public static class Metrics {
        /**
         * 指标服务端点 (Prometheus)
         */
        @NotBlank(message = "指标服务端点不能为空")
        private String endpoint = "http://localhost:9090";

        /**
         * 默认查询步长（秒）
         */
        @Min(value = 1, message = "查询步长至少为1秒")
        private int defaultStep = 60;

        /**
         * 默认查询时间范围（秒）
         */
        @Min(value = 60, message = "查询时间范围至少1分钟")
        private int defaultRange = 3600;
    }

    @Data
    public static class Logs {
        /**
         * 日志服务端点 (Loki)
         */
        @NotBlank(message = "日志服务端点不能为空")
        private String endpoint = "http://localhost:3100";

        /**
         * 默认查询限制
         */
        @Min(value = 1, message = "查询限制至少为1")
        private int defaultLimit = 1000;
    }

    @Data
    public static class Cache {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 服务列表缓存时间（秒）
         */
        @Min(value = 10, message = "缓存时间至少10秒")
        private int servicesTtl = 60;

        /**
         * 追踪数据缓存时间（秒）
         */
        @Min(value = 10, message = "缓存时间至少10秒")
        private int tracesTtl = 300;

        /**
         * 指标数据缓存时间（秒）
         */
        @Min(value = 5, message = "缓存时间至少5秒")
        private int metricsTtl = 30;
    }

    @Data
    public static class HttpClient {
        /**
         * 连接超时（秒）
         */
        @Min(value = 1, message = "连接超时至少1秒")
        private int connectTimeout = 5;

        /**
         * 读取超时（秒）
         */
        @Min(value = 1, message = "读取超时至少1秒")
        private int readTimeout = 30;

        /**
         * 最大连接数
         */
        @Min(value = 10, message = "最大连接数至少10")
        private int maxConnections = 200;

        /**
         * 每个路由的最大连接数
         */
        @Min(value = 5, message = "每路由最大连接数至少5")
        private int maxConnectionsPerRoute = 50;

        /**
         * 是否启用重试
         */
        private boolean retryEnabled = true;

        /**
         * 最大重试次数
         */
        @Min(value = 0, message = "重试次数不能为负")
        private int maxRetries = 3;
    }
}
