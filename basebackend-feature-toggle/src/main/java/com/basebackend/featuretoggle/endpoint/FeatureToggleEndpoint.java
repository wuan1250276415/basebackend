package com.basebackend.featuretoggle.endpoint;

import com.basebackend.featuretoggle.cache.FeatureToggleCache;
import com.basebackend.featuretoggle.config.FeatureToggleProperties;
import com.basebackend.featuretoggle.exception.FeatureToggleExceptionHandler;
import com.basebackend.featuretoggle.metrics.FeatureToggleMetrics;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 特性开关管理端点
 * <p>
 * 提供特性开关的状态、配置和统计信息的查看接口。
 * 可通过Spring Boot Actuator的/actuator/feature-toggles访问。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Endpoint(id = "feature-toggles")
@RequiredArgsConstructor
public class FeatureToggleEndpoint {

    private final FeatureToggleService featureToggleService;
    private final FeatureToggleProperties properties;
    private final FeatureToggleMetrics metrics;
    private final FeatureToggleCache cache;
    private final FeatureToggleExceptionHandler exceptionHandler;

    /**
     * 读取特性开关信息
     *
     * @return 特性开关信息
     */
    @ReadOperation
    public FeatureToggleInfo getInfo() {
        log.debug("Fetching feature toggle info from endpoint");

        FeatureToggleInfo info = new FeatureToggleInfo();

        // 基本信息
        info.setEnabled(properties.isEnabled());
        info.setProvider(properties.getProvider().name());
        info.setPrimaryProvider(properties.getPrimaryProvider().name());
        info.setProviderName(featureToggleService.getProviderName());
        info.setAvailable(featureToggleService.isAvailable());

        // 统计信息
        info.setMetrics(metrics.getStatistics());

        // 缓存统计
        if (cache != null) {
            FeatureToggleCache.CacheStats cacheStats = cache.getStats();
            info.setCacheStats(cacheStats);
        }

        // 异常统计
        if (exceptionHandler != null) {
            FeatureToggleExceptionHandler.ExceptionStatistics exceptionStats = exceptionHandler.getStatistics();
            info.setExceptionStats(exceptionStats);
        }

        // 配置信息
        info.setConfig(createConfigInfo());

        log.info("Feature toggle info retrieved: {}", info.getProviderName());
        return info;
    }

    /**
     * 创建配置信息
     */
    private Map<String, Object> createConfigInfo() {
        Map<String, Object> config = new HashMap<>();

        // Unleash配置
        if (properties.getUnleash() != null) {
            Map<String, Object> unleashConfig = new HashMap<>();
            unleashConfig.put("url", properties.getUnleash().getUrl());
            unleashConfig.put("appName", properties.getUnleash().getAppName());
            unleashConfig.put("environment", properties.getUnleash().getEnvironment());
            unleashConfig.put("apiTokenConfigured", properties.getUnleash().getApiToken() != null);
            config.put("unleash", unleashConfig);
        }

        // Flagsmith配置
        if (properties.getFlagsmith() != null) {
            Map<String, Object> flagsmithConfig = new HashMap<>();
            flagsmithConfig.put("url", properties.getFlagsmith().getUrl());
            flagsmithConfig.put("apiKeyConfigured", properties.getFlagsmith().getApiKey() != null);
            flagsmithConfig.put("enableLocalEvaluation", properties.getFlagsmith().isEnableLocalEvaluation());
            config.put("flagsmith", flagsmithConfig);
        }

        // 缓存配置
        if (properties.getCache() != null) {
            Map<String, Object> cacheConfig = new HashMap<>();
            cacheConfig.put("enabled", properties.getCache().isEnabled());
            cacheConfig.put("maxSize", properties.getCache().getMaxSize());
            cacheConfig.put("expireAfterWrite", properties.getCache().getExpireAfterWrite());
            cacheConfig.put("expireAfterAccess", properties.getCache().getExpireAfterAccess());
            config.put("cache", cacheConfig);
        }

        return config;
    }

    /**
     * 特性开关信息
     */
    public static class FeatureToggleInfo {
        private boolean enabled;
        private String provider;
        private String primaryProvider;
        private String providerName;
        private boolean available;
        private FeatureToggleMetrics.MetricsStatistics metrics;
        private FeatureToggleCache.CacheStats cacheStats;
        private FeatureToggleExceptionHandler.ExceptionStatistics exceptionStats;
        private Map<String, Object> config;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getPrimaryProvider() {
            return primaryProvider;
        }

        public void setPrimaryProvider(String primaryProvider) {
            this.primaryProvider = primaryProvider;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public FeatureToggleMetrics.MetricsStatistics getMetrics() {
            return metrics;
        }

        public void setMetrics(FeatureToggleMetrics.MetricsStatistics metrics) {
            this.metrics = metrics;
        }

        public FeatureToggleCache.CacheStats getCacheStats() {
            return cacheStats;
        }

        public void setCacheStats(FeatureToggleCache.CacheStats cacheStats) {
            this.cacheStats = cacheStats;
        }

        public FeatureToggleExceptionHandler.ExceptionStatistics getExceptionStats() {
            return exceptionStats;
        }

        public void setExceptionStats(FeatureToggleExceptionHandler.ExceptionStatistics exceptionStats) {
            this.exceptionStats = exceptionStats;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        @Override
        public String toString() {
            return String.format("FeatureToggleInfo{enabled=%s, provider=%s, available=%s, metrics=%s}",
                    enabled, provider, available, metrics);
        }
    }
}
