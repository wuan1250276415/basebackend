/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.example;

import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.service.GrayReleaseService;
import jakarta.annotation.PostConstruct;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GrayReleaseExample {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(GrayReleaseExample.class);
    private final GrayReleaseService grayReleaseService;

    @PostConstruct
    public void init() {
        log.info("=== \u7070\u5ea6\u53d1\u5e03\u793a\u4f8b ===");
        this.grayReleaseByIp();
        this.grayReleaseByPercentage();
        this.grayReleaseByLabel();
        this.grayReleasePromoteToFull();
        this.grayReleaseRollback();
    }

    private void grayReleaseByIp() {
        log.info("\n--- \u793a\u4f8b1: IP\u7070\u5ea6\u53d1\u5e03 ---");
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("IP");
        grayConfig.setTargetInstances("192.168.1.10,192.168.1.11");
        log.info("\u5f00\u59cb IP \u7070\u5ea6\u53d1\u5e03\uff0c\u76ee\u6807\u5b9e\u4f8b\uff1a192.168.1.10,192.168.1.11");
        log.info("  \u6d88\u606f\uff1a{}", (Object)"\u7070\u5ea6\u53d1\u5e03\u529f\u80fd\u9700\u8981\u5b9e\u9645\u7684 Nacos \u5b9e\u4f8b\u624d\u80fd\u6d4b\u8bd5");
    }

    private void grayReleaseByPercentage() {
        log.info("\n--- \u793a\u4f8b2: \u767e\u5206\u6bd4\u7070\u5ea6\u53d1\u5e03 ---");
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("PERCENTAGE");
        grayConfig.setPercentage(20);
        log.info("\u5f00\u59cb\u767e\u5206\u6bd4\u7070\u5ea6\u53d1\u5e03\uff0c\u7070\u5ea6\u6bd4\u4f8b\uff1a20%");
        log.info("  \u7070\u5ea6\u7b56\u7565\uff1a\u968f\u673a\u9009\u62e9 20% \u7684\u5b9e\u4f8b\u8fdb\u884c\u7070\u5ea6");
    }

    private void grayReleaseByLabel() {
        log.info("\n--- \u793a\u4f8b3: \u6807\u7b7e\u7070\u5ea6\u53d1\u5e03 ---");
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("LABEL");
        grayConfig.setLabels("{\"version\":\"1.0\",\"region\":\"beijing\"}");
        log.info("\u5f00\u59cb\u6807\u7b7e\u7070\u5ea6\u53d1\u5e03\uff0c\u7070\u5ea6\u6761\u4ef6\uff1aversion=1.0, region=beijing");
        log.info("  \u7070\u5ea6\u7b56\u7565\uff1a\u9009\u62e9\u5177\u6709\u6307\u5b9a\u6807\u7b7e\u7684\u5b9e\u4f8b\u8fdb\u884c\u7070\u5ea6");
    }

    private void grayReleasePromoteToFull() {
        log.info("\n--- \u793a\u4f8b4: \u7070\u5ea6\u5168\u91cf\u53d1\u5e03 ---");
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        log.info("\u5f00\u59cb\u7070\u5ea6\u5168\u91cf\u53d1\u5e03");
        log.info("  \u64cd\u4f5c\uff1a\u5c06\u7070\u5ea6\u914d\u7f6e\u63a8\u5e7f\u5230\u6240\u6709\u5b9e\u4f8b");
    }

    private void grayReleaseRollback() {
        log.info("\n--- \u793a\u4f8b5: \u7070\u5ea6\u56de\u6eda ---");
        ConfigInfo originalConfig = new ConfigInfo();
        originalConfig.setDataId("my-config.yml");
        originalConfig.setContent("old config content");
        originalConfig.setGroup("DEFAULT_GROUP");
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        log.info("\u5f00\u59cb\u7070\u5ea6\u56de\u6eda");
        log.info("  \u64cd\u4f5c\uff1a\u5c06\u914d\u7f6e\u56de\u6eda\u5230\u7070\u5ea6\u524d\u7684\u7248\u672c");
    }

    @Generated
    public GrayReleaseExample(GrayReleaseService grayReleaseService) {
        this.grayReleaseService = grayReleaseService;
    }
}

