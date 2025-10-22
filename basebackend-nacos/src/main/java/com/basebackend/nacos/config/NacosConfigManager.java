package com.basebackend.nacos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * Nacos 配置管理器
 */
@Slf4j
@RefreshScope
@RequiredArgsConstructor
public class NacosConfigManager {

    private final NacosConfigProperties nacosConfigProperties;

    /**
     * 获取服务发现配置
     */
    public NacosConfigProperties.Discovery getDiscoveryConfig() {
        return nacosConfigProperties.getDiscovery();
    }

    /**
     * 获取配置中心配置
     */
    public NacosConfigProperties.Config getConfigConfig() {
        return nacosConfigProperties.getConfig();
    }

    /**
     * 检查服务发现是否启用
     */
    public boolean isDiscoveryEnabled() {
        return nacosConfigProperties.getDiscovery().isEnabled();
    }

    /**
     * 检查配置中心是否启用
     */
    public boolean isConfigEnabled() {
        return nacosConfigProperties.getConfig().isEnabled();
    }
}
