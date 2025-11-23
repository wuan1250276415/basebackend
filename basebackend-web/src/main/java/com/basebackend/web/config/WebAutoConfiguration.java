package com.basebackend.web.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Web 模块自动配置类
 * 自动加载所有Web层组件配置
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({CorsConfig.class, SecurityHeaderConfig.class})
@AutoConfiguration
public class WebAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("BaseBackend Web 模块已加载");
    }

    /**
     * 注册 CORS 配置属性
     */
    @Bean
    @ConditionalOnMissingBean
    public CorsConfig corsConfig() {
        return new CorsConfig();
    }

    /**
     * 注册安全头配置属性
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityHeaderConfig securityHeaderConfig() {
        return new SecurityHeaderConfig();
    }

    /**
     * 提供 WebMetrics Bean（用于其他组件使用）
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        // Micrometer 已在其他模块中自动配置，这里只是提供 Bean
        return null;
    }
}
