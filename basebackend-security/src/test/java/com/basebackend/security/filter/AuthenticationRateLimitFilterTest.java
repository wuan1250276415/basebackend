package com.basebackend.security.filter;

import com.basebackend.security.service.AuthenticationRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AuthenticationRateLimitFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationRateLimitFilter 认证速率限制过滤器测试")
class AuthenticationRateLimitFilterTest {

    @Mock
    private AuthenticationRateLimiter rateLimiter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("非认证路径直接放行")
    void shouldPassNonAuthPaths() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(rateLimiter);
        when(request.getRequestURI()).thenReturn("/api/users/list");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiter, never()).tryAcquire(anyString());
    }

    @Test
    @DisplayName("登录路径未超限应放行")
    void shouldPassLoginPathWithinLimit() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(rateLimiter);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("198.51.100.100");
        when(rateLimiter.tryAcquire("198.51.100.100")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("登录路径超限应返回 429")
    void shouldReturn429WhenRateLimited() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(rateLimiter);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("198.51.100.100");
        when(rateLimiter.tryAcquire("198.51.100.100")).thenReturn(false);
        when(rateLimiter.getRemainingBlockSeconds("198.51.100.100")).thenReturn(300L);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "300");
        verify(filterChain, never()).doFilter(request, response);
        assertThat(sw.toString()).contains("请求过于频繁");
    }

    @Test
    @DisplayName("X-Forwarded-For 应被解析为客户端 IP")
    void shouldUseXForwardedForAsClientIp() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(rateLimiter);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 172.16.0.1");
        when(rateLimiter.tryAcquire("10.0.0.1")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).tryAcquire("10.0.0.1");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("/api/user/auth/login 路径也应受速率限制")
    void shouldRateLimitUserAuthLogin() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(rateLimiter);
        when(request.getRequestURI()).thenReturn("/api/user/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(rateLimiter.tryAcquire("10.0.0.2")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).tryAcquire("10.0.0.2");
    }
}
