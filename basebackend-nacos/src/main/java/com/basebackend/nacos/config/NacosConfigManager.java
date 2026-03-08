/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.context.config.annotation.RefreshScope
 */
package com.basebackend.nacos.config;

import com.basebackend.nacos.config.NacosConfigProperties;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@RefreshScope
public class NacosConfigManager {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosConfigManager.class);
    private final NacosConfigProperties nacosConfigProperties;

    public NacosConfigProperties.Discovery getDiscoveryConfig() {
        return this.nacosConfigProperties.getDiscovery();
    }

    public NacosConfigProperties.Config getConfigConfig() {
        return this.nacosConfigProperties.getConfig();
    }

    public boolean isDiscoveryEnabled() {
        return this.nacosConfigProperties.getDiscovery().isEnabled();
    }

    public boolean isConfigEnabled() {
        return this.nacosConfigProperties.getConfig().isEnabled();
    }

    @Generated
    public NacosConfigManager(NacosConfigProperties nacosConfigProperties) {
        this.nacosConfigProperties = nacosConfigProperties;
    }
}

