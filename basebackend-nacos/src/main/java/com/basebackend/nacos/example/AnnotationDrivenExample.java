/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.config.ConfigService
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.example;

import com.alibaba.nacos.api.config.ConfigService;
import jakarta.annotation.PostConstruct;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AnnotationDrivenExample {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(AnnotationDrivenExample.class);
    private final ConfigService configService;

    public AnnotationDrivenExample(ConfigService configService) {
        this.configService = configService;
    }

    @PostConstruct
    public void init() {
        log.info("=== \u6ce8\u89e3\u9a71\u52a8\u793a\u4f8b ===");
        this.useEnableNacosSupportAnnotation();
        this.configRefreshExample();
    }

    private void useEnableNacosSupportAnnotation() {
        log.info("\n--- \u793a\u4f8b1: @EnableNacosSupport \u6ce8\u89e3 ---");
        log.info("\u901a\u8fc7 @EnableNacosSupport \u6ce8\u89e3\u53ef\u4ee5\u81ea\u52a8\u52a0\u8f7d Nacos \u914d\u7f6e");
        log.info("\u652f\u6301\u7684\u53c2\u6570\uff1a");
        log.info("  - config: \u662f\u5426\u542f\u7528\u914d\u7f6e\u4e2d\u5fc3 (\u9ed8\u8ba4: true)");
        log.info("  - discovery: \u662f\u5426\u542f\u7528\u670d\u52a1\u53d1\u73b0 (\u9ed8\u8ba4: true)");
    }

    private void configRefreshExample() {
        log.info("\n--- \u793a\u4f8b2: @NacosRefreshScope \u914d\u7f6e\u5237\u65b0 ---");
        log.info("@NacosRefreshScope \u6ce8\u89e3\u793a\u4f8b\uff1a");
        log.info("```java");
        log.info("@Component");
        log.info("@NacosRefreshScope");
        log.info("public class MyConfigBean {");
        log.info("    @Value(\"\\${my.config.key:default}\")");
        log.info("    private String configKey;");
        log.info("");
        log.info("    public void printConfig() {");
        log.info("        System.out.println(\"\u5f53\u524d\u914d\u7f6e\uff1a\" + configKey);");
        log.info("    }");
        log.info("}");
        log.info("```");
        try {
            String config = this.configService.getConfig("my-config.yml", "DEFAULT_GROUP", 5000L);
            log.info("\u5f53\u524d\u914d\u7f6e\u5185\u5bb9\uff1a{}", (Object)config);
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u914d\u7f6e\u5931\u8d25", (Throwable)e);
        }
    }
}

