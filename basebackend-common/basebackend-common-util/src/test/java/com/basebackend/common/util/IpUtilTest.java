package com.basebackend.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IpUtil 单元测试
 */
class IpUtilTest {

    @Nested
    @DisplayName("getIpAddress")
    class GetIpAddressTests {

        @Test
        @DisplayName("request 为 null 时返回本地地址")
        void shouldReturnLocalhostWhenRequestIsNull() {
            assertThat(IpUtil.getIpAddress(null)).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("可信代理来源时优先解析 X-Forwarded-For")
        void shouldUseForwardedIpWhenRemoteIsTrustedProxy() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("10.0.0.5");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.5");

            assertThat(IpUtil.getIpAddress(request)).isEqualTo("203.0.113.10");
        }

        @Test
        @DisplayName("非可信代理来源时忽略转发头")
        void shouldIgnoreForwardedHeadersWhenRemoteIsUntrusted() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("8.8.8.8");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

            assertThat(IpUtil.getIpAddress(request)).isEqualTo("8.8.8.8");
        }

        @Test
        @DisplayName("可信代理但转发头无效时回退 remoteAddr")
        void shouldFallbackToRemoteAddrWhenForwardedHeaderInvalid() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("198.51.100.20");
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getHeader("Proxy-Client-IP")).thenReturn("");

            assertThat(IpUtil.getIpAddress(request)).isEqualTo("198.51.100.20");
        }

        @Test
        @DisplayName("可信代理时支持备用转发头")
        void shouldUseFallbackForwardedHeaderWhenPrimaryMissing() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getHeader("Proxy-Client-IP")).thenReturn("198.51.100.8");

            assertThat(IpUtil.getIpAddress(request)).isEqualTo("198.51.100.8");
        }

        @Test
        @DisplayName("IPv6 本地地址应规范化为 IPv4")
        void shouldNormalizeIpv6LoopbackToIpv4() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRemoteAddr()).thenReturn("::1");

            assertThat(IpUtil.getIpAddress(request)).isEqualTo("127.0.0.1");
        }
    }
}
