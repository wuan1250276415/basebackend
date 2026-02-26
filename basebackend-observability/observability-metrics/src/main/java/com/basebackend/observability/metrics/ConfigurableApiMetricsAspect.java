package com.basebackend.observability.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * 可配置的API指标采集切面
 * <p>
 * 改进点：
 * - 可通过配置启用/禁用
 * - 支持URI排除列表
 * - 可配置的慢接口阈值
 * - 更细粒度的指标控制
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "observability.metrics.api", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ConfigurableApiMetricsAspect {

    private final CustomMetrics customMetrics;

    /** 慢接口阈值（毫秒） */
    @Value("${observability.metrics.api.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    /** 是否记录响应时间 */
    @Value("${observability.metrics.api.record-response-time:true}")
    private boolean recordResponseTime;

    /** 是否记录错误详情 */
    @Value("${observability.metrics.api.record-error-details:true}")
    private boolean recordErrorDetails;

    /** 排除的URI模式（逗号分隔） */
    @Value("${observability.metrics.api.excluded-uris:/actuator/**,/health,/ready,/swagger-ui/**}")
    private Set<String> excludedUris;

    /**
     * 切入点：所有 Controller 层的方法
     */
    @Pointcut("execution(public * com.basebackend..controller..*.*(..)) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public void apiMethods() {
    }

    /**
     * 环绕通知：采集 API 指标
     */
    @Around("apiMethods()")
    public Object collectMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";

        // 检查是否需要排除
        if (shouldExclude(uri)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();

        // 增加活跃请求数
        customMetrics.incrementActiveRequests();

        Object result = null;
        Throwable exception = null;
        String status = "success";

        try {
            // 执行方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            status = "error";
            throw e;
        } finally {
            // 减少活跃请求数
            customMetrics.decrementActiveRequests();

            // 计算响应时间
            long duration = System.currentTimeMillis() - startTime;

            // 记录 API 调用
            customMetrics.recordApiCall(method, uri, status);
            customMetrics.apiRequestTime(method, uri, status);

            // 记录响应时间
            if (recordResponseTime) {
                customMetrics.recordApiResponseTime(method, uri, duration);
            }

            // 如果有异常，记录错误
            if (exception != null && recordErrorDetails) {
                String errorType = exception.getClass().getSimpleName();
                customMetrics.recordApiError(method, uri, errorType);
            }

            // 慢接口告警
            if (duration > slowThresholdMs) {
                log.warn("Slow API detected: {} {} - {}ms (threshold: {}ms)",
                        method, uri, duration, slowThresholdMs);
            }
        }
    }

    /**
     * 检查URI是否应该被排除
     */
    private boolean shouldExclude(String uri) {
        if (uri == null || excludedUris == null) {
            return false;
        }

        for (String pattern : excludedUris) {
            if (matchesPattern(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单的通配符匹配
     */
    private boolean matchesPattern(String uri, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return uri.startsWith(prefix);
        }
        return uri.equals(pattern);
    }
}
