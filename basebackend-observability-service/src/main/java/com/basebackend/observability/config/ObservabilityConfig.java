package com.basebackend.observability.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 可观测性服务配置
 *
 * @author BaseBackend Team
 * @since 2025-11-19
 */
@Configuration
public class ObservabilityConfig {

    /**
     * RestTemplate配置
     * 用于调用外部API（Prometheus、Loki、Zipkin等）
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
