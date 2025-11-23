package com.basebackend.database.observability;

import com.basebackend.observability.slo.annotation.SloMonitored;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Database 模块可观测性集成切面
 * <p>
 * 为数据库操作自动添加分布式追踪和 SLO 监控。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
@ConditionalOnProperty(prefix = "basebackend.database.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseObservabilityAspect {

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    public DatabaseObservabilityAspect(
            @Autowired(required = false) Tracer tracer,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 拦截 Mapper 方法，添加追踪
     */
    @Around("execution(* com.basebackend..*.mapper..*.*(..))")
    @SloMonitored(sloName = "database-operations", service = "database-service")
    public Object traceDatabaseOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (tracer == null) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Span span = tracer.spanBuilder("db." + className + "." + methodName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.operation", determineOperation(methodName))
                .setAttribute("db.mapper", className)
                .setAttribute("db.method", methodName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (meterRegistry != null) {
                meterRegistry.timer("database.operation.duration",
                        "mapper", className,
                        "method", methodName)
                        .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return result;

        } catch (Throwable ex) {
            span.recordException(ex);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.getMessage());

            if (meterRegistry != null) {
                meterRegistry.counter("database.errors",
                        "mapper", className,
                        "exception", ex.getClass().getSimpleName())
                        .increment();
            }

            throw ex;
        } finally {
            span.end();
        }
    }

    private String determineOperation(String methodName) {
        if (methodName.startsWith("select") || methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("list")) {
            return "SELECT";
        } else if (methodName.startsWith("insert") || methodName.startsWith("save") || methodName.startsWith("add")) {
            return "INSERT";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else {
            return "UNKNOWN";
        }
    }
}
