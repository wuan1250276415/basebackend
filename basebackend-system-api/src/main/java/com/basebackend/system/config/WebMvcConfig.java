package com.basebackend.system.config;

import com.basebackend.system.interceptor.SystemUserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SystemUserContextInterceptor systemUserContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(systemUserContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/actuator/**"
                );
    }
}
