/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.config.ConfigService
 *  com.alibaba.nacos.api.naming.NamingService
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.example;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BasicUsageExample {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(BasicUsageExample.class);
    private final ConfigService configService;
    private final NamingService namingService;

    @PostConstruct
    public void init() {
        log.info("=== \u57fa\u672c\u4f7f\u7528\u793a\u4f8b ===");
        this.fetchConfig();
        this.publishConfig();
        this.registerServiceInstance();
        this.queryServiceInstances();
    }

    private void fetchConfig() {
        try {
            log.info("\n--- \u793a\u4f8b1: \u83b7\u53d6\u914d\u7f6e ---");
            String content = this.configService.getConfig("my-config.yml", "DEFAULT_GROUP", 5000L);
            log.info("\u914d\u7f6e\u5185\u5bb9\uff1a{}", (Object)content);
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u914d\u7f6e\u5931\u8d25", (Throwable)e);
        }
    }

    private void publishConfig() {
        try {
            log.info("\n--- \u793a\u4f8b2: \u53d1\u5e03\u914d\u7f6e ---");
            boolean success = this.configService.publishConfig("my-config.yml", "content: value", "DEFAULT_GROUP");
            log.info("\u914d\u7f6e\u53d1\u5e03\u7ed3\u679c\uff1a{}", (Object)(success ? "\u6210\u529f" : "\u5931\u8d25"));
        }
        catch (Exception e) {
            log.error("\u53d1\u5e03\u914d\u7f6e\u5931\u8d25", (Throwable)e);
        }
    }

    private void registerServiceInstance() {
        try {
            log.info("\n--- \u793a\u4f8b3: \u6ce8\u518c\u670d\u52a1\u5b9e\u4f8b ---");
            this.namingService.registerInstance("my-service", "127.0.0.1", 8080, "DEFAULT");
            log.info("\u670d\u52a1\u5b9e\u4f8b\u6ce8\u518c\u6210\u529f\uff1a127.0.0.1:8080");
        }
        catch (Exception e) {
            log.error("\u6ce8\u518c\u670d\u52a1\u5b9e\u4f8b\u5931\u8d25", (Throwable)e);
        }
    }

    private void queryServiceInstances() {
        try {
            log.info("\n--- \u793a\u4f8b4: \u67e5\u8be2\u670d\u52a1\u5b9e\u4f8b ---");
            List<Instance> instances = this.namingService.getAllInstances("my-service");
            log.info("\u67e5\u8be2\u5230 {} \u4e2a\u5b9e\u4f8b", (Object)instances.size());
            instances.forEach(instance -> log.info("  - {}:{} (\u5065\u5eb7: {}, \u6743\u91cd: {})", new Object[]{instance.getIp(), instance.getPort(), instance.isHealthy(), instance.getWeight()}));
        }
        catch (Exception e) {
            log.error("\u67e5\u8be2\u670d\u52a1\u5b9e\u4f8b\u5931\u8d25", (Throwable)e);
        }
    }

    @Generated
    public BasicUsageExample(ConfigService configService, NamingService namingService) {
        this.configService = configService;
        this.namingService = namingService;
    }
}
