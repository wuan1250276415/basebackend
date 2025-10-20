package com.basebackend.observability.tracing;

import brave.Tracer;
import brave.sampler.Sampler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * 分布式追踪配置
 * 配置 Brave/Zipkin/Tempo 进行分布式链路追踪
 */
@Slf4j
@Configuration
public class TracingConfig {

    @Value("${observability.tempo.endpoint:http://localhost:9411/api/v2/spans}")
    private String tempoEndpoint;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    /**
     * 配置采样策略
     * probability 为 1.0 表示 100% 采样（开发/测试环境）
     * 生产环境建议降低到 0.1 或更低
     */
    @Bean
    public Sampler sampler() {
        log.info("Configuring tracing sampler with probability: {}", samplingProbability);
        return Sampler.create((float) samplingProbability);
    }

    /**
     * 配置 Zipkin Span Handler（用于 Tempo）
     * 只有在启用追踪时才创建此 Bean
     */
    @Bean
    @ConditionalOnProperty(name = "observability.tempo.enabled", havingValue = "true")
    public AsyncZipkinSpanHandler zipkinSpanHandler() {
        log.info("Configuring Zipkin Span Handler for Tempo: {}", tempoEndpoint);

        URLConnectionSender sender = URLConnectionSender.newBuilder()
                .endpoint(tempoEndpoint)
                .connectTimeout(1000)
                .readTimeout(10000)
                .build();

        return AsyncZipkinSpanHandler.newBuilder(sender)
                .build();
    }
}
