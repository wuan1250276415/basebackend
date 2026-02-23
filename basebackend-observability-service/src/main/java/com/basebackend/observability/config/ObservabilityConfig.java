package com.basebackend.observability.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
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
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
