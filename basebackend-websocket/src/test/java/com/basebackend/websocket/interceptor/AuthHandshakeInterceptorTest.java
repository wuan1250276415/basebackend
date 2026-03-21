package com.basebackend.websocket.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthHandshakeInterceptor 测试")
class AuthHandshakeInterceptorTest {

    private final AuthHandshakeInterceptor interceptor = new AuthHandshakeInterceptor();

    @Test
    @DisplayName("缺少已认证主体时拒绝握手")
    void shouldRejectHandshakeWithoutPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response),
                null,
                attributes
        );

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("存在已认证主体时允许握手并写入用户属性")
    void shouldAcceptAuthenticatedPrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(() -> "user-1");
        request.addUserRole("ADMIN");
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response),
                null,
                attributes
        );

        assertThat(allowed).isTrue();
        assertThat(attributes.get("userId")).isEqualTo("user-1");
        assertThat(attributes.get("tenantId")).isEqualTo("tenant-1");
        assertThat(attributes.get("isAdmin")).isEqualTo(Boolean.TRUE);
    }
}
