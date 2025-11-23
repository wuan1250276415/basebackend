package com.basebackend.web.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控拦截器
 * 收集请求性能指标，包括响应时间、吞吐量、并发数等
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    // 请求计数统计
    private final Counter totalRequests;
    private final Counter successfulRequests;
    private final Counter failedRequests;

    // 响应时间统计
    private final Timer requestTimer;

    // 分布式统计（按路径分组）
    private final DistributionSummary pathStatistics;

    // 并发连接数统计
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong peakConnections = new AtomicLong(0);

    public PerformanceInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.totalRequests = Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .register(meterRegistry);

        this.successfulRequests = Counter.builder("http_requests_success_total")
                .description("Number of successful HTTP requests")
                .register(meterRegistry);

        this.failedRequests = Counter.builder("http_requests_failed_total")
                .description("Number of failed HTTP requests")
                .register(meterRegistry);

        this.requestTimer = Timer.builder("http_request_duration")
                .description("HTTP request duration")
                .register(meterRegistry);

        this.pathStatistics = DistributionSummary.builder("http_request_per_path")
                .description("HTTP request statistics by path")
                .register(meterRegistry);
    }

    private static final String START_TIME_KEY = "performance_interceptor_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录开始时间
        request.setAttribute(START_TIME_KEY, System.currentTimeMillis());

        // 增加并发连接数
        long current = activeConnections.incrementAndGet();
        long peak = peakConnections.get();
        if (current > peak) {
            peakConnections.compareAndSet(peak, current);
        }

        // 记录请求开始
        totalRequests.increment();

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 减少并发连接数
        activeConnections.decrementAndGet();

        // 计算响应时间
        long startTime = (Long) request.getAttribute(START_TIME_KEY);
        long duration = System.currentTimeMillis() - startTime;

        // 记录响应时间
        requestTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

        // 记录按路径的统计
        String path = request.getRequestURI();
        pathStatistics.record(duration);

        // 记录成功/失败状态
        if (ex != null || response.getStatus() >= 400) {
            failedRequests.increment();
        } else {
            successfulRequests.increment();
        }
    }

    /**
     * 获取当前并发连接数
     */
    public long getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * 获取峰值连接数
     */
    public long getPeakConnections() {
        return peakConnections.get();
    }
}
