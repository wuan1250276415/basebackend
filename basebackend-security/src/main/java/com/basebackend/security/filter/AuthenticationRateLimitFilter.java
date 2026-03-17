package com.basebackend.security.filter;

import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.security.event.SecurityAuditEventPublisher;
import com.basebackend.security.event.SecurityEventType;
import com.basebackend.security.service.AuthenticationRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 认证接口速率限制过滤器
 * <p>
 * 仅对认证相关路径（登录/注册等）生效，基于客户端 IP 进行限流。
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/user/auth/login",
            "/api/user/auth/register"
    );

    private final AuthenticationRateLimiter rateLimiter;
    private SecurityAuditEventPublisher auditEventPublisher;

    public void setAuditEventPublisher(SecurityAuditEventPublisher auditEventPublisher) {
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!isRateLimitedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        if (!rateLimiter.tryAcquire(clientIp)) {
            long remaining = rateLimiter.getRemainingBlockSeconds(clientIp);
            log.warn("认证请求被速率限制: ip={}, path={}, remainingBlockSeconds={}", clientIp, path, remaining);
            if (auditEventPublisher != null) {
                auditEventPublisher.publish(this, SecurityEventType.RATE_LIMIT_EXCEEDED,
                        null, clientIp, "速率限制触发: path=" + path);
            }
            respondTooManyRequests(response, remaining);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return RATE_LIMITED_PATHS.contains(path);
    }

    /**
     * 解析客户端真实 IP，依次检查代理头。
     * 注意：生产环境中应由 Gateway/LB 层设置 X-Forwarded-For 并剥离客户端伪造头。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 取第一个 IP（最左侧为原始客户端 IP）
            int commaIdx = ip.indexOf(',');
            return commaIdx > 0 ? ip.substring(0, commaIdx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType(CommonConstants.CONTENT_TYPE_JSON);
        response.setCharacterEncoding(CommonConstants.UTF8);
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }
        response.getWriter().write(JsonUtils.toJsonString(
                Result.error(429, "请求过于频繁，请稍后再试")));
    }
}
