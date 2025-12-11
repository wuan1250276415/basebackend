package com.basebackend.security.filter;

import com.alibaba.fastjson2.JSON;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUserDetails;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.security.exception.TokenBlacklistException;
import com.basebackend.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

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
                    log.warn("Token已在黑名单中，已拒绝访问: token={}", token);
                    // 清理可能存在的认证上下文
                    SecurityContextHolder.clearContext();
                    handleAuthenticationError(response, "Token已失效");
                    return;
                }
            } catch (TokenBlacklistException e) {
                log.error("黑名单检查失败，拒绝访问: token={}", token, e);
                // 清理可能存在的认证上下文
                SecurityContextHolder.clearContext();
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

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("设置用户认证信息到SecurityContext: userId={}, username={}, deptId={}", userId, username, deptId);
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("认证失败: {}", e.getMessage());
            // 清理认证上下文
            SecurityContextHolder.clearContext();
            handleAuthenticationError(response, "认证失败");
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
        response.getWriter().write(JSON.toJSONString(Result.error(401, message)));
    }
}
