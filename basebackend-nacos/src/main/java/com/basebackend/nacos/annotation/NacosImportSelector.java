/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.ImportSelector
 *  org.springframework.core.type.AnnotationMetadata
 */
package com.basebackend.nacos.annotation;

import com.basebackend.nacos.config.NacosConfigConfiguration;
import com.basebackend.nacos.config.NacosDiscoveryConfiguration;
import com.basebackend.nacos.config.NacosAutoConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.type.AnnotationMetadata;

public class NacosImportSelector
implements ImportSelector, EnvironmentAware {
    private static final String CONFIG_ENABLED_PROPERTY = "nacos.config.enabled";
    private static final String DISCOVERY_ENABLED_PROPERTY = "nacos.discovery.enabled";
    private static final String TOGGLE_PROPERTY_SOURCE_NAME = "basebackendNacosEnableNacosSupportToggles";
    private ConfigurableEnvironment environment;

    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            this.environment = configurableEnvironment;
        }
    }

    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableNacosSupport.class.getName());
        boolean enableConfig = attributes == null || !Boolean.FALSE.equals(attributes.get("config"));
        boolean enableDiscovery = attributes == null || !Boolean.FALSE.equals(attributes.get("discovery"));
        this.applyToggleOverrides(enableConfig, enableDiscovery);
        ArrayList<String> imports = new ArrayList<String>();
        imports.add(NacosAutoConfiguration.class.getName());
        if (enableConfig) {
            imports.add(NacosConfigConfiguration.class.getName());
        }
        if (enableDiscovery) {
            imports.add(NacosDiscoveryConfiguration.class.getName());
        }
        return imports.toArray(new String[0]);
    }

    private void applyToggleOverrides(boolean enableConfig, boolean enableDiscovery) {
        if (this.environment == null) {
            return;
        }
        HashMap<String, Object> overrides = new HashMap<String, Object>();
        overrides.put(CONFIG_ENABLED_PROPERTY, String.valueOf(enableConfig));
        overrides.put(DISCOVERY_ENABLED_PROPERTY, String.valueOf(enableDiscovery));
        MapPropertySource propertySource = new MapPropertySource(TOGGLE_PROPERTY_SOURCE_NAME, overrides);
        MutablePropertySources propertySources = this.environment.getPropertySources();
        if (propertySources.contains(TOGGLE_PROPERTY_SOURCE_NAME)) {
            propertySources.replace(TOGGLE_PROPERTY_SOURCE_NAME, propertySource);
            return;
        }
        propertySources.addFirst(propertySource);
    }
}
