package com.basebackend.observability.tracing;

import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式追踪配置
 * <p>
 * Spring Boot 4 通过 micrometer-tracing-bridge-otel 自动配置 OTel Tracing，
 * 采样策略通过 management.tracing.sampling.probability 属性配置，
 * Span 导出通过 OTLP exporter 自动完成。
 * </p>
 * <p>
 * 此配置类保留为扩展点，用于注册自定义追踪组件。
 * 原有的 Brave Sampler 和 AsyncZipkinSpanHandler Bean 已被 Spring Boot 4 自动配置替代。
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnClass(Tracer.class)
public class TracingConfig {

    public TracingConfig() {
        log.info("TracingConfig initialized: using Micrometer Tracing with OTel bridge (Spring Boot 4 auto-configuration)");
    }
}
