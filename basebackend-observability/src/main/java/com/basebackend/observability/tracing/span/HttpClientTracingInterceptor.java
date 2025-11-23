package com.basebackend.observability.tracing.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 客户端追踪拦截器
 * <p>
 * 为 Spring {@link org.springframework.web.client.RestTemplate} 发起的所有 HTTP 请求创建
 * CLIENT 类型的 Span，并自动注入追踪上下文到请求头。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *     <li><b>Span 创建</b>：为每个外部请求创建 CLIENT span，span 名称格式为 "HTTP {method}"</li>
 *     <li><b>上下文注入</b>：将当前追踪上下文注入到 HTTP header（traceparent, baggage 等）</li>
 *     <li><b>属性填充</b>：使用 {@link SpanAttributeEnricher} 填充 HTTP、业务等属性</li>
 *     <li><b>状态码记录</b>：记录 HTTP 响应状态码，4xx/5xx 标记为 ERROR</li>
 *     <li><b>异常处理</b>：捕获网络异常，记录到 Span 并重新抛出</li>
 *     <li><b>延迟结束</b>：Span 在响应关闭时结束，包含响应体读取时间</li>
 *     <li><b>资源清理</b>：确保 Span 和 Scope 总是正确关闭</li>
 * </ul>
 * </p>
 * <p>
 * <b>条件加载：</b>通过 {@link com.basebackend.observability.tracing.config.HttpTracingConfiguration} 管理，
 * 需满足以下条件：
 * <ul>
 *     <li>{@code observability.tracing.enabled=true}（全局追踪开关，默认启用）</li>
 *     <li>{@code observability.tracing.http.client.enabled=true}（客户端追踪开关，默认启用）</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 在配置文件中启用
 * observability:
 *   tracing:
 *     enabled: true  # 全局追踪开关（默认true）
 *     http:
 *       client:
 *         enabled: true  # 客户端追踪开关（默认true）
 *
 * // RestTemplate 会自动注册此拦截器（通过 RestTemplateCustomizer）
 * @Bean
 * public RestTemplate restTemplate(RestTemplateBuilder builder) {
 *     return builder.build();
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>注意：</b>
 * <ul>
 *     <li>此拦截器通过 {@link HttpClientTracingConfig} 自动注册到 RestTemplate</li>
 *     <li>对于 WebClient，需要单独实现 {@code ExchangeFilterFunction}</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/">OpenTelemetry HTTP Semantic Conventions</a>
 */
public class HttpClientTracingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpClientTracingInterceptor.class);

    private final Tracer tracer;
    private final ContextPropagators contextPropagators;
    private final SpanAttributeEnricher spanAttributeEnricher;

    /**
     * HTTP Header Setter，用于将上下文注入到请求头
     */
    private static final TextMapSetter<HttpRequest> HTTP_REQUEST_SETTER =
            new TextMapSetter<HttpRequest>() {
                @Override
                public void set(HttpRequest carrier, String key, String value) {
                    if (carrier != null && carrier.getHeaders() != null) {
                        carrier.getHeaders().set(key, value);
                    }
                }
            };

    /**
     * 构造函数
     *
     * @param tracer                 OpenTelemetry Tracer
     * @param contextPropagators     上下文传播器
     * @param spanAttributeEnricher  Span 属性填充器
     */
    public HttpClientTracingInterceptor(
            Tracer tracer,
            ContextPropagators contextPropagators,
            SpanAttributeEnricher spanAttributeEnricher) {
        this.tracer = tracer;
        this.contextPropagators = contextPropagators;
        this.spanAttributeEnricher = spanAttributeEnricher;
        log.info("HTTP 客户端追踪拦截器已初始化");
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // 构建 Span 名称：HTTP {method}
        String spanName = buildSpanName(request);

        // 创建 CLIENT span
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        log.trace("创建 CLIENT span: name={}, traceId={}, spanId={}",
                spanName, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

        // 准备属性上下文
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(HttpRequest.class.getName(), request);

        // 填充 HTTP 客户端属性（请求阶段）
        Context currentContext = Context.current().with(span);
        spanAttributeEnricher.enrich(span, currentContext, attributes);

        // 将追踪上下文注入到请求头
        contextPropagators.getTextMapPropagator().inject(currentContext, request, HTTP_REQUEST_SETTER);

        log.trace("注入追踪上下文到请求头: traceparent={}, baggage={}",
                request.getHeaders().getFirst("traceparent"),
                request.getHeaders().getFirst("baggage"));

        // 将 Span 设置为当前上下文
        try (Scope scope = span.makeCurrent()) {
            // 执行实际的 HTTP 请求
            ClientHttpResponse response = execution.execute(request, body);

            // 包装响应，延迟 span.end() 到响应关闭时
            return new TracingClientHttpResponseWrapper(response, span);

        } catch (Exception ex) {
            // 记录异常到 Span，立即结束 Span（异常路径）
            try {
                recordException(span, ex);
            } finally {
                span.end();
                log.trace("结束 CLIENT span (异常): name={}, error={}",
                        spanName, ex.getClass().getSimpleName());
            }
            throw ex; // 重新抛出，不吞异常
        }
    }

    /**
     * 构建 Span 名称
     * <p>
     * 格式：HTTP {method}
     * </p>
     * <p>
     * 示例：
     * <ul>
     *     <li>HTTP GET</li>
     *     <li>HTTP POST</li>
     * </ul>
     * </p>
     * <p>
     * <b>注意：</b>对于客户端 Span，通常不包含完整的 URL 路径，因为：
     * <ul>
     *     <li>路径可能包含敏感信息（如 ID）</li>
     *     <li>路径差异会导致 Span 名称过于分散</li>
     *     <li>完整 URL 已经记录在 {@code http.url} 属性中</li>
     * </ul>
     * 如果需要更详细的 Span 名称，可以在 {@code http.url} 属性中查看。
     * </p>
     *
     * @param request HTTP 请求
     * @return Span 名称
     */
    private String buildSpanName(HttpRequest request) {
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        return "HTTP " + method;
    }

    /**
     * 记录响应状态码
     * <p>
     * 设置 {@code http.status_code} 属性，并根据状态码设置 Span 状态：
     * <ul>
     *     <li>2xx, 3xx: OK</li>
     *     <li>4xx: ERROR（客户端错误）</li>
     *     <li>5xx: ERROR（服务端错误）</li>
     * </ul>
     * </p>
     *
     * @param span     当前 Span
     * @param response HTTP 响应
     */
    private void recordResponseStatus(Span span, ClientHttpResponse response) {
        try {
            if (response == null) {
                return;
            }

            int status = response.getStatusCode().value();

            // 设置 http.status_code 属性
            span.setAttribute("http.status_code", (long) status);

            // 设置 Span 状态
            if (status >= 400) {
                // 4xx 和 5xx 都标记为 ERROR
                span.setStatus(StatusCode.ERROR, "HTTP " + status);
            } else {
                // 2xx 和 3xx 标记为 OK
                span.setStatus(StatusCode.OK);
            }

            log.trace("记录响应状态码: status={}, spanStatus={}",
                    status, status >= 400 ? "ERROR" : "OK");

        } catch (Exception ex) {
            log.debug("记录响应状态码失败", ex);
        }
    }

    /**
     * 记录异常到 Span
     * <p>
     * 将异常信息记录到 Span，并设置 Span 状态为 ERROR。
     * </p>
     *
     * @param span      当前 Span
     * @param exception 异常对象（包括 IOException, RestClientException 等）
     */
    private void recordException(Span span, Exception exception) {
        try {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            log.trace("记录异常到 Span: exception={}", exception.getClass().getSimpleName());
        } catch (Exception ex) {
            log.debug("记录异常失败", ex);
        }
    }

    /**
     * 追踪响应包装器
     * <p>
     * 包装 {@link ClientHttpResponse}，延迟 Span 结束到响应关闭时。
     * 这确保了 Span 的延迟包含响应体的读取时间，以及正确捕获响应体读取时的异常。
     * </p>
     */
    private class TracingClientHttpResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final Span span;
        private boolean closed = false;

        TracingClientHttpResponseWrapper(ClientHttpResponse delegate, Span span) {
            this.delegate = delegate;
            this.span = span;

            // 立即记录响应状态码（在body读取前）
            recordResponseStatus(span, delegate);
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public java.io.InputStream getBody() throws IOException {
            return delegate.getBody();
        }

        @Override
        public org.springframework.http.HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            if (!closed) {
                try {
                    delegate.close();
                } finally {
                    span.end();
                    closed = true;
                    log.trace("响应关闭，CLIENT span 结束");
                }
            }
        }
    }
}
