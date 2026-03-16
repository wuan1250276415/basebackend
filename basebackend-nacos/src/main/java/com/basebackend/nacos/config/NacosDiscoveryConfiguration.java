/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.NacosFactory
 *  com.alibaba.nacos.api.naming.NamingService
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
import com.alibaba.nacos.api.naming.NamingService;
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
@ConditionalOnProperty(prefix="nacos.discovery", name={"enabled"}, havingValue="true", matchIfMissing=true)
@Order(value=2)
public class NacosDiscoveryConfiguration {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosDiscoveryConfiguration.class);
    private final NacosConfigProperties nacosConfigProperties;
    private final CredentialEncryptionService credentialEncryptionService;

    @Bean
    @ConditionalOnMissingBean
    public NamingService namingService() {
        NacosConfigProperties.Discovery discovery = this.nacosConfigProperties.getDiscovery();
        Properties properties = this.buildClientProperties(discovery);
        try {
            NamingService namingService = NacosFactory.createNamingService((Properties)properties);
            log.info("Nacos NamingService \u521d\u59cb\u5316\u6210\u529f");
            log.info("\u670d\u52a1\u5730\u5740: {}", (Object)discovery.getServerAddr());
            log.info("\u547d\u540d\u7a7a\u95f4: {}", (Object)discovery.getNamespace());
            log.info("\u5206\u7ec4: {}", (Object)discovery.getGroup());
            return namingService;
        }
        catch (Exception e) {
            log.error("Nacos NamingService \u521d\u59cb\u5316\u5931\u8d25", (Throwable)e);
            throw new NacosInitializationException("NamingService\u521d\u59cb\u5316\u5931\u8d25", e);
        }
    }

    Properties buildClientProperties(NacosConfigProperties.Discovery discovery) {
        Properties properties = new Properties();
        properties.put("serverAddr", discovery.getServerAddr());
        properties.put("namespace", discovery.getNamespace());
        properties.put("group", discovery.getGroup());

        String username = this.credentialEncryptionService.decryptIfNeeded(discovery.getUsername());
        String password = this.credentialEncryptionService.decryptIfNeeded(discovery.getPassword());
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
        log.info("Nacos \u670d\u52a1\u53d1\u73b0\u914d\u7f6e\u5df2\u542f\u7528");
    }

    @Generated
    public NacosDiscoveryConfiguration(NacosConfigProperties nacosConfigProperties, CredentialEncryptionService credentialEncryptionService) {
        this.nacosConfigProperties = nacosConfigProperties;
        this.credentialEncryptionService = credentialEncryptionService;
    }
}
