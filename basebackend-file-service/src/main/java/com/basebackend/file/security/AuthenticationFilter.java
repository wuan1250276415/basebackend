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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 全局认证过滤器
 *
 * 统一处理所有请求的身份认证和授权验证：
 * 1. 优先解析 JWT Token (Authorization: Bearer ...)
 * 2. 备用解析 X-User-ID (仅在信任的网关场景)
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

    /**
     * 需要跳过认证的路径（公开接口）
     * 例如：健康检查、swagger 文档、登录接口等
     */
    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/favicon.ico"
    );

    /**
     * 可以使用 X-User-ID 的信任来源
     * 生产环境建议配置具体的网关IP列表或域名白名单
     */
    private static final Pattern TRUSTED_GATEWAY_PATTERN = Pattern.compile(
            "^(localhost|127\\.0\\.0\\.1|10\\..+|192\\.168\\..+|172\\.(1[6-9]|2\\d|3[0-1])\\..+)$"
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

        // 2. 备用解析 X-User-ID（仅信任来源）
        String userId = request.getHeader("X-User-ID");
        if (StringUtils.hasText(userId)) {
            return parseTrustedUserId(userId, request);
        }

        // 3. 两者都无，返回 null
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

    /**
     * 解析 JWT Token（安全增强版）
     *
     * <strong>注意</strong>：当前为简化实现，生产环境需使用标准 JWT 库验证。
     * 当前实现已添加基本安全检查：
     * 1. token 格式验证
     * 2. 组件分割和基本验证
     * 3. 日志记录和错误处理
     *
     * TODO: 生产环境需添加：
     * - 使用 jjwt 或 jose4j 库
     * - 验证 HMAC 签名（secret）
     * - 检查 exp（过期时间）< NOW
     * - 验证 iss（颁发者）和 aud（受众）
     * - 验证 nbf（生效时间）
     * - 检查 jti（唯一ID）防重放
     */
    private UserContext parseJwtToken(String token, HttpServletRequest request) {
        try {
            // 验证 token 不为空
            if (!StringUtils.hasText(token) || token.length() < 10) {
                log.warn("JWT Token 格式无效: token={}", token);
                return null;
            }

            // 简化实现：格式为 userId:tenantId:role1,role2
            // 生产环境必须替换为真正的 JWT 验证！
            String[] parts = token.split(":");
            if (parts.length < 1 || !StringUtils.hasText(parts[0])) {
                log.warn("JWT Token 格式无效: token={}", token);
                return null;
            }

            // 基本安全检查：token 不能包含特殊字符
            if (token.contains(" ") || token.contains("\n") || token.contains("\r")) {
                log.warn("JWT Token 包含非法字符");
                return null;
            }

            UserContext context = new UserContext();
            context.setUserId(parts[0].trim());
            context.setTenantId(parts.length > 1 ? parts[1].trim() : null);
            context.setAuthType(UserContext.AuthType.JWT);
            context.setTokenId(token);

            // 解析角色
            if (parts.length > 2 && StringUtils.hasText(parts[2])) {
                String rolesStr = parts[2].trim();
                if (rolesStr.length() > 0) {
                    String[] roles = rolesStr.split(",");
                    context.setRoles(Arrays.asList(roles));
                }
            }

            // 设置客户端信息
            setClientInfo(context, request);

            // 设置认证时间
            context.setAuthenticatedAt(LocalDateTime.now());

            log.debug("JWT 认证成功: userId={}, tenantId={}",
                    context.getUserId(), context.getTenantId());

            return context;

        } catch (Exception e) {
            log.error("解析 JWT Token 失败", e);
            return null;
        }
    }

    /**
     * 解析信任来源的用户ID（X-User-ID）
     */
    private UserContext parseTrustedUserId(String userId, HttpServletRequest request) {
        // 检查来源是否可信
        String clientIp = getClientIp(request);
        if (!isTrustedSource(clientIp)) {
            log.warn("不受信任的 X-User-ID 来源: ip={}, userId={}", clientIp, userId);
            return null;
        }

        UserContext context = new UserContext();
        context.setUserId(userId.trim());
        context.setAuthType(UserContext.AuthType.GATEWAY_TRUSTED);

        // 设置客户端信息
        setClientInfo(context, request);

        // 设置认证时间
        context.setAuthenticatedAt(LocalDateTime.now());

        log.debug("信任来源认证成功: userId={}, ip={}",
                context.getUserId(), clientIp);

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
     * 检查来源是否可信（安全增强版）
     *
     * <strong>安全策略</strong>：
     * 1. 默认拒绝所有 X-User-ID 请求（安全优先）
     * 2. 仅当明确配置可信网关 IP 时才允许
     * 3. 不信任任何公网请求
     * 4. 不信任 localhost（除非明确配置）
     *
     * <strong>生产环境配置</strong>：
     * -Dtrusted.gateway.ips=192.168.1.100,10.0.0.50
     * -Dtrust.localhost=true
     */
    private boolean isTrustedSource(String clientIp) {
        // 默认拒绝（安全优先）
        boolean trusted = false;

        // 检查是否配置了可信网关 IP 列表
        String trustedIps = System.getProperty("trusted.gateway.ips", "");
        if (StringUtils.hasText(trustedIps)) {
            String[] ips = trustedIps.split(",");
            for (String ip : ips) {
                if (ip.trim().equals(clientIp)) {
                    trusted = true;
                    break;
                }
            }
        }

        // 仅在明确信任时才允许 localhost
        if (!trusted && ("localhost".equals(clientIp) || "127.0.0.1".equals(clientIp))) {
            String trustLocalhost = System.getProperty("trust.localhost", "false");
            trusted = "true".equalsIgnoreCase(trustLocalhost);
        }

        // 记录所有 X-User-ID 尝试（审计）
        if (!trusted) {
            log.warn("拒绝不受信任的 X-User-ID 来源: ip={}", clientIp);
        }

        return trusted;
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
