package com.basebackend.observability.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * 追踪配置
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Configuration
public class TracingConfig {

    @Value("${spring.zipkin.base-url:http://localhost:9411}")
    private String zipkinUrl;

    @Bean
    public AsyncZipkinSpanHandler asyncZipkinSpanHandler() {
        return AsyncZipkinSpanHandler.create(
            URLConnectionSender.create(zipkinUrl + "/api/v2/spans")
        );
    }
}
