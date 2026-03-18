package com.basebackend.gateway.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway profile 路由配置测试")
class GatewayProfileRouteConfigTest {

    @Test
    @DisplayName("gateway profile 应包含 messaging 管理路由")
    void shouldContainMessagingManagementRoute() throws IOException {
        Map<String, Object> config = loadYaml("application-gateway.yml");

        Map<String, Object> messagingRoute = routes(config).stream()
                .filter(route -> "messaging-management".equals(route.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 messaging-management 路由"));

        assertThat(messagingRoute.get("uri")).isEqualTo("lb://basebackend-system-api");
        assertThat(stringList(messagingRoute, "predicates")).contains("Path=/messaging/**");
    }

    @Test
    @DisplayName("messaging 管理路由不应剥离路径前缀")
    void shouldKeepMessagingPathPrefix() throws IOException {
        Map<String, Object> config = loadYaml("application-gateway.yml");

        Map<String, Object> messagingRoute = routes(config).stream()
                .filter(route -> "messaging-management".equals(route.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 messaging-management 路由"));

        assertThat(stringList(messagingRoute, "filters"))
                .noneMatch(filter -> filter.startsWith("StripPrefix"));
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

    @SuppressWarnings("unchecked")
    private List<String> stringList(Map<String, Object> route, String key) {
        Object value = route.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String classpathLocation) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new ClassPathResource(classpathLocation).getInputStream()) {
            return (Map<String, Object>) yaml.load(inputStream);
        }
    }
}
