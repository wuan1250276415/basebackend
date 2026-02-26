package com.basebackend.database.tenant.filter;

import com.basebackend.database.tenant.context.TenantContext;
import com.basebackend.database.tenant.resolver.HeaderTenantResolver;
import com.basebackend.database.tenant.resolver.TenantResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantContextFilter 测试")
class TenantContextFilterTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("正常解析租户并设置上下文")
    void resolveAndSetContext() throws Exception {
        TenantContextFilter filter = createFilter(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant001");
        request.setRequestURI("/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Filter 执行完后 context 已清除（finally 块）
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("忽略路径直接放行")
    void ignorePathPassThrough() throws Exception {
        TenantContextFilter filter = createFilter(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("required=true 且未解析到租户返回 403")
    void requiredModeReturns403() throws Exception {
        TenantContextFilter filter = createFilter(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
        // 不设置 X-Tenant-Id Header
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("租户标识缺失");
    }

    @Test
    @DisplayName("required=false 且未解析到租户正常放行")
    void optionalModePassThrough() throws Exception {
        TenantContextFilter filter = createFilter(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("请求结束后自动清除上下文（防泄漏）")
    void contextClearedAfterRequest() throws Exception {
        TenantContextFilter filter = createFilter(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant001");
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("/login 路径被忽略")
    void loginPathIgnored() throws Exception {
        TenantContextFilter filter = createFilter(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private TenantContextFilter createFilter(boolean required) {
        List<TenantResolver> resolvers = List.of(new HeaderTenantResolver());
        List<String> ignorePaths = List.of("/actuator", "/health", "/login", "/auth", "/public");
        return new TenantContextFilter(resolvers, ignorePaths, required);
    }
}
