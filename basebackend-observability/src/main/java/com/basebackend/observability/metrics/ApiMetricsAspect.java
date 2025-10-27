package com.basebackend.observability.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 指标采集切面
 * 自动采集 API 调用次数、响应时间、错误率等指标
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiMetricsAspect {

    private final CustomMetrics customMetrics;

    /**
     * 切入点：所有 Controller 层的方法
     */
    @Pointcut("execution(public * com.basebackend..controller..*.*(..))")
    public void apiMethods() {
    }

    /**
     * 环绕通知：采集 API 指标
     */
    @Around("apiMethods()")
    public Object collectMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";

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
            customMetrics.recordApiResponseTime(method, uri, duration);

            // 如果有异常，记录错误
            if (exception != null) {
                String errorType = exception.getClass().getSimpleName();
                customMetrics.recordApiError(method, uri, errorType);
            }

            // 慢接口告警（响应时间超过 1 秒）
            if (duration > 1000) {
                log.warn("Slow API detected: {} {} - {}ms",
                        method, uri, duration);
            }
        }
    }
}
