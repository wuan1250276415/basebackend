package com.basebackend.scheduler.monitoring.config;

import com.basebackend.observability.metrics.BusinessMetrics;
import com.basebackend.scheduler.monitoring.metrics.WorkflowMetrics;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 监控拦截器
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Component
public class MonitoringInterceptor implements HandlerInterceptor {

    private final WorkflowMetrics workflowMetrics;
    private final BusinessMetrics businessMetrics;
    private final ConcurrentMap<String, Timer.Sample> requestSamples = new ConcurrentHashMap<>();

    public MonitoringInterceptor(WorkflowMetrics workflowMetrics, BusinessMetrics businessMetrics) {
        this.workflowMetrics = workflowMetrics;
        this.businessMetrics = businessMetrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = request.getHeader("X-Request-ID");
        Timer.Sample sample = Timer.start();
        requestSamples.put(requestId, sample);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        String requestId = request.getHeader("X-Request-ID");
        Timer.Sample sample = requestSamples.remove(requestId);
        
        if (sample != null) {
            sample.stop(Timer.builder("http_request_duration")
                    .description("HTTP request duration")
                    .register(workflowMetrics.getMeterRegistry()));
        }
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        if (uri.contains("/workflow/start") && method.equals("POST")) {
            businessMetrics.recordUserLogout();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = request.getHeader("X-Request-ID");
        Timer.Sample sample = requestSamples.remove(requestId);
        
        if (sample != null && ex != null) {
            sample.stop(Timer.builder("http_request_duration")
                    .description("HTTP request duration")
                    .register(workflowMetrics.getMeterRegistry()));
        }
    }
}
