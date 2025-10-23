package com.basebackend.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Properties;

/**
 * Nacos 自动配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(NacosConfigProperties.class)
@ConditionalOnProperty(prefix = "nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({NacosDiscoveryConfiguration.class, NacosConfigConfiguration.class})
@ComponentScan("com.basebackend.nacos")
public class NacosAutoConfiguration {

    private final NacosConfigProperties nacosConfigProperties;

    public NacosAutoConfiguration(NacosConfigProperties nacosConfigProperties) {
        this.nacosConfigProperties = nacosConfigProperties;
    }

    @Bean
    public NacosConfigManager customNacosConfigManager() {
        log.info("初始化自定义 Nacos 配置管理器");
        return new NacosConfigManager(nacosConfigProperties);
    }

    @Bean
    public ConfigService configService() throws Exception {
        Properties properties = new Properties();
        properties.put("serverAddr", nacosConfigProperties.getConfig().getServerAddr());
        properties.put("namespace", nacosConfigProperties.getConfig().getNamespace());
        properties.put("group", nacosConfigProperties.getConfig().getGroup());
        properties.put("username", nacosConfigProperties.getConfig().getUsername());
        properties.put("password", nacosConfigProperties.getConfig().getPassword());
        log.info("初始化 Nacos ConfigService: {}", properties);
        return NacosFactory.createConfigService(properties);
    }

    @Bean
    public NamingService namingService() throws Exception {
        Properties properties = new Properties();
        properties.put("serverAddr", nacosConfigProperties.getDiscovery().getServerAddr());
        properties.put("namespace", nacosConfigProperties.getDiscovery().getNamespace());
        log.info("初始化 Nacos NamingService: {}", properties);
        return NacosFactory.createNamingService(properties);
    }

}
