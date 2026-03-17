package com.basebackend.security.filter;

import com.basebackend.security.config.SecurityBaselineProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * OriginValidationFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OriginValidationFilter Origin 校验过滤器测试")
class OriginValidationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecurityBaselineProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityBaselineProperties();
    }

    // ---- 安全方法直接放行 ----

    @Test
    @DisplayName("GET 请求直接放行，不校验 Origin")
    void shouldPassThroughForGetRequests() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HEAD 请求直接放行")
    void shouldPassThroughForHeadRequests() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("HEAD");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("OPTIONS 请求直接放行")
    void shouldPassThroughForOptionsRequests() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ---- 无 Cookie 时不校验（非浏览器请求） ----

    @Test
    @DisplayName("POST 请求无 Cookie 时直接放行")
    void shouldPassThroughPostWithoutCookie() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ---- allowedOrigins 为空时跳过校验 ----

    @Test
    @DisplayName("allowedOrigins 为空时跳过 Origin 校验直接放行")
    void shouldPassThroughWhenAllowedOriginsEmpty() throws Exception {
        properties.setAllowedOrigins(Collections.emptyList());
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ---- Origin 匹配 ----

    @Test
    @DisplayName("Origin 匹配允许列表时放行")
    void shouldPassWhenOriginMatches() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn("https://example.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Origin 大小写不敏感匹配")
    void shouldMatchOriginCaseInsensitively() throws Exception {
        properties.setAllowedOrigins(List.of("https://Example.COM"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn("https://example.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("带端口号的 Origin 匹配")
    void shouldMatchOriginWithPort() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com:8443"));
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn("https://example.com:8443");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ---- Origin 不匹配时拒绝 ----

    @Test
    @DisplayName("Origin 不在允许列表中时返回 403")
    void shouldReject403WhenOriginNotAllowed() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        properties.setEnforceReferer(false);
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn("https://evil.com");
        when(request.getRequestURI()).thenReturn("/api/test");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(sw.toString()).contains("403");
    }

    // ---- Referer 回退 ----

    @Test
    @DisplayName("Origin 缺失但 Referer 匹配时放行（enforceReferer=true）")
    void shouldFallbackToRefererWhenOriginMissing() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        properties.setEnforceReferer(true);
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn(null);
        when(request.getHeader("Referer")).thenReturn("https://example.com/page");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Origin 和 Referer 都不匹配时返回 403")
    void shouldRejectWhenBothOriginAndRefererInvalid() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        properties.setEnforceReferer(true);
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn("https://evil.com");
        when(request.getHeader("Referer")).thenReturn("https://evil.com/page");
        when(request.getRequestURI()).thenReturn("/api/test");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("enforceReferer=false 时不检查 Referer")
    void shouldNotCheckRefererWhenEnforceRefererDisabled() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        properties.setEnforceReferer(false);
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
    }

    // ---- 格式异常 ----

    @Test
    @DisplayName("Origin 格式无效时拒绝请求")
    void shouldRejectMalformedOrigin() throws Exception {
        properties.setAllowedOrigins(List.of("https://example.com"));
        properties.setEnforceReferer(false);
        OriginValidationFilter filter = new OriginValidationFilter(properties);

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Cookie")).thenReturn("session=abc");
        when(request.getHeader("Origin")).thenReturn("not-a-valid-uri://[broken");
        when(request.getRequestURI()).thenReturn("/api/test");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
    }
}
