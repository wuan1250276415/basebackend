package com.basebackend.nacos.annotation;

import com.basebackend.nacos.config.NacosAutoConfiguration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Nacos导入选择器
 * 根据@EnableNacosSupport注解参数动态导入配置
 */
public class NacosImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 获取注解属性
        Boolean configEnabled = (Boolean) importingClassMetadata.getAnnotationAttributes(
                "com.basebackend.nacos.annotation.EnableNacosSupport"
        ).get("config");

        Boolean discoveryEnabled = (Boolean) importingClassMetadata.getAnnotationAttributes(
                "com.basebackend.nacos.annotation.EnableNacosSupport"
        ).get("discovery");

        // 默认都启用
        boolean enableConfig = configEnabled == null || configEnabled;
        boolean enableDiscovery = discoveryEnabled == null || discoveryEnabled;

        // 注意：实际启用还是取决于nacos.enabled配置
        // 这里只是导入配置类，实际加载会通过@ConditionalOnProperty控制
        return new String[]{
            NacosAutoConfiguration.class.getName()
        };
    }
}
