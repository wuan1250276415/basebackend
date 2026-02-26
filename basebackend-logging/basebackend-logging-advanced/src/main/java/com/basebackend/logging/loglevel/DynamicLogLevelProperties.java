package com.basebackend.logging.loglevel;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态日志级别配置属性
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Data
@ConfigurationProperties(prefix = "basebackend.logging.log-level")
public class DynamicLogLevelProperties {

    /**
     * 是否启用动态日志级别管理
     */
    private boolean enabled = true;

    /**
     * 默认 TTL（秒），0 表示永久生效
     */
    private int defaultTtlSeconds = 0;

    /**
     * 最大 TTL（秒），防止设置过长的临时级别
     */
    private int maxTtlSeconds = 3600;

    /**
     * Nacos 集成配置
     */
    private NacosIntegration nacos = new NacosIntegration();

    @Data
    public static class NacosIntegration {
        private boolean enabled = false;
        private String dataId = "logging-level";
        private String group = "DEFAULT_GROUP";
    }
}
