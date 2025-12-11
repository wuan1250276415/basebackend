package com.basebackend.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AWS S3配置属性
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage.s3")
public class S3Properties {

    /**
     * 是否启用S3
     */
    private boolean enabled = false;

    /**
     * S3服务端点
     */
    private String endpoint;

    /**
     * Access Key ID
     */
    private String accessKey;

    /**
     * Secret Access Key
     */
    private String secretKey;

    /**
     * 默认存储桶名称
     */
    private String bucketName;

    /**
     * 存储区域
     */
    private String region;

    /**
     * 自定义域名（可选）
     */
    private String customDomain;

    /**
     * 访问协议
     */
    private String protocol = "https";

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 5000;

    /**
     * socket超时时间（毫秒）
     */
    private int socketTimeout = 5000;

    /**
     * 最大连接数
     */
    private int maxConnections = 100;
}
