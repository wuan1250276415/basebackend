package com.basebackend.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 结构化日志配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "logging.structured")
public class StructuredLogConfig {

    /**
     * 是否启用结构化日志
     */
    private boolean enabled = true;

    /**
     * 是否启用 Loki 日志推送
     */
    private boolean lokiEnabled = false;

    /**
     * Loki 服务地址
     */
    private String lokiUrl = "http://localhost:3100/loki/api/v1/push";

    /**
     * 日志批量发送大小
     */
    private int batchSize = 100;

    /**
     * 日志批量发送超时时间（秒）
     */
    private int batchTimeout = 10;

    /**
     * 服务名称
     */
    private String serviceName = "basebackend";

    /**
     * 环境
     */
    private String environment = "dev";

    /**
     * 是否包含调用栈信息
     */
    private boolean includeStackTrace = true;

    /**
     * 是否包含 MDC 信息
     */
    private boolean includeMdc = true;

    /**
     * 日志保留天数
     */
    private int retentionDays = 7;
}
