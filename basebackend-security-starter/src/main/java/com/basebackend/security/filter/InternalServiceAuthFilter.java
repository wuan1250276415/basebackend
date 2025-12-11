package com.basebackend.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 内部服务调用验证过滤器
 * 用于验证来自其他微服务的内部调用
 *
 * @author Claude Code
 * @since 2025-12-09
 */
@Slf4j
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    private static final String X_INTERNAL_CALL = "X-Internal-Call";
    private static final String INTERNAL_SERVICE_PRINCIPAL = "INTERNAL_SERVICE";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 检查是否为内部服务调用
        String internalCall = request.getHeader(X_INTERNAL_CALL);

        if ("true".equalsIgnoreCase(internalCall)) {
            // 如果当前没有认证信息，则为内部服务调用创建一个认证主体
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("检测到内部服务调用，设置内部服务认证: path={}", request.getRequestURI());

                // 创建内部服务认证 token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        INTERNAL_SERVICE_PRINCIPAL,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
