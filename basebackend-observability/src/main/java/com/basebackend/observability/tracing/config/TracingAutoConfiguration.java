package com.basebackend.observability.tracing.config;

import com.basebackend.observability.tracing.context.TracePropagatorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 分布式追踪自动配置
 * <p>
 * 集成所有追踪相关组件，包括：
 * <ul>
 *     <li>上下文传播器配置（W3C TraceContext + Baggage + 业务上下文）</li>
 *     <li>HTTP 追踪配置（Server Filter + Client Interceptor）</li>
 *     <li>采样策略配置（规则采样 + 错误采样 + 延迟采样 + 动态采样）</li>
 * </ul>
 * </p>
 * <p>
 * <b>加载顺序：</b>
 * <ol>
 *     <li>在 Spring Boot Actuator 和 OpenTelemetry 自动配置之后加载</li>
 *     <li>导入子配置类：TracePropagatorConfiguration、HttpTracingConfiguration、SamplerConfiguration</li>
 *     <li>子配置类按需条件加载（通过各自的 @ConditionalOnProperty）</li>
 * </ol>
 * </p>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * observability:
 *   tracing:
 *     enabled: true                    # 启用追踪功能（此配置控制总开关）
 *     propagation:
 *       enabled: true                  # 启用业务上下文传播
 *       business-keys:
 *         - X-Tenant-Id
 *         - X-User-Id
 *     http:
 *       server:
 *         enabled: true                # 启用服务端追踪
 *       client:
 *         enabled: true                # 启用客户端追踪
 *     sampler:
 *       enabled: true                  # 启用采样策略
 *       default-rate: 0.1              # 默认采样率 10%
 *       always-sample-errors: true     # 强制采样错误请求
 *       always-sample-slow: true       # 强制采样慢请求
 *       latency-threshold-ms: 1000     # 慢请求阈值 1 秒
 *       rules:
 *         - url-pattern: "/api/admin/.*"
 *           rate: 1.0                  # 管理 API 100% 采样
 *       dynamic:
 *         enabled: false               # 是否启用动态采样
 *         initial-rate: 0.1
 *         min-rate: 0.01
 *         max-rate: 1.0
 *         target-spans-per-minute: 1000
 *         adjust-interval: 30s
 * }</pre>
 * </p>
 * <p>
 * <b>条件加载：</b>
 * <ul>
 *     <li>整体开关：observability.tracing.enabled=true（默认 true）</li>
 *     <li>上下文传播：observability.tracing.propagation.enabled=true（默认 true）</li>
 *     <li>HTTP 追踪：observability.tracing.http.server/client.enabled=true（默认 true）</li>
 *     <li>采样策略：observability.tracing.sampler.enabled=true（默认 true）</li>
 * </ul>
 * </p>
 * <p>
 * <b>集成点：</b>
 * <ul>
 *     <li>ContextPropagators Bean 自动注入到 OpenTelemetry SDK（通过 OtelAutoConfiguration）</li>
 *     <li>Sampler Bean 自动注入到 SdkTracerProvider（通过 OtelAutoConfiguration）</li>
 *     <li>SpanProcessor Bean 自动注册到 SdkTracerProvider（通过 OtelAutoConfiguration）</li>
 *     <li>HttpServerTracingFilter 自动注册为 ServletFilter（通过 FilterRegistrationBean）</li>
 *     <li>HttpClientTracingInterceptor 自动添加到 RestTemplate（通过 RestTemplateCustomizer）</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see TracingProperties 追踪配置属性
 * @see TracePropagatorConfiguration 上下文传播器配置
 * @see HttpTracingConfiguration HTTP 追踪配置
 * @see SamplerConfiguration 采样器配置
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
@ConditionalOnProperty(
        prefix = "observability.tracing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true  // 默认启用
)
@AutoConfigureAfter(name = {
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "com.basebackend.observability.otel.config.OtelAutoConfiguration"
})
@AutoConfigureBefore(name = {
        "org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration"
})
@Import({
        TracePropagatorConfiguration.class,  // 上下文传播器配置
        HttpTracingConfiguration.class,      // HTTP 追踪配置
        SamplerConfiguration.class           // 采样器配置
})
public class TracingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TracingAutoConfiguration.class);

    public TracingAutoConfiguration(TracingProperties properties) {
        log.info("分布式追踪自动配置已启用: enabled={}", properties.isEnabled());
        log.info("追踪配置概览: propagation={}, http.server={}, http.client={}, sampler.defaultRate={}",
                properties.getPropagation().isEnabled(),
                properties.getHttp().getServer().isEnabled(),
                properties.getHttp().getClient().isEnabled(),
                properties.getSampler().getDefaultRate());
    }
}
