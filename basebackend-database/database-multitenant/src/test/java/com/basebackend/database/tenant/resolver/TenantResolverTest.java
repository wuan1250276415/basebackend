package com.basebackend.database.tenant.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.*;

@DisplayName("租户解析器测试")
class TenantResolverTest {

    // ==================== HeaderTenantResolver ====================

    @Nested
    @DisplayName("HeaderTenantResolver 测试")
    class HeaderTenantResolverTest {

        private final HeaderTenantResolver resolver = new HeaderTenantResolver();

        @Test
        @DisplayName("从默认 Header 解析租户 ID")
        void resolveFromDefaultHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "tenant001");

            assertThat(resolver.resolve(request)).isEqualTo("tenant001");
        }

        @Test
        @DisplayName("自定义 Header 名称")
        void resolveFromCustomHeader() {
            HeaderTenantResolver customResolver = new HeaderTenantResolver("Tenant-Code");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Tenant-Code", "custom_tenant");

            assertThat(customResolver.resolve(request)).isEqualTo("custom_tenant");
        }

        @Test
        @DisplayName("Header 不存在返回 null")
        void headerNotPresent() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("Header 为空白返回 null")
        void headerBlank() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "   ");

            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("Header 值自动 trim")
        void headerTrimmed() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "  tenant001  ");

            assertThat(resolver.resolve(request)).isEqualTo("tenant001");
        }

        @Test
        @DisplayName("优先级为 10")
        void orderIs10() {
            assertThat(resolver.getOrder()).isEqualTo(10);
        }
    }

    // ==================== DomainTenantResolver ====================

    @Nested
    @DisplayName("DomainTenantResolver 测试")
    class DomainTenantResolverTest {

        private final DomainTenantResolver resolver = new DomainTenantResolver();

        @Test
        @DisplayName("从子域名解析租户")
        void resolveFromSubdomain() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("tenant1.api.example.com");

            assertThat(resolver.resolve(request)).isEqualTo("tenant1");
        }

        @Test
        @DisplayName("排除 www 子域名")
        void excludeWww() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("www.example.com");

            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("排除 api 子域名")
        void excludeApi() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("api.example.com");

            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("排除 admin 子域名")
        void excludeAdmin() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("admin.example.com");

            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("域名段不足3段返回 null")
        void shortDomain() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("example.com");

            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("localhost 返回 null")
        void localhost() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("localhost");

            assertThat(resolver.resolve(request)).isNull();
        }

        @Test
        @DisplayName("优先级为 30")
        void orderIs30() {
            assertThat(resolver.getOrder()).isEqualTo(30);
        }
    }
}
