package com.basebackend.common.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 用户上下文拦截器配置
 */
@Data
@ConfigurationProperties(prefix = "basebackend.common.user-context")
public class UserContextProperties {

    /**
    * 是否启用用户上下文拦截器
    */
    private boolean enabled = true;

    /**
     * 拦截路径
     */
    private List<String> pathPatterns = List.of("/**");

    /**
     * 排除路径
     */
    private List<String> excludePatterns = Arrays.asList(
            "/**/auth/login",
            "/**/auth/register",
            "/error",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/favicon.ico",
            "/actuator/**"
    );

    /**
     * 拦截器顺序
     */
    private int order = 10;
}
