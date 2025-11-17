package com.basebackend.security.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 安全审计拦截器
 * 自动拦截HTTP请求并记录安全事件
 */
@Slf4j
@Component
public class SecurityAuditInterceptor implements HandlerInterceptor {

    @Autowired
    private SecurityAuditService auditService;

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        request.setAttribute(START_TIME, System.currentTimeMillis());

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                          ModelAndView modelAndView) throws Exception {
        // 请求处理完成后的逻辑
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 记录请求完成信息
        String username = getCurrentUsername(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        int statusCode = response.getStatus();

        // 计算响应时间
        long startTime = (long) request.getAttribute(START_TIME);
        long responseTime = System.currentTimeMillis() - startTime;

        // 记录API调用事件
        auditService.logApiCall(username, endpoint, method, ipAddress, userAgent, statusCode, responseTime);

        // 如果有异常，记录异常事件
        if (ex != null) {
            log.error("请求异常: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 从请求中获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        // 优先从OAuth2.0 JWT令牌中获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                // 这里可以解析JWT获取用户名
                // 实际实现中需要调用JWT验证服务
            } catch (Exception e) {
                log.debug("解析JWT令牌失败", e);
            }
        }

        // 从会话中获取
        if (request.getSession() != null) {
            Object username = request.getSession().getAttribute("username");
            if (username != null) {
                return username.toString();
            }
        }

        // 从请求头中获取
        String xUsername = request.getHeader("X-Username");
        if (xUsername != null) {
            return xUsername;
        }

        // 默认返回anonymous
        return "anonymous";
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
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
