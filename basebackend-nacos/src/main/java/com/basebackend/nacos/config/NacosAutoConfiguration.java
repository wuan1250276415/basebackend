package com.basebackend.nacos.config;

import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import com.basebackend.nacos.repository.InMemoryGrayReleaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Nacos 自动配置聚合类
 * <p>
 * 负责聚合配置中心和配置中心子配置类，
 * 提供统一的配置入口和基础Bean。
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NacosConfigProperties.class)
@ConditionalOnProperty(
    prefix = "nacos",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Import({
    NacosConfigConfiguration.class,
    NacosDiscoveryConfiguration.class
})
@RequiredArgsConstructor
public class NacosAutoConfiguration {

    private final NacosConfigProperties nacosConfigProperties;

    /**
     * 配置验证Bean
     * 确保关键配置正确性
     */
    @Bean
    @ConditionalOnMissingBean
    public NacosConfigValidator nacosConfigValidator() {
        return new NacosConfigValidator(nacosConfigProperties);
    }

    /**
     * Nacos配置管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public NacosConfigManager customNacosConfigManager() {
        log.info("初始化自定义 Nacos 配置管理器");
        return new NacosConfigManager(nacosConfigProperties);
    }

    /**
     * 灰度发布历史仓储
     * <p>
     * 默认使用内存实现，生产环境可通过自定义Bean覆盖为数据库实现
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(GrayReleaseHistoryRepository.class)
    public GrayReleaseHistoryRepository grayReleaseHistoryRepository() {
        log.info("初始化内存版灰度发布历史仓储");
        return new InMemoryGrayReleaseHistoryRepository();
    }

}
