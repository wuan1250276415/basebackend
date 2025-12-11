package com.basebackend.featuretoggle.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nacos配置管理器
 * <p>
 * 管理特性开关从Nacos配置中心的加载、缓存和刷新。
 * 提供统一的配置访问接口。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class NacosConfigManager {

    private final NacosConfigService nacosConfigService;
    private final FeatureToggleProperties properties;
    private final Map<String, Boolean> featureToggleStates = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public NacosConfigManager(NacosConfigService nacosConfigService, FeatureToggleProperties properties) {
        this.nacosConfigService = nacosConfigService;
        this.properties = properties;
        log.info("Initializing NacosConfigManager");
    }

    /**
     * 初始化配置管理器
     */
    public void initialize() {
        if (initialized) {
            log.warn("NacosConfigManager is already initialized");
            return;
        }

        try {
            log.info("Initializing NacosConfigManager...");

            // 加载初始配置
            if (nacosConfigService != null && nacosConfigService.isAvailable()) {
                nacosConfigService.loadAllConfigs();
                log.info("Initial config loaded from Nacos");
            } else {
                log.warn("NacosConfigService not available, skipping initial config load");
            }

            // 注册配置变化监听器
            if (nacosConfigService != null) {
                nacosConfigService.registerConfigChangeListener("feature-toggles", this::refreshFeatureToggles);
            }

            // 刷新特性开关状态
            refreshFeatureToggles();

            initialized = true;
            log.info("NacosConfigManager initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize NacosConfigManager", e);
        }
    }

    /**
     * 刷新特性开关配置
     */
    public void refreshFeatureToggles() {
        try {
            log.debug("Refreshing feature toggle states from Nacos");

            // 清空现有状态
            featureToggleStates.clear();

            // 从Nacos加载特性开关配置
            // 这里我们假设配置格式为：feature.{name}.enabled=true
            Map<String, String> configCache = nacosConfigService.getConfigCache();

            for (Map.Entry<String, String> entry : configCache.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 处理特性开关配置
                if (key.startsWith("feature.") && key.endsWith(".enabled")) {
                    String featureName = key.substring(8, key.length() - 8); // 提取特性名称
                    boolean enabled = nacosConfigService.getBooleanConfig(key, false);
                    featureToggleStates.put(featureName, enabled);

                    log.debug("Feature toggle state loaded: {} = {}", featureName, enabled);
                }
            }

            log.info("Refreshed {} feature toggle states from Nacos", featureToggleStates.size());

        } catch (Exception e) {
            log.error("Error refreshing feature toggle states from Nacos", e);
        }
    }

    /**
     * 检查特性是否启用
     *
     * @param featureName 特性名称
     * @return 是否启用
     */
    public boolean isFeatureEnabled(String featureName) {
        if (!initialized) {
            initialize();
        }

        // 首先检查Nacos配置
        Boolean nacosEnabled = featureToggleStates.get(featureName);
        if (nacosEnabled != null) {
            return nacosEnabled;
        }

        // 如果Nacos中没有配置，回退到本地配置
        return isFeatureEnabledFromLocal(featureName);
    }

    /**
     * 从本地配置检查特性是否启用
     *
     * @param featureName 特性名称
     * @return 是否启用
     */
    private boolean isFeatureEnabledFromLocal(String featureName) {
        // 这里可以添加本地配置的检查逻辑
        // 例如从本地properties文件或环境变量读取
        log.debug("Feature '{}' not found in Nacos config, checking local config", featureName);
        return false; // 默认关闭
    }

    /**
     * 获取特性开关状态
     *
     * @return 所有特性开关状态
     */
    public Map<String, Boolean> getAllFeatureToggleStates() {
        return new ConcurrentHashMap<>(featureToggleStates);
    }

    /**
     * 强制刷新配置（手动触发）
     */
    public void forceRefresh() {
        log.info("Force refreshing Nacos configurations");
        if (nacosConfigService != null) {
            nacosConfigService.clearCache();
            nacosConfigService.loadAllConfigs();
        }
        refreshFeatureToggles();
    }

    /**
     * 检查Nacos是否可用
     *
     * @return 是否可用
     */
    public boolean isNacosAvailable() {
        return nacosConfigService != null && nacosConfigService.isAvailable();
    }

    /**
     * 获取配置统计信息
     *
     * @return 统计信息
     */
    public String getStatistics() {
        return String.format("NacosConfigManager{initialized=%s, nacosAvailable=%s, featureToggleCount=%d}",
                initialized, isNacosAvailable(), featureToggleStates.size());
    }

    /**
     * 清理资源
     */
    public void destroy() {
        if (nacosConfigService != null) {
            nacosConfigService.unregisterConfigChangeListener("feature-toggles");
        }
        featureToggleStates.clear();
        initialized = false;
        log.info("NacosConfigManager destroyed");
    }

    @Override
    public String toString() {
        return getStatistics();
    }
}
