package com.basebackend.common.observability;

import com.basebackend.common.logging.StructuredLogging;
import com.basebackend.common.metrics.BusinessMetrics;
import com.basebackend.common.metrics.PerformanceMetrics;
import com.basebackend.common.tracing.TracingUtil;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 可观测性配置类
 * 整合链路追踪、日志和指标，实现端到端可观测性
 *
 * 核心功能:
 * 1. 统一可观测性上下文管理
 * 2. 跨组件链路追踪
 * 3. 日志与追踪关联
 * 4. 指标与追踪关联
 * 5. 自动埋点
 * 6. 故障诊断
 *
 * @author basebackend team
 * @version 1.0
 */
@Configuration
public class ObservabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    @Value("${otel.service.name:basebackend-service}")
    private String serviceName;

    @Value("${otel.enabled:true}")
    private boolean otelEnabled;

    /**
     * 端到端可观测性管理器
     */
    @Bean
    @Primary
    public ObservabilityManager observabilityManager(
            Tracer tracer,
            TracingUtil tracingUtil,
            BusinessMetrics businessMetrics,
            PerformanceMetrics performanceMetrics,
            MeterRegistry meterRegistry) {
        return new ObservabilityManager(
            tracer,
            tracingUtil,
            businessMetrics,
            performanceMetrics,
            meterRegistry
        );
    }

    /**
     * 可观测性 HTTP 拦截器
     * 自动记录请求、响应、追踪 ID
     */
    @Bean
    public HandlerInterceptor observabilityInterceptor(ObservabilityManager observabilityManager) {
        return new ObservabilityInterceptor(observabilityManager);
    }

    /**
     * Web MVC 配置
     */
    @Bean
    public WebMvcConfigurer observabilityWebMvcConfigurer(HandlerInterceptor observabilityInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(observabilityInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/health", "/actuator/health", "/metrics", "/prometheus");
            }
        };
    }

    /**
     * 可观测性管理器
     * 统一管理追踪、日志、指标
     */
    public static class ObservabilityManager {

        private final Tracer tracer;
        private final TracingUtil tracingUtil;
        private final BusinessMetrics businessMetrics;
        private final PerformanceMetrics performanceMetrics;
        private final MeterRegistry meterRegistry;

        public ObservabilityManager(
                Tracer tracer,
                TracingUtil tracingUtil,
                BusinessMetrics businessMetrics,
                PerformanceMetrics performanceMetrics,
                MeterRegistry meterRegistry) {
            this.tracer = tracer;
            this.tracingUtil = tracingUtil;
            this.businessMetrics = businessMetrics;
            this.performanceMetrics = performanceMetrics;
            this.meterRegistry = meterRegistry;
        }

        // ==================== 追踪相关方法 ====================

        /**
         * 开始一个新的操作追踪
         */
        public Span startOperation(String operationName, Map<String, String> attributes) {
            Span span = tracer.spanBuilder(operationName)
                .setKind(Span.Kind.INTERNAL)
                .startSpan();

            if (attributes != null) {
                attributes.forEach(span::setAttribute);
            }

            return span;
        }

        /**
         * 在追踪上下文中执行操作
         */
        public <T> T executeInTrace(String operationName, Map<String, String> attributes, Consumer<Span> operation) {
            Span span = startOperation(operationName, attributes);
            try (Context context = Context.current().with(span)) {
                operation.accept(span);
                span.setStatus(Span.Status.OK);
                return null;
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(Span.Status.ERROR);
                throw e;
            } finally {
                span.end();
            }
        }

        /**
         * 异步操作追踪
         */
        public <T> CompletableFuture<T> executeAsyncInTrace(String operationName, SupplierWithTrace<T> supplier) {
            return CompletableFuture.supplyAsync(() -> {
                return supplier.get(tracer);
            });
        }

        // ==================== 日志相关方法 ====================

        /**
         * 记录结构化日志并关联追踪信息
         */
        public void logInfo(String message, String traceId, Map<String, Object> fields) {
            Map<String, Object> allFields = new HashMap<>();
            if (fields != null) {
                allFields.putAll(fields);
            }

            // 添加追踪信息
            if (traceId != null) {
                allFields.put("trace_id", traceId);
                String spanId = getCurrentSpanId();
                if (spanId != null) {
                    allFields.put("span_id", spanId);
                }
            }

            StructuredLogging.info(message, traceId, allFields);
        }

        /**
         * 记录业务操作日志
         */
        public void logBusinessOperation(String operation, String entityType, String entityId,
                                       String status, String traceId, Map<String, Object> details) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("operation", operation);
            fields.put("entity_type", entityType);
            fields.put("entity_id", entityId);
            fields.put("status", status);

            if (details != null) {
                fields.putAll(details);
            }

            logInfo("Business operation: " + operation, traceId, fields);
        }

        // ==================== 指标相关方法 ====================

        /**
         * 记录业务指标
         */
        public void recordBusinessMetric(String metricName, String operation, String entityType,
                                       long durationMs, Map<String, String> tags) {
            if (tags == null) {
                tags = new HashMap<>();
            }
            tags.put("operation", operation);
            tags.put("entity_type", entityType);

            businessMetrics.recordBusinessOperation(operation, entityType, "unknown", durationMs, 0);

            // 记录指标
            meterRegistry.counter(metricName, tags).increment();
        }

        /**
         * 记录性能指标
         */
        public void recordPerformanceMetric(String metricName, String endpoint, String method,
                                          int statusCode, long durationMs, long responseSize) {
            performanceMetrics.recordHttpRequest(method, endpoint, statusCode, durationMs, 0, responseSize);

            // 记录指标
            Map<String, String> tags = Map.of(
                "endpoint", endpoint,
                "method", method,
                "status_code", String.valueOf(statusCode)
            );
            meterRegistry.timer(metricName, tags).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        // ==================== 综合方法 ====================

        /**
         * 记录端到端操作
         */
        public void recordEndToEndOperation(String operationName, String operationType,
                                          String entityId, long durationMs, boolean success,
                                          Map<String, Object> customFields) {
            String traceId = getCurrentTraceId();

            // 1. 记录日志
            Map<String, Object> logFields = new HashMap<>();
            logFields.put("operation", operationName);
            logFields.put("operation_type", operationType);
            logFields.put("entity_id", entityId);
            logFields.put("duration_ms", durationMs);
            logFields.put("success", success);

            if (customFields != null) {
                logFields.putAll(customFields);
            }

            String status = success ? "success" : "failure";
            logBusinessOperation(operationName, operationType, entityId, status, traceId, logFields);

            // 2. 记录指标
            businessMetrics.recordBusinessOperation(
                operationType, "operation", entityId, durationMs, 0
            );

            // 3. 记录性能指标
            if (success) {
                meterRegistry.counter("operation_success_total", "operation", operationName).increment();
            } else {
                meterRegistry.counter("operation_failure_total", "operation", operationName).increment();
                performanceMetrics.incrementErrorCount();
            }
        }

        /**
         * 诊断问题
         */
        public void diagnose(String problemType, String description, String traceId,
                           Map<String, Object> context) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("problem_type", problemType);
            fields.put("description", description);

            if (context != null) {
                fields.putAll(context);
            }

            logger.error("Problem diagnosed: {} - TraceID: {}",
                description,
                traceId != null ? traceId : "unknown"
            );

            StructuredLogging.error("Problem diagnosed: " + description, traceId, fields);
        }

        // ==================== 辅助方法 ====================

        private String getCurrentTraceId() {
            return tracingUtil.getCurrentTraceId();
        }

        private String getCurrentSpanId() {
            return tracingUtil.getCurrentSpanId();
        }
    }

    /**
     * HTTP 拦截器
     */
    public static class ObservabilityInterceptor implements HandlerInterceptor {

        private final ObservabilityManager observabilityManager;

        public ObservabilityInterceptor(ObservabilityManager observabilityManager) {
            this.observabilityManager = observabilityManager;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String traceId = request.getHeader("x-trace-id");
            if (traceId == null) {
                traceId = StructuredLogging.generateTraceId();
            }

            // 设置 MDC 上下文
            StructuredLogging.setMdcContext(traceId, null);

            // 记录请求开始
            request.setAttribute("request_start_time", System.currentTimeMillis());
            request.setAttribute("trace_id", traceId);

            // 添加响应头
            response.setHeader("x-trace-id", traceId);

            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
            // 记录请求完成
            String traceId = (String) request.getAttribute("trace_id");
            Long startTime = (Long) request.getAttribute("request_start_time");

            if (traceId != null && startTime != null) {
                long duration = System.currentTimeMillis() - startTime;

                observabilityManager.recordEndToEndOperation(
                    "http_request",
                    "api",
                    request.getRequestURI(),
                    duration,
                    response.getStatus() < 400,
                    Map.of(
                        "method", request.getMethod(),
                        "status_code", response.getStatus(),
                        "user_agent", request.getHeader("User-Agent")
                    )
                );
            }
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                   Object handler, Exception exception) {
            // 清除 MDC 上下文
            StructuredLogging.clearMdcContext();
        }
    }

    /**
     * Supplier with Trace
     */
    @FunctionalInterface
    public interface SupplierWithTrace<T> {
        T get(Tracer tracer);
    }
}
