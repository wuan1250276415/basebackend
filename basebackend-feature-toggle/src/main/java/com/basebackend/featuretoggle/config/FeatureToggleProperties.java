package com.basebackend.featuretoggle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 特性开关配置属性
 *
 * @author BaseBackend
 */
@Data
@ConfigurationProperties(prefix = "feature-toggle")
public class FeatureToggleProperties {

    /**
     * 是否启用特性开关
     */
    private boolean enabled = false;

    /**
     * 提供商类型：unleash、flagsmith、both
     */
    private ProviderType provider = ProviderType.UNLEASH;

    /**
     * 当provider=both时，优先使用的提供商
     */
    private ProviderType primaryProvider = ProviderType.UNLEASH;

    /**
     * Unleash配置
     */
    private UnleashConfig unleash = new UnleashConfig();

    /**
     * Flagsmith配置
     */
    private FlagsmithConfig flagsmith = new FlagsmithConfig();

    @Data
    public static class UnleashConfig {
        /**
         * Unleash服务URL
         */
        private String url = "http://localhost:4242/api";

        /**
         * API Token（客户端密钥）
         */
        private String apiToken;

        /**
         * 应用名称
         */
        private String appName = "basebackend";

        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 环境
         */
        private String environment = "development";

        /**
         * 项目名称（可选，用于项目级隔离）
         */
        private String projectName;

        /**
         * 同步间隔（秒）
         */
        private long fetchTogglesInterval = 10;

        /**
         * 指标发送间隔（秒）
         */
        private long sendMetricsInterval = 60;

        /**
         * 启动时是否同步
         */
        private boolean synchronousFetchOnInitialisation = true;
    }

    @Data
    public static class FlagsmithConfig {
        /**
         * Flagsmith服务URL
         */
        private String url = "https://edge.api.flagsmith.com/api/v1/";

        /**
         * API密钥（环境密钥）
         */
        private String apiKey;

        /**
         * 连接超时（毫秒）
         */
        private int connectTimeout = 2000;

        /**
         * 写入超时（毫秒）
         */
        private int writeTimeout = 5000;

        /**
         * 读取超时（毫秒）
         */
        private int readTimeout = 5000;

        /**
         * 是否启用本地缓存
         */
        private boolean enableLocalEvaluation = false;

        /**
         * 环境刷新间隔（秒）
         */
        private int environmentRefreshIntervalSeconds = 60;
    }

    /**
     * 提供商类型枚举
     */
    public enum ProviderType {
        /**
         * 使用Unleash
         */
        UNLEASH,

        /**
         * 使用Flagsmith
         */
        FLAGSMITH,

        /**
         * 同时使用两者
         */
        BOTH
    }
}
