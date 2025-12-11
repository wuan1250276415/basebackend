package com.basebackend.security.filter;

import com.alibaba.fastjson2.JSON;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.security.exception.TokenBlacklistException;
import com.basebackend.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JWT认证过滤器测试
 * 测试JWT令牌认证流程
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter JWT认证过滤器测试")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("有效Token认证成功")
    void shouldAuthenticateWithValidToken() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "valid-jwt-token";
        Long userId = 123L;
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getSubjectFromToken(token)).thenReturn(username);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, times(1)).validateToken(token);
        verify(jwtUtil, times(1)).getUserIdFromToken(token);
        verify(jwtUtil, times(1)).getSubjectFromToken(token);
        verify(tokenBlacklistService, times(1)).isBlacklisted(token);
        verify(filterChain, times(1)).doFilter(request, response);

        // 验证SecurityContext中的认证信息
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);
    }

    @Test
    @DisplayName("无Token时继续过滤器链")
    void shouldContinueFilterChainWithoutToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, never()).validateToken(anyString());
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);

        // SecurityContext应该为空
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("空Token时继续过滤器链")
    void shouldContinueFilterChainWithEmptyToken() throws Exception {
        // Given
        SecurityContextHolder.clearContext();
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("无效Token时继续过滤器链")
    void shouldContinueFilterChainWithInvalidToken() throws Exception {
        // Given
        String token = "invalid-token";
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenBlacklistService, times(1)).isBlacklisted(token);
        verify(jwtUtil, times(1)).validateToken(token);
        verify(filterChain, times(1)).doFilter(request, response);

        // SecurityContext应该为空（认证失败）
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Token在黑名单中时应拒绝访问")
    void shouldRejectTokenInBlacklist() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "blacklisted-token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);
        when(response.getWriter()).thenReturn(writer);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenBlacklistService, times(1)).isBlacklisted(token);
        verify(jwtUtil, never()).validateToken(token);
        verify(filterChain, never()).doFilter(request, response);

        // 验证错误响应
        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("Token已失效");
    }

    @Test
    @DisplayName("认证失败时应处理错误")
    void shouldHandleAuthenticationError() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "valid-token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("Token parsing error"));
        when(response.getWriter()).thenReturn(writer);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, times(1)).validateToken(token);
        verify(filterChain, never()).doFilter(request, response);

        // 验证错误响应
        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("认证失败");
    }

    @Test
    @DisplayName("使用userId作为principal")
    void shouldUseUserIdAsPrincipal() throws Exception {
        // Given
        String token = "valid-token";
        Long userId = 456L;
        String username = "anotheruser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getSubjectFromToken(token)).thenReturn(username);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);
    }

    @Test
    @DisplayName("userId为null时使用username作为principal")
    void shouldUseUsernameAsPrincipalWhenUserIdIsNull() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "valid-token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(null);
        when(jwtUtil.getSubjectFromToken(token)).thenReturn(username);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(username);
    }

    @Test
    @DisplayName("从Header中正确提取Token")
    void shouldExtractTokenFromAuthorizationHeader() throws Exception {
        // Given
        String token = "extracted-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(789L);
        when(jwtUtil.getSubjectFromToken(token)).thenReturn("user789");
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, times(1)).validateToken("extracted-token");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("错误格式的Authorization Header")
    void shouldHandleMalformedAuthorizationHeader() throws Exception {
        // Given
        SecurityContextHolder.clearContext();
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - 应该忽略错误格式的header
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("认证对象包含正确的详情")
    void shouldSetAuthenticationWithCorrectDetails() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "valid-token";
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        lenient().when(jwtUtil.validateToken(token)).thenReturn(true);
        lenient().when(jwtUtil.getUserIdFromToken(token)).thenReturn(100L);
        lenient().when(jwtUtil.getSubjectFromToken(token)).thenReturn("user100");
        lenient().when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getDetails()).isNotNull();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("多次请求认证信息累加")
    void shouldNotAccumulateAuthenticationOnMultipleRequests() throws Exception {
        // Given
        String token1 = "valid-token-1";
        String token2 = "valid-token-2";

        // 第一次请求
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer " + token1);
        lenient().when(jwtUtil.validateToken(token1)).thenReturn(true);
        lenient().when(jwtUtil.getUserIdFromToken(token1)).thenReturn(200L);
        lenient().when(jwtUtil.getSubjectFromToken(token1)).thenReturn("user200");
        lenient().when(tokenBlacklistService.isBlacklisted(token1)).thenReturn(false);

        // 第二次请求
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer " + token2);
        lenient().when(jwtUtil.validateToken(token2)).thenReturn(true);
        lenient().when(jwtUtil.getUserIdFromToken(token2)).thenReturn(300L);
        lenient().when(jwtUtil.getSubjectFromToken(token2)).thenReturn("user300");
        lenient().when(tokenBlacklistService.isBlacklisted(token2)).thenReturn(false);

        // When - 第一次请求
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        var auth1 = SecurityContextHolder.getContext().getAuthentication();

        // 重置SecurityContext（模拟新请求）
        SecurityContextHolder.clearContext();

        // 第二次请求
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        var auth2 = SecurityContextHolder.getContext().getAuthentication();

        // Then
        assertThat(auth2).isNotNull();
        assertThat(auth2.getPrincipal()).isEqualTo(300L);
    }

    @Test
    @DisplayName("黑名单检查失败时返回错误响应")
    void shouldContinueWhenBlacklistCheckFails() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "valid-token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenThrow(new RuntimeException("Redis error"));
        when(response.getWriter()).thenReturn(writer);

        // When - 不应抛出异常，会返回认证失败响应
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - 应该返回错误响应而不是继续过滤链
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 验证错误响应
        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("认证失败");
    }

    @Test
    @DisplayName("JWT工具异常时返回错误响应")
    void shouldContinueFilterChainWhenJwtUtilThrowsException() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "valid-token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("JWT error"));
        when(response.getWriter()).thenReturn(writer);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - 应该返回错误响应而不是继续过滤链
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 验证错误响应
        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("认证失败");
    }

    @Test
    @DisplayName("响应状态码设置正确")
    void shouldSetCorrectResponseStatus() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // 清理之前的认证信息
        String token = "blacklisted-token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);
        when(response.getWriter()).thenReturn(writer);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        // 验证响应状态码（401 Unauthorized）
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).isNotEmpty();
    }

    @Test
    @DisplayName("黑名单服务异常时返回错误响应并清理上下文")
    void shouldHandleBlacklistServiceException() throws Exception {
        // Given
        String token = "valid-token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenThrow(new TokenBlacklistException("Redis error"));
        when(response.getWriter()).thenReturn(writer);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 验证SecurityContext被清理
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 验证错误响应
        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("认证服务不可用");
    }

    @Test
    @DisplayName("已有认证时跳过处理")
    void shouldSkipProcessingWhenAlreadyAuthenticated() throws Exception {
        // Given
        // 设置已有的认证信息
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("existing-user", null, Collections.emptyList())
        );
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
