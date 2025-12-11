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

    public boolean isEnabled() {
        return enabled;
    }

    public ProviderType getProvider() {
        return provider;
    }

    public ProviderType getPrimaryProvider() {
        return primaryProvider;
    }

    public UnleashConfig getUnleash() {
        return unleash;
    }

    public FlagsmithConfig getFlagsmith() {
        return flagsmith;
    }

    public CacheConfig getCache() {
        return new CacheConfig();
    }

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

        public String getUrl() {
            return url;
        }

        public String getApiToken() {
            return apiToken;
        }

        public String getAppName() {
            return appName;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getProjectName() {
            return projectName;
        }

        public long getFetchTogglesInterval() {
            return fetchTogglesInterval;
        }

        public long getSendMetricsInterval() {
            return sendMetricsInterval;
        }

        public boolean isSynchronousFetchOnInitialisation() {
            return synchronousFetchOnInitialisation;
        }
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

        public String getUrl() {
            return url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public boolean isEnableLocalEvaluation() {
            return enableLocalEvaluation;
        }

        public int getEnvironmentRefreshIntervalSeconds() {
            return environmentRefreshIntervalSeconds;
        }
    }

    /**
     * 缓存配置
     */
    @Data
    public static class CacheConfig {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 最大缓存大小
         */
        private long maxSize = 10000L;

        /**
         * 写入后过期时间（秒）
         */
        private long expireAfterWrite = 300L;

        /**
         * 访问后过期时间（秒）
         */
        private long expireAfterAccess = 600L;

        // 手动添加 getter 以解决 Lombok 注解处理问题
        public boolean isEnabled() {
            return enabled;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public long getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public long getExpireAfterAccess() {
            return expireAfterAccess;
        }
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
