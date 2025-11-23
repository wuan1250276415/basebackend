package com.basebackend.cache.observability;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cache 模块可观测性集成切面
 * <p>
 * 为缓存操作自动添加分布式追踪和 SLO 监控。
 * </p>
 * <p>
 * <b>功能：</b>
 * <ul>
 *     <li>自动追踪缓存操作（get/put/evict）</li>
 *     <li>记录缓存命中率、延迟等指标</li>
 *     <li>集成 SLO 监控（缓存可用性）</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
@ConditionalOnProperty(prefix = "basebackend.cache.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheObservabilityAspect {

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    public CacheObservabilityAspect(
            @Autowired(required = false) Tracer tracer,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 拦截所有 CacheService 的方法，添加追踪
     */
    @Around("execution(* com.basebackend.cache.service.CacheService.*(..)) || " +
            "execution(* com.basebackend.cache.service.CacheServiceImpl.*(..))")
    @SloMonitored(sloName = "cache-operations", service = "cache-service")
    public Object traceCacheOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (tracer == null) {
            // OpenTelemetry 未启用，直接执行
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();
        String operation = determineCacheOperation(methodName);

        Span span = tracer.spanBuilder("cache." + operation)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("cache.operation", operation)
                .setAttribute("cache.method", methodName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // 提取缓存 key（如果是第一个参数）
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof String) {
                span.setAttribute("cache.key", (String) args[0]);
            }

            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // 记录延迟指标
            if (meterRegistry != null) {
                meterRegistry.timer("cache.operation.duration",
                        "operation", operation,
                        "method", methodName)
                        .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            // 记录缓存命中/未命中
            if ("get".equals(operation)) {
                boolean hit = result != null;
                span.setAttribute("cache.hit", hit);

                if (meterRegistry != null) {
                    meterRegistry.counter("cache.requests",
                            "operation", operation,
                            "result", hit ? "hit" : "miss")
                            .increment();
                }
            }

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return result;

        } catch (Throwable ex) {
            span.recordException(ex);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.getMessage());

            if (meterRegistry != null) {
                meterRegistry.counter("cache.errors",
                        "operation", operation,
                        "exception", ex.getClass().getSimpleName())
                        .increment();
            }

            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * 拦截分布式锁操作
     */
    @Around("execution(* com.basebackend.cache.lock.DistributedLockService.*(..))")
    public Object traceDistributedLock(ProceedingJoinPoint joinPoint) throws Throwable {
        if (tracer == null) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();

        Span span = tracer.spanBuilder("cache.lock." + methodName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("lock.method", methodName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // 提取锁 key
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof String) {
                span.setAttribute("lock.key", (String) args[0]);
            }

            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // 记录锁获取时间
            if (meterRegistry != null) {
                meterRegistry.timer("cache.lock.duration",
                        "method", methodName)
                        .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            // 记录锁获取成功/失败
            if (methodName.contains("tryLock") && result instanceof Boolean) {
                boolean acquired = (Boolean) result;
                span.setAttribute("lock.acquired", acquired);

                if (meterRegistry != null) {
                    meterRegistry.counter("cache.lock.attempts",
                            "result", acquired ? "success" : "failure")
                            .increment();
                }
            }

            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return result;

        } catch (Throwable ex) {
            span.recordException(ex);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * 根据方法名判断缓存操作类型
     */
    private String determineCacheOperation(String methodName) {
        if (methodName.startsWith("get") || methodName.contains("find")) {
            return "get";
        } else if (methodName.startsWith("put") || methodName.startsWith("set") || methodName.contains("save")) {
            return "put";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove") || methodName.contains("evict")) {
            return "delete";
        } else if (methodName.contains("exist") || methodName.contains("has")) {
            return "exists";
        } else {
            return "unknown";
        }
    }
}
