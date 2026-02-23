package com.basebackend.common.idempotent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性配置属性
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "basebackend.idempotent")
public class IdempotentProperties {

    /**
     * 是否启用幂等性检查
     */
    private boolean enabled = true;

    /**
     * 默认超时时间
     */
    private long defaultTimeout = 5;

    /**
     * 默认时间单位
     */
    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    /**
     * Token 请求头名称
     */
    private String tokenHeader = "X-Idempotent-Token";

    /**
     * Token 有效期（秒）
     */
    private long tokenTimeout = 300;
}
