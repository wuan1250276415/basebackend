/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.NacosFactory
 *  com.alibaba.nacos.api.config.ConfigService
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.core.annotation.Order
 */
package com.basebackend.nacos.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.basebackend.nacos.config.NacosConfigProperties;
import com.basebackend.nacos.exception.NacosInitializationException;
import com.basebackend.nacos.security.CredentialEncryptionService;
import jakarta.annotation.PostConstruct;
import java.util.Properties;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="nacos.config", name={"enabled"}, havingValue="true", matchIfMissing=true)
@Order(value=1)
public class NacosConfigConfiguration {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosConfigConfiguration.class);
    private final NacosConfigProperties nacosConfigProperties;
    private final CredentialEncryptionService credentialEncryptionService;

    @Bean
    @ConditionalOnMissingBean
    public ConfigService configService() {
        NacosConfigProperties.Config config = this.nacosConfigProperties.getConfig();
        Properties properties = this.buildClientProperties(config);
        try {
            ConfigService configService = NacosFactory.createConfigService((Properties)properties);
            log.info("Nacos ConfigService \u521d\u59cb\u5316\u6210\u529f");
            log.info("\u914d\u7f6e\u4e2d\u5fc3\u5730\u5740: {}", (Object)config.getServerAddr());
            log.info("\u547d\u540d\u7a7a\u95f4: {}", (Object)config.getNamespace());
            log.info("\u5206\u7ec4: {}", (Object)config.getGroup());
            return configService;
        }
        catch (Exception e) {
            log.error("Nacos ConfigService \u521d\u59cb\u5316\u5931\u8d25", (Throwable)e);
            throw new NacosInitializationException("ConfigService\u521d\u59cb\u5316\u5931\u8d25", e);
        }
    }

    Properties buildClientProperties(NacosConfigProperties.Config config) {
        Properties properties = new Properties();
        properties.put("serverAddr", config.getServerAddr());
        properties.put("namespace", config.getNamespace());
        properties.put("group", config.getGroup());

        String username = this.credentialEncryptionService.decryptIfNeeded(config.getUsername());
        String password = this.credentialEncryptionService.decryptIfNeeded(config.getPassword());
        if (StringUtils.hasText((String)username)) {
            properties.put("username", username);
        }
        if (StringUtils.hasText((String)password)) {
            properties.put("password", password);
        }
        return properties;
    }

    @PostConstruct
    public void init() {
        log.info("Nacos \u914d\u7f6e\u4e2d\u5fc3\u914d\u7f6e\u5df2\u542f\u7528");
    }

    @Generated
    public NacosConfigConfiguration(NacosConfigProperties nacosConfigProperties, CredentialEncryptionService credentialEncryptionService) {
        this.nacosConfigProperties = nacosConfigProperties;
        this.credentialEncryptionService = credentialEncryptionService;
    }
}
