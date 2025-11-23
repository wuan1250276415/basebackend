package com.basebackend.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
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
 * Nacos 服务发现配置
 * <p>
 * 负责服务发现NamingService的创建和管理，
 * 实现服务的注册、发现、上下线管理等功能。
 * </p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nacos.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(2)
public class NacosDiscoveryConfiguration {

    private final NacosConfigProperties nacosConfigProperties;

    /**
     * 服务发现NamingService
     */
    @Bean
    @ConditionalOnMissingBean
    public NamingService namingService() {
        NacosConfigProperties.Discovery discovery = nacosConfigProperties.getDiscovery();

        Properties properties = new Properties();
        properties.put("serverAddr", discovery.getServerAddr());
        properties.put("namespace", discovery.getNamespace());
        properties.put("group", discovery.getGroup());
        properties.put("username", discovery.getUsername());
        properties.put("password", discovery.getPassword());

        try {
            NamingService namingService = NacosFactory.createNamingService(properties);
            log.info("Nacos NamingService 初始化成功");
            log.info("服务地址: {}", discovery.getServerAddr());
            log.info("命名空间: {}", discovery.getNamespace());
            log.info("分组: {}", discovery.getGroup());
            return namingService;
        } catch (Exception e) {
            log.error("Nacos NamingService 初始化失败", e);
            throw new NacosInitializationException("NamingService初始化失败", e);
        }
    }

    @PostConstruct
    public void init() {
        log.info("Nacos 服务发现配置已启用");
    }
}
