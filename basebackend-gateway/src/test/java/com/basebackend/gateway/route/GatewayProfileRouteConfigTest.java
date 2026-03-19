package com.basebackend.gateway.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway profile 路由配置测试")
class GatewayProfileRouteConfigTest {

    @Test
    @DisplayName("gateway profile 应包含关键静态路由")
    void shouldContainExpectedStaticRoutes() throws IOException {
        Map<String, Object> config = loadYaml("application-gateway.yml");
        List<Map<String, Object>> routeList = routes(config);
        Map<String, Map<String, Object>> routes = routesById(config);

        assertThat(routes.keySet()).containsAll(expectedRouteIds());

        assertRoute(routes, "user-api", "lb://basebackend-user-api", "Path=/basebackend-user-api/**");
        assertHasStripPrefix(routes, "user-api");

        assertRoute(routes, "system-api", "lb://basebackend-system-api", "Path=/basebackend-system-api/**");
        assertHasStripPrefix(routes, "system-api");

        assertRoute(routes, "messaging-management", "lb://basebackend-system-api", "Path=/messaging/**");
        assertNoStripPrefix(routes, "messaging-management");

        assertRoute(routes, "notification-service-with-prefix", "lb://basebackend-notification-service", "Path=/basebackend-notification-service/**");
        assertHasStripPrefix(routes, "notification-service-with-prefix");

        assertRoute(routes, "notification-sse", "lb://basebackend-notification-service", "Path=/api/notifications/stream");
        assertNoStripPrefix(routes, "notification-sse");
        assertRoute(routes, "notification-service", "lb://basebackend-notification-service", "Path=/api/notifications/**");
        assertNoStripPrefix(routes, "notification-service");
        assertRouteOrder(routeList, "notification-sse", "notification-service");

        assertRoute(routes, "observability-service-with-prefix", "lb://basebackend-observability-service", "Path=/basebackend-observability-service/**");
        assertHasStripPrefix(routes, "observability-service-with-prefix");

        assertRoute(routes, "observability-metrics", "lb://basebackend-observability-service", "Path=/api/metrics/**");
        assertNoStripPrefix(routes, "observability-metrics");

        assertRoute(routes, "observability-traces", "lb://basebackend-observability-service", "Path=/api/traces/**");
        assertNoStripPrefix(routes, "observability-traces");

        assertRoute(routes, "observability-logs", "lb://basebackend-observability-service", "Path=/api/logs/**");
        assertNoStripPrefix(routes, "observability-logs");

        assertRoute(routes, "observability-alerts", "lb://basebackend-observability-service", "Path=/api/alerts/**");
        assertNoStripPrefix(routes, "observability-alerts");

        assertRoute(routes, "ticket-api", "lb://basebackend-ticket-api", "Path=/api/ticket/**");
        assertNoStripPrefix(routes, "ticket-api");

        assertRoute(routes, "mall-product-api", "lb://basebackend-mall-product-api", "Path=/api/mall/products/**");
        assertNoStripPrefix(routes, "mall-product-api");

        assertRoute(routes, "mall-trade-api", "lb://basebackend-mall-trade-api", "Path=/api/mall/trades/**");
        assertNoStripPrefix(routes, "mall-trade-api");

        assertRoute(routes, "mall-pay-api", "lb://basebackend-mall-pay-api", "Path=/api/mall/payments/**");
        assertNoStripPrefix(routes, "mall-pay-api");

        assertRoute(routes, "file-service", "lb://basebackend-file-service", "Path=/api/files/**");
        assertNoStripPrefix(routes, "file-service");

        assertThat(routes.keySet()).doesNotContain(legacyAuthRouteId(), legacyAdminRouteId());
        assertThat(grayRuleServiceNames(config)).doesNotContain(legacyAdminRouteId(), legacyDemoServiceName());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> routes(Map<String, Object> config) {
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
        Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
        Map<String, Object> server = (Map<String, Object>) gateway.get("server");
        Map<String, Object> webflux = (Map<String, Object>) server.get("webflux");
        return (List<Map<String, Object>>) webflux.get("routes");
    }

    private Map<String, Map<String, Object>> routesById(Map<String, Object> config) {
        Map<String, Map<String, Object>> routes = new LinkedHashMap<>();
        for (Map<String, Object> route : routes(config)) {
            routes.put(String.valueOf(route.get("id")), route);
        }
        return routes;
    }

    private Set<String> expectedRouteIds() {
        return Set.of(
                "user-api",
                "system-api",
                "messaging-management",
                "notification-service-with-prefix",
                "notification-service",
                "notification-sse",
                "observability-service-with-prefix",
                "observability-metrics",
                "observability-traces",
                "observability-logs",
                "observability-alerts",
                "ticket-api",
                "mall-product-api",
                "mall-trade-api",
                "mall-pay-api",
                "file-service"
        );
    }

    private String legacyAuthRouteId() {
        return "auth" + "-api";
    }

    private String legacyAdminRouteId() {
        return "admin" + "-api";
    }

    private String legacyDemoServiceName() {
        return "basebackend-" + "demo" + "-api";
    }

    @SuppressWarnings("unchecked")
    private List<String> grayRuleServiceNames(Map<String, Object> config) {
        Map<String, Object> gateway = (Map<String, Object>) config.get("gateway");
        Map<String, Object> gray = (Map<String, Object>) gateway.get("gray");
        Object rules = gray.get("rules");
        if (!(rules instanceof List<?> ruleList)) {
            return Collections.emptyList();
        }
        return ruleList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(rule -> String.valueOf(rule.get("serviceName")))
                .toList();
    }

    private void assertRoute(Map<String, Map<String, Object>> routes, String routeId, String expectedUri, String expectedPath) {
        Map<String, Object> route = routes.get(routeId);
        assertThat(route).as("未找到 %s 路由", routeId).isNotNull();
        assertThat(route.get("uri")).isEqualTo(expectedUri);
        assertThat(stringList(route, "predicates")).contains(expectedPath);
    }

    private void assertNoStripPrefix(Map<String, Map<String, Object>> routes, String routeId) {
        Map<String, Object> route = routes.get(routeId);
        assertThat(stringList(route, "filters"))
                .noneMatch(filter -> filter.startsWith("StripPrefix"));
    }

    private void assertHasStripPrefix(Map<String, Map<String, Object>> routes, String routeId) {
        Map<String, Object> route = routes.get(routeId);
        assertThat(stringList(route, "filters"))
                .anyMatch(filter -> filter.startsWith("StripPrefix"));
    }

    private void assertRouteOrder(List<Map<String, Object>> routeList, String firstRouteId, String secondRouteId) {
        assertThat(routeIndex(routeList, firstRouteId)).isLessThan(routeIndex(routeList, secondRouteId));
    }

    private int routeIndex(List<Map<String, Object>> routeList, String routeId) {
        for (int i = 0; i < routeList.size(); i++) {
            if (routeId.equals(String.valueOf(routeList.get(i).get("id")))) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Map<String, Object> route, String key) {
        Object value = route.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        return ((List<Object>) value).stream()
                .map(String::valueOf)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String classpathLocation) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new ClassPathResource(classpathLocation).getInputStream()) {
            return (Map<String, Object>) yaml.load(inputStream);
        }
    }
}
