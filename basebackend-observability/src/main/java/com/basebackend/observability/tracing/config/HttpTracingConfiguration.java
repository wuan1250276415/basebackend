package com.basebackend.observability.tracing.config;

import com.basebackend.observability.tracing.span.HttpClientTracingInterceptor;
import com.basebackend.observability.tracing.span.HttpServerTracingFilter;
import com.basebackend.observability.tracing.span.SpanAttributeEnricher;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;

/**
 * HTTP 追踪配置
 * <p>
 * 负责注册 HTTP 服务端和客户端追踪组件，包括：
 * <ul>
 *     <li>HTTP 服务端追踪过滤器（{@link HttpServerTracingFilter}）</li>
 *     <li>HTTP 客户端追踪拦截器（{@link HttpClientTracingInterceptor}）</li>
 *     <li>RestClient 追踪定制器（自动注册拦截器到所有 RestClient）</li>
 * </ul>
 * </p>
 * <p>
 * <b>条件加载：</b>
 * <ul>
 *     <li>{@code observability.tracing.enabled=true}（全局追踪开关，默认启用）</li>
 *     <li>{@code observability.tracing.http.server.enabled=true}（服务端追踪开关，默认启用）</li>
 *     <li>{@code observability.tracing.http.client.enabled=true}（客户端追踪开关，默认启用）</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see HttpServerTracingFilter
 * @see HttpClientTracingInterceptor
 */
@Configuration
@ConditionalOnProperty(
        prefix = "observability.tracing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class HttpTracingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HttpTracingConfiguration.class);

    /**
     * 注册 HTTP 服务端追踪过滤器
     * <p>
     * 使用 FilterRegistrationBean 来精确控制过滤器的注册和顺序。
     * </p>
     *
     * @param tracer                OpenTelemetry Tracer
     * @param contextPropagators    上下文传播器
     * @param spanAttributeEnricher Span 属性填充器
     * @return FilterRegistrationBean
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(io.opentelemetry.api.trace.Tracer.class)
    @ConditionalOnProperty(
            prefix = "observability.tracing.http.server",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public FilterRegistrationBean<HttpServerTracingFilter> httpServerTracingFilter(
            Tracer tracer,
            ContextPropagators contextPropagators,
            SpanAttributeEnricher spanAttributeEnricher) {

        HttpServerTracingFilter filter = new HttpServerTracingFilter(tracer, contextPropagators, spanAttributeEnricher);

        FilterRegistrationBean<HttpServerTracingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * 创建 HTTP 客户端追踪拦截器
     *
     * @param tracer                OpenTelemetry Tracer
     * @param contextPropagators    上下文传播器
     * @param spanAttributeEnricher Span 属性填充器
     * @return HttpClientTracingInterceptor
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(io.opentelemetry.api.trace.Tracer.class)
    @ConditionalOnProperty(
            prefix = "observability.tracing.http.client",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public HttpClientTracingInterceptor httpClientTracingInterceptor(
            Tracer tracer,
            ContextPropagators contextPropagators,
            SpanAttributeEnricher spanAttributeEnricher) {
        return new HttpClientTracingInterceptor(tracer, contextPropagators, spanAttributeEnricher);
    }

    /**
     * 创建 RestClient 定制器
     * <p>
     * 自动配置 {@link RestClient} 以启用分布式追踪，将 {@link HttpClientTracingInterceptor}
     * 注册到所有通过 {@link RestClient.Builder} 创建的 RestClient 实例。
     * </p>
     * <p>
     * 使用示例：
     * <pre>{@code
     * // 通过 RestClient.Builder 创建的 RestClient 会自动注册拦截器
     * @Bean
     * public RestClient restClient(RestClient.Builder builder) {
     *     return builder.build();
     * }
     * }</pre>
     * </p>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此配置只影响通过 Spring Boot 管理的 RestClient</li>
     *     <li>如果项目中有直接 RestClient.create() 的代码，需要手动添加拦截器</li>
     *     <li>对于 WebClient，需要单独配置 {@code ExchangeFilterFunction}</li>
     * </ul>
     * </p>
     *
     * @param interceptor HTTP 客户端追踪拦截器
     * @return RestClientCustomizer 实例
     */
    @Bean
    @ConditionalOnClass(RestClient.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(HttpClientTracingInterceptor.class)
    @ConditionalOnProperty(
            prefix = "observability.tracing.http.client",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public RestClientCustomizer tracingRestClientCustomizer(HttpClientTracingInterceptor interceptor) {
        log.info("注册 RestClient 追踪定制器");
        return restClientBuilder -> {
            restClientBuilder.requestInterceptor(interceptor);
            log.debug("为 RestClient 添加追踪拦截器");
        };
    }
}
