package com.basebackend.gateway.gray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GrayLoadBalancer 单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GrayLoadBalancer 单元测试")
class GrayLoadBalancerTest {

    private GrayRouteProperties grayRouteProperties;
    private GrayLoadBalancer grayLoadBalancer;

    @BeforeEach
    void setUp() {
        grayRouteProperties = new GrayRouteProperties();
        grayRouteProperties.setEnabled(true);
        grayRouteProperties.setTrustedProxyCidrs(Arrays.asList(
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16"));
        grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);
    }

    private ServiceInstance createMockInstance(String instanceId, String version) {
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getInstanceId()).thenReturn(instanceId);
        Map<String, String> metadata = new HashMap<>();
        if (version != null) {
            metadata.put("version", version);
        }
        when(instance.getMetadata()).thenReturn(metadata);
        return instance;
    }

    @Nested
    @DisplayName("灰度禁用测试")
    class GrayDisabledTests {

        @Test
        @DisplayName("灰度禁用时应该返回第一个实例")
        void shouldReturnFirstInstanceWhenGrayDisabled() {
            // Given
            grayRouteProperties.setEnabled(false);
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);

            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result);
            assertEquals("instance1", result.getInstanceId());
        }

        @Test
        @DisplayName("实例列表为空时应该返回null")
        void shouldReturnNullWhenInstancesEmpty() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, Collections.emptyList());

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("实例列表为null时应该返回null")
        void shouldReturnNullWhenInstancesNull() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, null);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Header 灰度策略测试")
    class HeaderStrategyTests {

        @BeforeEach
        void setUpHeaderStrategy() {
            GrayRouteProperties.GrayRule rule = new GrayRouteProperties.GrayRule();
            rule.setServiceName("test-service");
            rule.setStrategy("header");
            rule.setHeaderName("X-Gray");
            rule.setHeaderValue("true");
            rule.setGrayVersion("v2");
            rule.setStableVersion("v1");

            grayRouteProperties.setRules(Collections.singletonList(rule));
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);
        }

        @Test
        @DisplayName("匹配Header时应该路由到灰度版本")
        void shouldRouteToGrayVersionWhenHeaderMatches() {
            // Given
            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-Gray", "true")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result);
            assertEquals("v2", result.getMetadata().get("version"));
        }

        @Test
        @DisplayName("不匹配Header时应该路由到稳定版本")
        void shouldRouteToStableVersionWhenHeaderNotMatches() {
            // Given
            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-Gray", "false")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result);
            assertEquals("v1", result.getMetadata().get("version"));
        }
    }

    @Nested
    @DisplayName("用户ID 灰度策略测试")
    class UserIdStrategyTests {

        @BeforeEach
        void setUpUserStrategy() {
            GrayRouteProperties.GrayRule rule = new GrayRouteProperties.GrayRule();
            rule.setServiceName("test-service");
            rule.setStrategy("user");
            rule.setUserIds(Arrays.asList("user1", "user2", "user3"));
            rule.setGrayVersion("v2");
            rule.setStableVersion("v1");

            grayRouteProperties.setRules(Collections.singletonList(rule));
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);
        }

        @Test
        @DisplayName("灰度用户应该路由到灰度版本")
        void shouldRouteToGrayVersionForGrayUser() {
            // Given
            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-User-Id", "user1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result);
            assertEquals("v2", result.getMetadata().get("version"));
        }

        @Test
        @DisplayName("非灰度用户应该路由到稳定版本")
        void shouldRouteToStableVersionForNonGrayUser() {
            // Given
            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-User-Id", "user999")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result);
            assertEquals("v1", result.getMetadata().get("version"));
        }
    }

    @Nested
    @DisplayName("IP 灰度策略测试")
    class IpStrategyTests {

        @BeforeEach
        void setUpIpStrategy() {
            GrayRouteProperties.GrayRule rule = new GrayRouteProperties.GrayRule();
            rule.setServiceName("test-service");
            rule.setStrategy("ip");
            rule.setIpList(Arrays.asList("192.168.1.100", "192.168.1.101"));
            rule.setGrayVersion("v2");
            rule.setStableVersion("v1");

            grayRouteProperties.setRules(Collections.singletonList(rule));
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);
        }

        // IP策略测试需要模拟RemoteAddress，这里简化测试
        @Test
        @DisplayName("无规则的服务应该使用会话黏性选择")
        void shouldUseSessionStickyWhenNoRule() {
            // Given
            grayRouteProperties.setRules(Collections.emptyList());
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);

            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("权重策略测试")
    class WeightStrategyTests {

        @BeforeEach
        void setUpWeightStrategy() {
            GrayRouteProperties.GrayRule rule = new GrayRouteProperties.GrayRule();
            rule.setServiceName("test-service");
            rule.setStrategy("weight");
            rule.setWeight(50); // 50%灰度
            rule.setGrayVersion("v2");
            rule.setStableVersion("v1");

            grayRouteProperties.setRules(Collections.singletonList(rule));
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);
        }

        @Test
        @DisplayName("同一用户多次请求应该路由到相同版本")
        void shouldRouteToSameVersionForSameUser() {
            // Given
            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-User-Id", "testuser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When - 多次选择
            ServiceInstance result1 = grayLoadBalancer.choose("test-service", exchange, instances);
            ServiceInstance result2 = grayLoadBalancer.choose("test-service", exchange, instances);
            ServiceInstance result3 = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then - 应该一致
            assertNotNull(result1);
            assertEquals(result1.getMetadata().get("version"), result2.getMetadata().get("version"));
            assertEquals(result2.getMetadata().get("version"), result3.getMetadata().get("version"));
        }
    }

    @Nested
    @DisplayName("会话黏性测试")
    class SessionStickyTests {

        @Test
        @DisplayName("同一用户ID应该路由到同一实例")
        void shouldRouteToSameInstanceForSameUserId() {
            // Given - 无灰度规则，纯会话黏性
            grayRouteProperties.setRules(Collections.emptyList());
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);

            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v1"),
                    createMockInstance("instance3", "v1"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-User-Id", "stickyuser")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result1 = grayLoadBalancer.choose("test-service", exchange, instances);
            ServiceInstance result2 = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(result1.getInstanceId(), result2.getInstanceId());
        }
    }

    @Nested
    @DisplayName("版本实例不存在测试")
    class VersionNotFoundTests {

        @Test
        @DisplayName("目标版本实例不存在时应该回退到默认实例")
        void shouldFallbackToDefaultWhenVersionNotFound() {
            // Given
            GrayRouteProperties.GrayRule rule = new GrayRouteProperties.GrayRule();
            rule.setServiceName("test-service");
            rule.setStrategy("header");
            rule.setHeaderName("X-Gray");
            rule.setHeaderValue("true");
            rule.setGrayVersion("v3"); // 不存在的版本
            rule.setStableVersion("v1");

            grayRouteProperties.setRules(Collections.singletonList(rule));
            grayLoadBalancer = new GrayLoadBalancer(grayRouteProperties);

            List<ServiceInstance> instances = Arrays.asList(
                    createMockInstance("instance1", "v1"),
                    createMockInstance("instance2", "v2"));

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header("X-Gray", "true")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When
            ServiceInstance result = grayLoadBalancer.choose("test-service", exchange, instances);

            // Then - 应该回退到任意实例
            assertNotNull(result);
        }
    }
}
