package com.basebackend.observability.tracing.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 服务端追踪过滤器
 * <p>
 * 为所有 HTTP 请求创建 SERVER 类型的 Span，并自动填充 HTTP 语义约定属性。
 * 集成了上下文传播、属性填充、异常处理和状态码记录。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *     <li><b>上下文提取</b>：从 HTTP header 提取上游追踪上下文（traceparent, baggage 等）</li>
 *     <li><b>Span 创建</b>：为每个请求创建 SERVER span，span 名称格式为 "HTTP {method} {path}"</li>
 *     <li><b>属性填充</b>：使用 {@link SpanAttributeEnricher} 填充 HTTP、业务等属性</li>
 *     <li><b>状态码记录</b>：记录 HTTP 状态码，4xx/5xx 标记为 ERROR</li>
 *     <li><b>异常处理</b>：捕获请求处理异常，记录到 Span 并重新抛出</li>
 *     <li><b>资源清理</b>：确保 Span 和 Scope 总是正确关闭</li>
 *     <li><b>防重复追踪</b>：避免在 FORWARD/INCLUDE/ERROR dispatch 中重复创建 Span</li>
 * </ul>
 * </p>
 * <p>
 * <b>条件加载：</b>通过 {@link com.basebackend.observability.tracing.config.HttpTracingConfiguration} 管理，
 * 需满足以下条件：
 * <ul>
 *     <li>{@code observability.tracing.enabled=true}（全局追踪开关，默认启用）</li>
 *     <li>{@code observability.tracing.http.server.enabled=true}（服务端追踪开关，默认启用）</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例（自动注册，无需手动配置）：
 * <pre>
 * observability:
 *   tracing:
 *     enabled: true  # 全局追踪开关（默认true）
 *     http:
 *       server:
 *         enabled: true  # 服务端追踪开关（默认true）
 * </pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/">OpenTelemetry HTTP Semantic Conventions</a>
 */
public class HttpServerTracingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(HttpServerTracingFilter.class);

    private final Tracer tracer;
    private final ContextPropagators contextPropagators;
    private final SpanAttributeEnricher spanAttributeEnricher;

    /**
     * HTTP Header Getter，用于从请求头提取上下文
     */
    private static final TextMapGetter<HttpServletRequest> HTTP_SERVLET_REQUEST_GETTER =
            new TextMapGetter<HttpServletRequest>() {
                @Override
                public Iterable<String> keys(HttpServletRequest carrier) {
                    return Collections.list(carrier.getHeaderNames());
                }

                @Override
                public String get(HttpServletRequest carrier, String key) {
                    return carrier.getHeader(key);
                }
            };

    /**
     * 构造函数
     *
     * @param tracer                 OpenTelemetry Tracer
     * @param contextPropagators     上下文传播器
     * @param spanAttributeEnricher  Span 属性填充器
     */
    public HttpServerTracingFilter(
            Tracer tracer,
            ContextPropagators contextPropagators,
            SpanAttributeEnricher spanAttributeEnricher) {
        this.tracer = tracer;
        this.contextPropagators = contextPropagators;
        this.spanAttributeEnricher = spanAttributeEnricher;
        log.info("HTTP 服务端追踪过滤器已初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 只处理 HTTP 请求
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 防重复追踪保护：避免在同一个请求的多次 dispatch 中重复创建 Span
        String attrKey = "otel.server.span.started";
        if (httpRequest.getAttribute(attrKey) != null) {
            log.trace("Span 已创建，跳过重复追踪: uri={}", httpRequest.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 只处理 REQUEST dispatch，跳过 FORWARD, INCLUDE, ERROR, ASYNC
        // 注意：暂不支持异步 Servlet，异步请求会被跳过，避免产生不完整的 Span
        DispatcherType dispatcherType = httpRequest.getDispatcherType();
        if (dispatcherType != DispatcherType.REQUEST) {
            log.trace("跳过非 REQUEST dispatch: type={}, uri={}",
                    dispatcherType, httpRequest.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 标记已处理，防止重复
        httpRequest.setAttribute(attrKey, Boolean.TRUE);

        // 提取上游追踪上下文
        Context extractedContext = contextPropagators.getTextMapPropagator()
                .extract(Context.current(), httpRequest, HTTP_SERVLET_REQUEST_GETTER);

        // 构建 Span 名称：HTTP {method} {path}
        String spanName = buildSpanName(httpRequest);

        // 创建 SERVER span
        Span span = tracer.spanBuilder(spanName)
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        log.trace("创建 SERVER span: name={}, traceId={}, spanId={}",
                spanName, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

        // 准备属性上下文
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(HttpServletRequest.class.getName(), httpRequest);
        attributes.put(HttpServletResponse.class.getName(), httpResponse);

        // 填充 HTTP 属性（请求阶段）
        spanAttributeEnricher.enrich(span, extractedContext, attributes);

        // 将 Span 和提取的上下文设置为当前上下文（保留 Baggage）
        try (Scope scope = extractedContext.with(span).makeCurrent()) {
            try {
                // 执行请求处理链
                chain.doFilter(request, response);

                // 记录 http.route（在处理链之后才能获取）
                recordHttpRoute(span, httpRequest);

                // 记录响应状态码和 http.status_code 属性
                recordResponseStatus(span, httpResponse);

            } catch (Exception ex) {
                // 记录异常到 Span
                recordException(span, ex);

                // 尝试设置 http.status_code（如果响应已提交）
                try {
                    int status = httpResponse.getStatus();
                    if (status > 0) {
                        span.setAttribute("http.status_code", (long) status);
                    } else {
                        // 响应未提交，设置默认 500
                        span.setAttribute("http.status_code", 500L);
                    }
                } catch (Exception ignored) {
                    // 无法获取状态码，设置默认 500
                    span.setAttribute("http.status_code", 500L);
                }

                throw ex; // 重新抛出，不吞异常
            }
        } finally {
            // 确保 Span 总是结束
            span.end();
            log.trace("结束 SERVER span: name={}, status={}", spanName, httpResponse.getStatus());
        }
    }

    /**
     * 构建 Span 名称
     * <p>
     * 格式：HTTP {method} {path}
     * </p>
     * <p>
     * 示例：
     * <ul>
     *     <li>HTTP GET /api/users</li>
     *     <li>HTTP POST /api/orders</li>
     * </ul>
     * </p>
     *
     * @param request HTTP 请求
     * @return Span 名称
     */
    private String buildSpanName(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // 如果有 servlet path pattern，优先使用（如 Spring MVC 的 @RequestMapping）
        Object bestMatchingPattern = request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
        if (bestMatchingPattern != null) {
            path = bestMatchingPattern.toString();
        }

        return "HTTP " + method + " " + path;
    }

    /**
     * 记录 HTTP 路由模式
     * <p>
     * 从 Spring MVC 的请求属性中提取路由模式（如 /api/users/{id}），
     * 并设置 {@code http.route} 属性。
     * </p>
     * <p>
     * <b>注意：</b>此方法必须在 {@code chain.doFilter} 之后调用，
     * 因为路由模式是在请求处理过程中设置的。
     * </p>
     *
     * @param span    当前 Span
     * @param request HTTP 请求
     */
    private void recordHttpRoute(Span span, HttpServletRequest request) {
        try {
            Object pattern = request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
            if (pattern != null) {
                span.setAttribute("http.route", pattern.toString());
                log.trace("记录 HTTP 路由: route={}", pattern);
            }
        } catch (Exception ex) {
            log.debug("记录 HTTP 路由失败", ex);
        }
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
    private void recordResponseStatus(Span span, HttpServletResponse response) {
        try {
            int status = response.getStatus();

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
     * @param exception 异常对象
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
}
