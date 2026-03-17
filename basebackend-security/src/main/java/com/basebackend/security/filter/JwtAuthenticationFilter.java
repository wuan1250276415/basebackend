package com.basebackend.security.filter;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUserDetails;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.security.event.SecurityAuditEventPublisher;
import com.basebackend.security.event.SecurityEventType;
import com.basebackend.security.exception.TokenBlacklistException;
import com.basebackend.security.service.PermissionService;
import com.basebackend.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 可选注入：当 PermissionService 存在时，用于填充 GrantedAuthority，
     * 使 Spring Security 原生的 @PreAuthorize / @Secured 注解可用。
     */
    @Autowired(required = false)
    private PermissionService permissionService;

    @Autowired(required = false)
    private SecurityAuditEventPublisher auditEventPublisher;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            // 检查是否已有认证信息，如果有则短路，避免重复解析或覆盖
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("请求已有认证信息，直接放行");
                filterChain.doFilter(request, response);
                return;
            }

            // 获取Token
            String token = getTokenFromRequest(request);

            // 如果没有Token，直接继续过滤器链
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 先检查Token是否在黑名单中
            try {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("Token已在黑名单中，已拒绝访问: tokenHash={}", briefHash(token));
                    SecurityContextHolder.clearContext();
                    publishAuditEvent(request, SecurityEventType.TOKEN_BLACKLISTED, null, "Token在黑名单中");
                    handleAuthenticationError(response, "Token已失效");
                    return;
                }
            } catch (TokenBlacklistException e) {
                log.error("黑名单检查失败，拒绝访问: tokenHash={}", briefHash(token), e);
                SecurityContextHolder.clearContext();
                publishAuditEvent(request, SecurityEventType.AUTH_SERVICE_UNAVAILABLE, null, e.getMessage());
                handleAuthenticationError(response, "认证服务不可用");
                return;
            }

            // 验证Token
            if (jwtUtil.validateToken(token)) {
                // 从Token中获取完整用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                Long deptId = jwtUtil.getDeptIdFromToken(token);

                // 创建JwtUserDetails作为principal，包含完整用户信息
                JwtUserDetails userDetails = JwtUserDetails.builder()
                        .userId(userId)
                        .username(username)
                        .deptId(deptId)
                        .build();

                // 创建认证对象，尝试从 PermissionService 加载权限以支持 Spring Security 原生注解
                Collection<? extends GrantedAuthority> authorities = resolveAuthorities();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                publishAuditEvent(request, SecurityEventType.AUTHENTICATION_SUCCESS, username, null);
                log.debug("设置用户认证信息到SecurityContext: userId={}, username={}, deptId={}", userId, username, deptId);
            } else {
                publishAuditEvent(request, SecurityEventType.AUTHENTICATION_FAILURE, null, "Token验证失败");
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("认证失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            publishAuditEvent(request, SecurityEventType.AUTHENTICATION_FAILURE, null, e.getMessage());
            handleAuthenticationError(response, "认证失败");
        }
    }

    /**
     * 从 PermissionService 解析当前用户的 GrantedAuthority 列表。
     * 将角色映射为 ROLE_ 前缀，权限直接映射。
     * 若 PermissionService 不可用则返回空列表（退化到自定义 AOP 校验）。
     */
    private Collection<? extends GrantedAuthority> resolveAuthorities() {
        if (permissionService == null) {
            return Collections.emptyList();
        }
        try {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
            List<String> roles = permissionService.getCurrentUserRoles();
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            List<String> permissions = permissionService.getCurrentUserPermissions();
            if (permissions != null) {
                for (String perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority(perm));
                }
            }
            return authorities;
        } catch (Exception e) {
            log.debug("无法从 PermissionService 加载权限，退化到空 authorities: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 处理认证错误
     */
    private void handleAuthenticationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CommonConstants.CONTENT_TYPE_JSON);
        response.setCharacterEncoding(CommonConstants.UTF8);
        response.getWriter().write(JsonUtils.toJsonString(Result.error(401, message)));
    }

    private void publishAuditEvent(HttpServletRequest request, SecurityEventType type,
                                   String principal, String detail) {
        if (auditEventPublisher != null) {
            auditEventPublisher.publish(this, type, principal, request.getRemoteAddr(), detail);
        }
    }

    /**
     * 生成 Token 的短哈希用于日志追踪（前 8 位 SHA-256），避免日志中泄露原始 Token
     */
    private static String briefHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "<hash-unavailable>";
        }
    }
}
