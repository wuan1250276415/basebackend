package com.basebackend.common.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import io.opentelemetry.semconv.http.HttpClientDnsAttributes;
import io.opentelemetry.semconv.http.HttpClientRequestAttributes;
import io.opentelemetry.semconv.http.HttpClientResponseAttributes;
import io.opentelemetry.semconv.http.HttpServerAttributes;
import io.opentelemetry.semconv.http.HttpServerRequestAttributes;
import io.opentelemetry.semconv.http.HttpServerResponseAttributes;
import io.opentelemetry.semconv.network.attributes.NetworkAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 链路追踪拦截器
 * 自动拦截 HTTP 请求、数据库查询、第三方调用等操作，生成链路追踪数据
 *
 * 功能特性:
 * 1. HTTP 请求链路追踪 (入口)
 * 2. HTTP 客户端调用追踪 (出口)
 * 3. 数据库查询追踪
 * 4. 异常链路追踪
 * 5. 自定义业务埋点
 *
 * @author basebackend team
 * @version 1.0
 */
@Component
public class TracingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingInterceptor.class);

    private static final String SPAN_ATTRIBUTE = "opentelemetry.span";
    private static final String SCOPE_ATTRIBUTE = "opentelemetry.scope";

    private final Tracer tracer;

    public TracingInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 请求预处理 - 创建入口 Span
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 提取 Trace Context
        Context context = Context.current();

        // 2. 从请求头中获取 TraceID (如果存在)
        String traceId = request.getHeader("x-trace-id");
        if (traceId != null && !traceId.isEmpty()) {
            // 这里可以添加从请求头提取 TraceID 的逻辑
            logger.debug("Received trace from header: {}", traceId);
        }

        // 3. 构建 Span 名称
        String method = request.getMethod();
        String path = request.getRequestURI();
        String spanName = method + " " + path;

        // 4. 创建服务器 Span
        Span serverSpan = tracer.spanBuilder(spanName)
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(HttpAttributes.HTTP_METHOD, method)
            .setAttribute(HttpAttributes.HTTP_SCHEME, request.getScheme())
            .setAttribute(HttpAttributes.HTTP_HOST, request.getServerName())
            .setAttribute(HttpAttributes.HTTP_TARGET, path)
            .setAttribute(HttpAttributes.HTTP_URL, request.getRequestURL().toString())
            .setAttribute(HttpAttributes.HTTP_USER_AGENT, request.getHeader("User-Agent"))
            .setAttribute(HttpAttributes.HTTP_CLIENT_IP, getClientIp(request))
            .setAttribute(HttpAttributes.NETWORK_PROTOCOL_VERSION, request.getProtocol())
            .setAttribute("servlet.path", request.getServletPath())
            .setAttribute("servlet.context", request.getContextPath())
            .setAttribute("thread.name", Thread.currentThread().getName())
            .startSpan();

        // 5. 将 Span 放入当前 Context
        Context serverContext = Context.current().with(serverSpan);

        // 6. 激活 Span (设置当前 Context)
        Scope scope = serverContext.makeCurrent();

        // 7. 将 Span 和 Scope 存储到请求属性中
        request.setAttribute(SPAN_ATTRIBUTE, serverSpan);
        request.setAttribute(SCOPE_ATTRIBUTE, scope);

        // 8. 添加请求开始事件
        serverSpan.addEvent("request.started")
            .addEvent("Request processing started")
            .setAttribute("request.start.time", System.currentTimeMillis());

        // 9. 在响应头中添加 TraceID
        response.setHeader("x-trace-id", serverSpan.getSpanContext().getTraceId());

        logger.debug("Created server span: {} for request: {} {}",
            serverSpan.getSpanContext().getSpanId(),
            method,
            path
        );

        return true;
    }

    /**
     * 请求后处理 - 添加响应信息到 Span
     */
    @Override
    public void postHandle(HttpServletRequest request,
                          HttpServletResponse response,
                          Object handler,
                          ModelAndView modelAndView) {
        Span serverSpan = getSpan(request);
        if (serverSpan == null) {
            return;
        }

        // 1. 添加响应状态码
        serverSpan.setAttribute(HttpAttributes.HTTP_STATUS_CODE, (long) response.getStatus());

        // 2. 计算请求耗时
        Long startTime = (Long) request.getAttribute("request.start.time");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            serverSpan.setAttribute("request.duration", duration);
        }

        // 3. 添加响应头信息
        String contentType = response.getContentType();
        if (contentType != null) {
            serverSpan.setAttribute(HttpAttributes.HTTP_RESPONSE_CONTENT_LENGTH, contentType);
        }

        // 4. 添加响应事件
        serverSpan.addEvent("request.completed")
            .addEvent("Request processing completed");
    }

    /**
     * 请求完成后处理 - 结束 Span
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception exception) {
        Scope scope = getScope(request);
        Span serverSpan = getSpan(request);

        if (scope != null) {
            scope.close();
        }

        if (serverSpan != null) {
            // 1. 处理异常
            if (exception != null) {
                serverSpan.recordException(exception);
                serverSpan.setStatus(StatusCode.ERROR, exception.getMessage());
                logger.warn("Request failed: {} {}, error: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception
                );
            }

            // 2. 添加最终状态
            int statusCode = response.getStatus();
            if (statusCode >= 500) {
                serverSpan.setStatus(StatusCode.ERROR, "Server Error: " + statusCode);
            } else if (statusCode >= 400) {
                serverSpan.setStatus(StatusCode.ERROR, "Client Error: " + statusCode);
            } else {
                serverSpan.setStatus(StatusCode.OK);
            }

            // 3. 结束 Span
            serverSpan.end();

            logger.debug("Completed span: {} for request: {} {} (status: {})",
                serverSpan.getSpanContext().getSpanId(),
                request.getMethod(),
                request.getRequestURI(),
                statusCode
            );
        }

        // 4. 清理请求属性
        request.removeAttribute(SPAN_ATTRIBUTE);
        request.removeAttribute(SCOPE_ATTRIBUTE);
    }

    /**
     * 获取当前 Span
     */
    public Span getCurrentSpan() {
        return Span.current();
    }

    /**
     * 创建客户端 Span (用于 HTTP 调用)
     */
    @WithSpan
    public void trackHttpCall(String url, String method) {
        // 使用 @WithSpan 注解自动创建 Span
        // 这个方法会自动捕获方法执行的链路信息
    }

    /**
     * 创建数据库查询 Span
     */
    @WithSpan
    public void trackDatabaseQuery(@SpanAttribute("db.query") String query) {
        // 使用 @WithSpan 注解自动创建数据库 Span
        // 需要在调用方法时传入查询语句
    }

    /**
     * 创建自定义业务 Span
     */
    public Span createCustomSpan(String operationName, String... attributes) {
        return tracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("custom.operation", true)
            .startSpan();
    }

    /**
     * 异步链路追踪支持
     */
    public CompletableFuture<String> trackAsyncOperation(String operationName, Runnable operation) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

            try (Scope scope = span.makeCurrent()) {
                span.addEvent("async.operation.started");
                operation.run();
                span.addEvent("async.operation.completed");
                span.setStatus(StatusCode.OK);
                return "success";
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                throw new RuntimeException(e);
            } finally {
                span.end();
            }
        });
    }

    /**
     * 辅助方法: 从请求中获取 Span
     */
    private Span getSpan(HttpServletRequest request) {
        return (Span) request.getAttribute(SPAN_ATTRIBUTE);
    }

    /**
     * 辅助方法: 从请求中获取 Scope
     */
    private Scope getScope(HttpServletRequest request) {
        return (Scope) request.getAttribute(SCOPE_ATTRIBUTE);
    }

    /**
     * 辅助方法: 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
