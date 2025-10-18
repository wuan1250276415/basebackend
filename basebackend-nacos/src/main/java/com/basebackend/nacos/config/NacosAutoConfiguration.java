package com.basebackend.nacos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Nacos 自动配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(NacosConfigProperties.class)
@ConditionalOnProperty(prefix = "nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({NacosDiscoveryConfiguration.class, NacosConfigConfiguration.class})
public class NacosAutoConfiguration {

    private final NacosConfigProperties nacosConfigProperties;

    public NacosAutoConfiguration(NacosConfigProperties nacosConfigProperties) {
        this.nacosConfigProperties = nacosConfigProperties;
    }

    @Bean
    public NacosConfigManager nacosConfigManager() {
        log.info("初始化 Nacos 配置管理器");
        return new NacosConfigManager(nacosConfigProperties);
    }
}
