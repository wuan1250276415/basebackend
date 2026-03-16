/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Import
 */
package com.basebackend.nacos.config;

import com.basebackend.nacos.config.NacosConfigManager;
import com.basebackend.nacos.config.NacosConfigProperties;
import com.basebackend.nacos.config.NacosConfigValidator;
import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import com.basebackend.nacos.repository.InMemoryGrayReleaseHistoryRepository;
import com.basebackend.nacos.security.CredentialEncryptionService;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(value={NacosConfigProperties.class})
@ConditionalOnProperty(prefix="nacos", name={"enabled"}, havingValue="true", matchIfMissing=true)
public class NacosAutoConfiguration {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosAutoConfiguration.class);
    private final NacosConfigProperties nacosConfigProperties;
    private final Environment environment;

    @Bean
    @ConditionalOnMissingBean
    public NacosConfigValidator nacosConfigValidator() {
        return new NacosConfigValidator(this.nacosConfigProperties, this.environment);
    }

    @Bean(value={"customNacosConfigManager"})
    @ConditionalOnMissingBean
    public NacosConfigManager customNacosConfigManager() {
        log.info("\u521d\u59cb\u5316\u81ea\u5b9a\u4e49 Nacos \u914d\u7f6e\u7ba1\u7406\u5668");
        return new NacosConfigManager(this.nacosConfigProperties);
    }

    @Bean
    @ConditionalOnMissingBean(value={GrayReleaseHistoryRepository.class})
    public GrayReleaseHistoryRepository grayReleaseHistoryRepository() {
        log.info("\u521d\u59cb\u5316\u5185\u5b58\u7248\u7070\u5ea6\u53d1\u5e03\u5386\u53f2\u4ed3\u50a8");
        return new InMemoryGrayReleaseHistoryRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public CredentialEncryptionService credentialEncryptionService() {
        return new CredentialEncryptionService(this.environment);
    }

    @Generated
    public NacosAutoConfiguration(NacosConfigProperties nacosConfigProperties, Environment environment) {
        this.nacosConfigProperties = nacosConfigProperties;
        this.environment = environment;
    }
}
