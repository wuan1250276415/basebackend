package com.basebackend.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 结构化日志配置
 *
 * <p>绑定配置前缀 {@code basebackend.logging.structured}，
 * 由 {@link LoggingAutoConfiguration} 通过 {@code @EnableConfigurationProperties} 激活。
 *
 * <p>注意：此类不使用 {@code @Configuration}，防止被组件扫描双重注册。
 * 同时将前缀从 {@code logging.structured} 改为 {@code basebackend.logging.structured}，
 * 避免与 Spring Boot 保留的 {@code logging.*} 命名空间冲突。
 */
@Data
@ConfigurationProperties(prefix = "basebackend.logging.structured")
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
     * Loki 服务地址（无默认值，需在部署时明确配置）
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
