/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.config.ConfigService
 *  com.alibaba.nacos.api.config.listener.Listener
 *  com.alibaba.nacos.api.exception.NacosException
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.util.DigestUtils
 */
package com.basebackend.nacos.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.basebackend.nacos.isolation.ConfigIsolationContext;
import com.basebackend.nacos.isolation.ConfigIsolationManager;
import com.basebackend.nacos.model.ConfigInfo;
import java.nio.charset.StandardCharsets;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class NacosConfigService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosConfigService.class);
    private final ConfigService nacosConfigService;
    private final ConfigIsolationManager isolationManager;

    public String getConfig(ConfigInfo configInfo) throws NacosException {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        String namespace = context.buildNamespace();
        log.info("\u83b7\u53d6\u914d\u7f6e: dataId={}, group={}, namespace={}", new Object[]{dataId, group, namespace});
        return this.nacosConfigService.getConfig(dataId, group, 5000L);
    }

    public boolean publishConfig(ConfigInfo configInfo) throws NacosException {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        String content = configInfo.getContent();
        String type = configInfo.getType() != null ? configInfo.getType() : "yaml";
        log.info("\u53d1\u5e03\u914d\u7f6e: dataId={}, group={}, type={}", new Object[]{dataId, group, type});
        return this.nacosConfigService.publishConfig(dataId, group, content, type);
    }

    public boolean removeConfig(ConfigInfo configInfo) throws NacosException {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.info("\u5220\u9664\u914d\u7f6e: dataId={}, group={}", (Object)dataId, (Object)group);
        return this.nacosConfigService.removeConfig(dataId, group);
    }

    public void addListener(ConfigInfo configInfo, Listener listener) throws NacosException {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.info("\u6dfb\u52a0\u914d\u7f6e\u76d1\u542c\u5668: dataId={}, group={}", (Object)dataId, (Object)group);
        this.nacosConfigService.addListener(dataId, group, listener);
    }

    public void removeListener(ConfigInfo configInfo, Listener listener) {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.info("\u79fb\u9664\u914d\u7f6e\u76d1\u542c\u5668: dataId={}, group={}", (Object)dataId, (Object)group);
        this.nacosConfigService.removeListener(dataId, group, listener);
    }

    public String calculateMd5(String content) {
        if (content == null) {
            return null;
        }
        return DigestUtils.md5DigestAsHex((byte[])content.getBytes(StandardCharsets.UTF_8));
    }

    private ConfigIsolationContext buildContext(ConfigInfo configInfo) {
        return this.isolationManager.createContext(configInfo.getEnvironment(), configInfo.getTenantId(), configInfo.getAppId());
    }

    @Generated
    public NacosConfigService(ConfigService nacosConfigService, ConfigIsolationManager isolationManager) {
        this.nacosConfigService = nacosConfigService;
        this.isolationManager = isolationManager;
    }
}

