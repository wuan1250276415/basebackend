package com.basebackend.observability.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 增强版RestTemplate配置
 * <p>
 * 改进点：
 * - 可配置的超时参数
 * - 错误处理增强
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EnhancedRestTemplateConfig {

    private final ObservabilityProperties properties;

    /**
     * 增强版RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        ObservabilityProperties.HttpClient httpConfig = properties.getHttpClient();

        // 创建简单的请求工厂配置
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(httpConfig.getConnectTimeout() * 1000);
        factory.setReadTimeout(httpConfig.getReadTimeout() * 1000);

        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(httpConfig.getConnectTimeout()))
                .setReadTimeout(Duration.ofSeconds(httpConfig.getReadTimeout()))
                .build();

        log.info("Enhanced RestTemplate initialized: connectTimeout={}s, readTimeout={}s",
                httpConfig.getConnectTimeout(),
                httpConfig.getReadTimeout());

        return restTemplate;
    }
}
