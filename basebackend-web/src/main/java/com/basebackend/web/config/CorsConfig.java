package com.basebackend.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置属性
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Data
@Component
@ConfigurationProperties(prefix = "web.cors")
public class CorsConfig {
    /**
     * 是否启用CORS
     */
    private boolean enabled = true;

    /**
     * 允许的来源域名列表
     */
    private List<String> allowedOrigins = Arrays.asList("http://localhost:3000", "http://localhost:8080");

    /**
     * 允许的方法
     */
    private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

    /**
     * 允许的请求头
     */
    private List<String> allowedHeaders = Arrays.asList("*");

    /**
     * 允许的响应头
     */
    private List<String> exposedHeaders = Arrays.asList("x-request-id", "x-trace-id", "x-total-count");

    /**
     * 是否允许携带凭证（Cookie等）
     */
    private boolean allowCredentials = true;

    /**
     * 预检请求的缓存时间（秒）
     */
    private long maxAge = 3600L;

    /**
     * 是否在响应中暴露 Origin 头
     */
    private boolean exposeOrigin = false;

    /**
     * 动态跨域策略支持
     */
    private boolean dynamicConfig = false;
}
