package com.basebackend.database.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Database 模块可观测性集成配置
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = {
        "io.opentelemetry.api.trace.Tracer",
        "com.basebackend.observability.slo.annotation.SloMonitored"
})
@ConditionalOnProperty(
        prefix = "basebackend.database.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@AutoConfigureAfter(name = {
        "com.basebackend.observability.otel.config.OtelAutoConfiguration",
        "com.basebackend.observability.slo.config.SloConfiguration"
})
@ComponentScan(basePackages = "com.basebackend.database.observability")
public class DatabaseObservabilityConfiguration {

    public DatabaseObservabilityConfiguration() {
        log.info("Database 可观测性集成已启用");
    }
}
