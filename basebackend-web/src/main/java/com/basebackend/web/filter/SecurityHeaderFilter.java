package com.basebackend.web.filter;

import com.basebackend.web.config.SecurityHeaderConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全头设置过滤器
 * 为响应添加各种安全相关的HTTP头
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
public class SecurityHeaderFilter implements Filter {

    private final SecurityHeaderConfig config;

    public SecurityHeaderFilter(SecurityHeaderConfig config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 如果请求已经是缓存包装的，则直接使用
        // 否则进行包装（以支持某些特定功能）
        ServletRequest wrappedRequest = request;
        if (!(request instanceof ContentCachingRequestWrapper)) {
            wrappedRequest = new ContentCachingRequestWrapper((HttpServletRequest) request);
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 添加安全头
        addSecurityHeaders(httpResponse);

        log.debug("Security headers added for request: {}", ((HttpServletRequest) request).getRequestURI());

        chain.doFilter(wrappedRequest, response);
    }

    /**
     * 添加所有安全头
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        // X-Frame-Options
        if (config.getFrameOptions() != null) {
            response.addHeader("X-Frame-Options", config.getFrameOptions());
        }

        // X-Content-Type-Options
        if (config.getContentTypeOptions() != null) {
            response.addHeader("X-Content-Type-Options", config.getContentTypeOptions());
        }

        // X-XSS-Protection
        if (config.getXssProtection() != null) {
            response.addHeader("X-XSS-Protection", config.getXssProtection());
        }

        // Strict-Transport-Security
        if (config.getStrictTransportSecurity() != null) {
            String hsts = config.getStrictTransportSecurity();
            if (config.isIncludeSubDomains() && !hsts.contains("includeSubDomains")) {
                hsts += "; includeSubDomains";
            }
            if (config.isPreload() && !hsts.contains("preload")) {
                hsts += "; preload";
            }
            response.addHeader("Strict-Transport-Security", hsts);
        }

        // Content-Security-Policy
        if (config.getContentSecurityPolicy() != null) {
            response.addHeader("Content-Security-Policy", config.getContentSecurityPolicy());
        }

        // Referrer-Policy
        if (config.getReferrerPolicy() != null) {
            response.addHeader("Referrer-Policy", config.getReferrerPolicy());
        }

        // Permissions-Policy
        if (config.getPermissionsPolicy() != null) {
            response.addHeader("Permissions-Policy", config.getPermissionsPolicy());
        }

        // 自定义安全头
        if (config.getCustomHeaders() != null && !config.getCustomHeaders().isEmpty()) {
            for (String header : config.getCustomHeaders()) {
                String[] parts = header.split(":");
                if (parts.length == 2) {
                    response.addHeader(parts[0].trim(), parts[1].trim());
                }
            }
        }

        // Cache-Control（防止缓存敏感信息）
        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Expires", "0");
    }
}
