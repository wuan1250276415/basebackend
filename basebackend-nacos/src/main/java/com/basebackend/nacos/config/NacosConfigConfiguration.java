package com.basebackend.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.basebackend.nacos.exception.NacosInitializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

/**
 * Nacos 配置中心配置
 * <p>
 * 负责配置中心ConfigService的创建和管理，
 * 实现配置中心的动态刷新和配置隔离功能。
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nacos.config", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
public class NacosConfigConfiguration {

    private final NacosConfigProperties nacosConfigProperties;

    /**
     * 配置中心ConfigService
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigService configService() {
        NacosConfigProperties.Config config = nacosConfigProperties.getConfig();

        Properties properties = new Properties();
        properties.put("serverAddr", config.getServerAddr());
        properties.put("namespace", config.getNamespace());
        properties.put("group", config.getGroup());
        properties.put("username", config.getUsername());
        properties.put("password", config.getPassword());

        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
            log.info("Nacos ConfigService 初始化成功");
            log.info("配置中心地址: {}", config.getServerAddr());
            log.info("命名空间: {}", config.getNamespace());
            log.info("分组: {}", config.getGroup());
            return configService;
        } catch (Exception e) {
            log.error("Nacos ConfigService 初始化失败", e);
            throw new NacosInitializationException("ConfigService初始化失败", e);
        }
    }

    @PostConstruct
    public void init() {
        log.info("Nacos 配置中心配置已启用");
    }
}
