package com.basebackend.scheduler.monitoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web监控配置
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MonitoringInterceptor monitoringInterceptor;

    public WebConfig(MonitoringInterceptor monitoringInterceptor) {
        this.monitoringInterceptor = monitoringInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/api/**", "/workflow/**", "/camunda/**");
    }
}
