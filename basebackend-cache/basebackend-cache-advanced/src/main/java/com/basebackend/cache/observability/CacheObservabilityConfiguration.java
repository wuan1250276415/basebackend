package com.basebackend.cache.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Cache 模块可观测性集成配置
 * <p>
 * 自动配置缓存操作的追踪、监控和日志。
 * </p>
 * <p>
 * <b>集成功能：</b>
 * <ul>
 *     <li>OpenTelemetry 分布式追踪</li>
 *     <li>Micrometer 指标收集</li>
 *     <li>SLO 监控</li>
 * </ul>
 * </p>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * basebackend:
 *   cache:
 *     observability:
 *       enabled: true           # 启用可观测性集成（默认 true）
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = {
        "io.opentelemetry.api.trace.Tracer",
        "com.basebackend.observability.slo.aspect.SloMonitored"
})
@ConditionalOnProperty(
        prefix = "basebackend.cache.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@AutoConfigureAfter(name = {
        "com.basebackend.observability.otel.config.OtelAutoConfiguration",
        "com.basebackend.observability.slo.config.SloConfiguration"
})
@ComponentScan(basePackages = "com.basebackend.cache.observability")
public class CacheObservabilityConfiguration {

    public CacheObservabilityConfiguration() {
        log.info("Cache 可观测性集成已启用");
        log.info("  - OpenTelemetry 追踪: 已启用");
        log.info("  - SLO 监控: 已启用");
        log.info("  - Micrometer 指标: 已启用");
    }
}
