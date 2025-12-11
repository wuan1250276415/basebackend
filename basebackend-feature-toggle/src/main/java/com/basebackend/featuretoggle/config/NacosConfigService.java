package com.basebackend.featuretoggle.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos配置服务
 * <p>
 * 提供从Nacos动态加载特性开关配置的能力，支持配置热更新。
 * 当Nacos中的配置发生变化时，自动同步到本地缓存。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *   <li>动态配置加载 - 从Nacos加载特性开关配置</li>
 *   <li>配置热更新 - 监听Nacos配置变化并自动更新</li *>
 *   <li>多环境支持 - 支持dev、test、prod等多环境配置</li>
 *   <li>配置缓存 - 本地缓存配置，减少Nacos访问</li>
 *   <li>降级策略 - Nacos不可用时使用本地默认配置</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnClass({ConfigService.class})
@ConditionalOnProperty(name = "basebackend.feature-toggle.nacos.enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfigService {

    private final ConfigService configService;
    private final NacosConfigProperties nacosConfigProperties;
    private final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Runnable> configChangeListeners = new ConcurrentHashMap<>();
    private final Executor executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "nacos-config-refresh");
        t.setDaemon(true);
        return t;
    });

    public NacosConfigService(ConfigService configService, NacosConfigProperties nacosConfigProperties) {
        this.configService = configService;
        this.nacosConfigProperties = nacosConfigProperties;
        log.info("Initializing NacosConfigService");
        initializeConfigListener();
    }

    /**
     * 初始化配置监听器
     */
    private void initializeConfigListener() {
        String dataId = nacosConfigProperties.getDataId();
        String groupId = nacosConfigProperties.getGroupId();

        try {
            // 注册配置监听器
            configService.addListener(dataId, groupId, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("Received config change from Nacos: dataId={}, groupId={}", dataId, groupId);
                    refreshConfig(configInfo);
                }

                @Override
                public Executor getExecutor() {
                    return executor;
                }
            });

            log.info("Successfully registered Nacos config listener for dataId={}, groupId={}", dataId, groupId);
        } catch (NacosException e) {
            log.error("Failed to register Nacos config listener", e);
        }
    }

    /**
     * 刷新配置
     */
    private void refreshConfig(String configInfo) {
        try {
            if (configInfo != null && !configInfo.trim().isEmpty()) {
                parseAndCacheConfig(configInfo);

                // 触发配置变化监听器
                configChangeListeners.values().forEach(listener -> {
                    try {
                        listener.run();
                    } catch (Exception e) {
                        log.error("Error executing config change listener", e);
                    }
                });

                log.info("Config refreshed successfully from Nacos");
            }
        } catch (Exception e) {
            log.error("Error refreshing config from Nacos", e);
        }
    }

    /**
     * 解析并缓存配置
     */
    private void parseAndCacheConfig(String configInfo) {
        try {
            // 解析JSON格式的配置
            // 这里简化处理，实际项目中应该使用Jackson或Gson等库
            String[] lines = configInfo.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.contains("=") && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        configCache.put(key, value);
                        log.debug("Cached config: {} = {}", key, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing config from Nacos", e);
        }
    }

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    public String getConfig(String key) {
        return getConfig(key, null);
    }

    /**
     * 获取配置值（带默认值）
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getConfig(String key, String defaultValue) {
        String value = configCache.get(key);
        if (value == null) {
            log.debug("Config not found for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * 获取布尔型配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 获取数值型配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getIntConfig(String key, int defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config value for key: {}, value: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 加载所有配置
     */
    public void loadAllConfigs() {
        String dataId = nacosConfigProperties.getDataId();
        String groupId = nacosConfigProperties.getGroupId();
        long timeoutMs = nacosConfigProperties.getTimeoutMs();

        try {
            log.info("Loading configs from Nacos: dataId={}, groupId={}", dataId, groupId);
            String configInfo = configService.getConfig(dataId, groupId, timeoutMs);

            if (configInfo != null) {
                parseAndCacheConfig(configInfo);
                log.info("Successfully loaded {} configs from Nacos", configCache.size());
            } else {
                log.warn("No config found in Nacos for dataId={}, groupId={}", dataId, groupId);
            }
        } catch (NacosException e) {
            log.error("Failed to load configs from Nacos", e);
        }
    }

    /**
     * 注册配置变化监听器
     *
     * @param listenerKey 监听器键
     * @param listener 监听器
     */
    public void registerConfigChangeListener(String listenerKey, Runnable listener) {
        configChangeListeners.put(listenerKey, listener);
        log.debug("Registered config change listener: {}", listenerKey);
    }

    /**
     * 取消配置变化监听器
     *
     * @param listenerKey 监听器键
     */
    public void unregisterConfigChangeListener(String listenerKey) {
        configChangeListeners.remove(listenerKey);
        log.debug("Unregistered config change listener: {}", listenerKey);
    }

    /**
     * 清空配置缓存
     */
    public void clearCache() {
        configCache.clear();
        log.info("Cleared Nacos config cache");
    }

    /**
     * 获取缓存的配置数量
     *
     * @return 配置数量
     */
    public int getCacheSize() {
        return configCache.size();
    }

    /**
     * 获取缓存内容（调试用）
     *
     * @return 配置缓存内容
     */
    public ConcurrentHashMap<String, String> getConfigCache() {
        return new ConcurrentHashMap<>(configCache);
    }

    /**
     * 检查是否可用
     *
     * @return 是否可用
     */
    public boolean isAvailable() {
        return configService != null && !configCache.isEmpty();
    }

    /**
     * 获取配置统计信息
     *
     * @return 统计信息
     */
    public String getStatistics() {
        return String.format("NacosConfigService{configCacheSize=%d, listeners=%d}",
                configCache.size(), configChangeListeners.size());
    }

    @Override
    public String toString() {
        return getStatistics();
    }
}
