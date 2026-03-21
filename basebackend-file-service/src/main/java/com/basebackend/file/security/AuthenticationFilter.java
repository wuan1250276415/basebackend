package com.basebackend.file.security;

import com.basebackend.common.model.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 全局认证过滤器
 *
 * 统一处理所有请求的身份认证和授权验证：
 * 1. 优先解析 JWT Token (Authorization: Bearer ...)
 * 2. 拒绝直接信任客户端传入的 X-User-ID
 * 3. 提取客户端信息 (IP、User-Agent) 用于审计和限流
 * 4. 将用户上下文存储到 ThreadLocal 中
 * 5. 在 finally 块中清理 ThreadLocal 防止内存泄漏
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final JwtTokenService jwtTokenService;

    /**
     * 需要跳过认证的路径（公开接口）
     * 例如：健康检查、swagger 文档、登录接口等
     */
    private static final List<String> SKIP_PATHS = List.of(
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 检查是否需要跳过认证
            if (shouldSkipAuthentication(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 解析用户身份
            UserContext context = parseUserContext(request);
            if (context == null) {
                writeUnauthorizedResponse(response, "身份认证失败：未提供有效的认证信息");
                return;
            }

            // 3. 存储用户上下文
            UserContextHolder.setContext(context);

            // 4. 继续处理请求
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("认证过滤器异常", e);
            writeUnauthorizedResponse(response, "身份认证失败：服务器错误");
        } finally {
            // 5. 清理 ThreadLocal（防止内存泄漏）
            UserContextHolder.clear();
        }
    }

    /**
     * 检查是否应该跳过认证
     *
     * 跳过条件：
     * 1. CORS 预检请求（OPTIONS）
     * 2. 健康检查、文档等公开路径
     */
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        // 1. CORS 预检请求直接跳过
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 跳过配置的公开路径
        String path = request.getRequestURI();
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 解析用户上下文
     *
     * @param request HTTP 请求
     * @return 用户上下文，如果认证失败返回 null
     */
    private UserContext parseUserContext(HttpServletRequest request) {
        // 1. 尝试解析 JWT Token
        String jwtToken = extractJwtToken(request);
        if (StringUtils.hasText(jwtToken)) {
            return parseJwtToken(jwtToken, request);
        }

        String forwardedUserId = request.getHeader("X-User-ID");
        if (StringUtils.hasText(forwardedUserId)) {
            log.warn("拒绝直接信任 X-User-ID 头: path={}, remoteAddr={}",
                    request.getRequestURI(), request.getRemoteAddr());
        }

        // 2. 无有效 JWT，返回 null
        log.debug("未找到有效的认证信息: path={}",
                request.getRequestURI());
        return null;
    }

    /**
     * 提取 JWT Token
     */
    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private UserContext parseJwtToken(String token, HttpServletRequest request) {
        UserContext context = jwtTokenService.parseAndValidateToken(token);
        if (context == null) {
            log.warn("JWT Token 验证失败: path={}, remoteAddr={}", request.getRequestURI(), request.getRemoteAddr());
            return null;
        }

        setClientInfo(context, request);
        if (context.getAuthenticatedAt() == null) {
            context.setAuthenticatedAt(LocalDateTime.now());
        }
        log.debug("JWT 认证成功: userId={}, tenantId={}", context.getUserId(), context.getTenantId());
        return context;
    }

    /**
     * 设置客户端信息
     */
    private void setClientInfo(UserContext context, HttpServletRequest request) {
        context.setClientIp(getClientIp(request));
        context.setUserAgent(request.getHeader("User-Agent"));
    }

    /**
     * 获取客户端IP（安全增强版）
     *
     * <strong>安全策略</strong>：
     * 1. 默认使用 remoteAddr（最可信）
     * 2. 仅在信任的代理环境下才使用 X-Forwarded-For/X-Real-IP
     * 3. 防止客户端伪造 X-Forwarded-For 头
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先使用 remoteAddr（最可信，无法伪造）
        String remoteAddr = request.getRemoteAddr();

        // 仅当明确信任代理时才使用 X-Forwarded-For/X-Real-IP
        // 生产环境应配置具体的可信代理IP列表
        String trustedProxy = System.getProperty("trusted.proxy.ip", "");
        if (StringUtils.hasText(trustedProxy) && remoteAddr.equals(trustedProxy)) {
            // X-Forwarded-For 仅在信任代理场景下使用
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor)) {
                // X-Forwarded-For 可能包含多个IP，取第一个
                String ip = xForwardedFor.split(",")[0].trim();
                // 验证 IP 格式
                if (isValidIp(ip)) {
                    return ip;
                }
            }

            // 备用 X-Real-IP
            String xRealIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(xRealIp) && isValidIp(xRealIp)) {
                return xRealIp.trim();
            }
        }

        // 默认返回 remoteAddr
        return remoteAddr;
    }

    /**
     * 验证 IP 地址格式
     */
    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        // 简单的 IP 格式验证（IPv4）
        return ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");
    }

    /**
     * 写入未授权响应
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        Result<Void> result = Result.error(message);
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);

        log.warn("认证失败: {}", message);
    }
}
