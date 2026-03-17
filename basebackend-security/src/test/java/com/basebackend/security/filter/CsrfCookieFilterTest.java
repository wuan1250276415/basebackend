package com.basebackend.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * CsrfCookieFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CsrfCookieFilter CSRF Cookie 过滤器测试")
class CsrfCookieFilterTest {

    @InjectMocks
    private CsrfCookieFilter csrfCookieFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private CsrfToken csrfToken;

    @Test
    @DisplayName("无 CsrfToken 属性时直接放行，不写 Cookie")
    void shouldContinueChainWhenNoCsrfTokenAttribute() throws Exception {
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(null);

        csrfCookieFilter.doFilterInternal(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("CsrfToken 存在且无已有 Cookie 时写入新 Cookie")
    void shouldSetCookieWhenCsrfTokenPresentAndNoPreviousCookie() throws Exception {
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);
        when(csrfToken.getToken()).thenReturn("test-csrf-token");
        when(request.getCookies()).thenReturn(null);

        csrfCookieFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo("XSRF-TOKEN");
        assertThat(cookie.getValue()).isEqualTo("test-csrf-token");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isFalse();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("CsrfToken 与已有 Cookie 值相同时不重复写入")
    void shouldNotSetCookieWhenTokenMatchesExistingCookie() throws Exception {
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);
        when(csrfToken.getToken()).thenReturn("same-token");
        Cookie existingCookie = new Cookie("XSRF-TOKEN", "same-token");
        when(request.getCookies()).thenReturn(new Cookie[]{existingCookie});

        csrfCookieFilter.doFilterInternal(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("CsrfToken 与已有 Cookie 值不同时更新 Cookie")
    void shouldUpdateCookieWhenTokenDiffersFromExisting() throws Exception {
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);
        when(csrfToken.getToken()).thenReturn("new-token");
        Cookie existingCookie = new Cookie("XSRF-TOKEN", "old-token");
        when(request.getCookies()).thenReturn(new Cookie[]{existingCookie});

        csrfCookieFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getValue()).isEqualTo("new-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("CsrfToken 值为 null 时不写 Cookie")
    void shouldNotSetCookieWhenTokenValueIsNull() throws Exception {
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);
        when(csrfToken.getToken()).thenReturn(null);

        csrfCookieFilter.doFilterInternal(request, response, filterChain);

        verify(response, never()).addCookie(any(Cookie.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HTTPS 请求时 Cookie 应设置 Secure 标志")
    void shouldSetSecureFlagOnHttpsRequest() throws Exception {
        when(request.getAttribute(CsrfToken.class.getName())).thenReturn(csrfToken);
        when(csrfToken.getToken()).thenReturn("secure-token");
        when(request.getCookies()).thenReturn(null);
        when(request.isSecure()).thenReturn(true);

        csrfCookieFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        assertThat(cookieCaptor.getValue().getSecure()).isTrue();
    }
}
