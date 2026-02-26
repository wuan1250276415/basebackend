package com.basebackend.observability.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 可观测性服务配置
 *
 * @author BaseBackend Team
 * @since 2025-11-19
 */
@Configuration
public class ObservabilityConfig {

    /**
     * RestClient配置
     * 用于调用外部API（Prometheus、Loki、Zipkin等）
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);

        return builder
                .requestFactory(factory)
                .build();
    }
}
