package com.basebackend.gateway.cors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态跨域配置
 * <p>
 * 支持通过配置文件和运行时 API 动态修改 CORS 规则，无需重启网关。
 *
 * <pre>
 * gateway:
 *   dynamic-cors:
 *     enabled: true
 *     allowed-origins:
 *       - http://localhost:3000
 *       - https://admin.example.com
 *     allowed-methods:
 *       - GET
 *       - POST
 *       - PUT
 *       - DELETE
 *     allowed-headers:
 *       - "*"
 *     allow-credentials: true
 *     max-age: 3600
 * </pre>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "gateway.dynamic-cors")
public class DynamicCorsProperties {

    /** 是否启用动态 CORS */
    private boolean enabled = false;

    /** 允许的源 */
    private List<String> allowedOrigins = new ArrayList<>();

    /** 允许的 HTTP 方法 */
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    /** 允许的请求头 */
    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    /** 暴露的响应头 */
    private List<String> exposedHeaders = new ArrayList<>();

    /** 是否允许携带凭证 */
    private boolean allowCredentials = true;

    /** 预检请求缓存时间（秒） */
    private long maxAge = 3600;

    /**
     * 转换为 Spring CorsConfiguration
     */
    public CorsConfiguration toCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.isEmpty() ? List.of("*") : allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setExposedHeaders(exposedHeaders);
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        return config;
    }

    /**
     * 动态添加允许的源
     */
    public void addAllowedOrigin(String origin) {
        if (!allowedOrigins.contains(origin)) {
            allowedOrigins.add(origin);
            log.info("动态添加 CORS 允许源: {}", origin);
        }
    }

    /**
     * 动态移除允许的源
     */
    public void removeAllowedOrigin(String origin) {
        allowedOrigins.remove(origin);
        log.info("动态移除 CORS 允许源: {}", origin);
    }
}
