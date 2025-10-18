package com.basebackend.nacos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Nacos 配置中心配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nacos.config", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosConfigConfiguration {

    private final NacosConfigProperties nacosConfigProperties;

    @PostConstruct
    public void init() {
        log.info("Nacos 配置中心配置已启用");
        log.info("配置中心地址: {}", nacosConfigProperties.getConfig().getServerAddr());
        log.info("命名空间: {}", nacosConfigProperties.getConfig().getNamespace());
        log.info("分组: {}", nacosConfigProperties.getConfig().getGroup());
    }
}
