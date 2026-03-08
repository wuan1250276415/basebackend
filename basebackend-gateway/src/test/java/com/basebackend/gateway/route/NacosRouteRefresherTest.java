package com.basebackend.gateway.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NacosRouteRefresher 单元测试")
class NacosRouteRefresherTest {

    @Mock
    private DynamicRouteService dynamicRouteService;

    private NacosRouteRefresher nacosRouteRefresher;

    @BeforeEach
    void setUp() {
        nacosRouteRefresher = new NacosRouteRefresher(dynamicRouteService, new ObjectMapper());
    }

    @Test
    @DisplayName("stale 路由应触发删除并同步 currentRouteIds")
    void shouldDeleteStaleRoutesAndSyncCurrentRouteIds() {
        setCurrentRouteIds(Set.of("route-old", "route-keep"));
        when(dynamicRouteService.deleteRoute("route-old")).thenReturn(Mono.just("success"));
        when(dynamicRouteService.updateRoute(any(RouteDefinition.class))).thenReturn(Mono.just("success"));

        List<RouteDefinition> routes = List.of(
                routeDefinition("route-keep"),
                routeDefinition("route-new"));

        Mono<Void> applyResult = ReflectionTestUtils.invokeMethod(nacosRouteRefresher, "applyRouteChanges", routes);
        StepVerifier.create(applyResult).verifyComplete();

        verify(dynamicRouteService).deleteRoute("route-old");
        verify(dynamicRouteService, times(2)).updateRoute(any(RouteDefinition.class));
        assertThat(currentRouteIds()).containsExactlyInAnyOrder("route-keep", "route-new");
    }

    @Test
    @DisplayName("新路由更新应串行执行")
    void shouldUpdateRoutesSequentially() {
        setCurrentRouteIds(Set.of());
        List<String> lifecycleEvents = new ArrayList<>();

        when(dynamicRouteService.updateRoute(any(RouteDefinition.class))).thenAnswer(invocation -> {
            RouteDefinition route = invocation.getArgument(0);
            return Mono.defer(() -> {
                lifecycleEvents.add("start:" + route.getId());
                return Mono.delay(Duration.ofMillis(20))
                        .then(Mono.fromRunnable(() -> lifecycleEvents.add("end:" + route.getId())))
                        .thenReturn("success");
            });
        });

        List<RouteDefinition> routes = List.of(
                routeDefinition("route-a"),
                routeDefinition("route-b"),
                routeDefinition("route-c"));

        Mono<Void> applyResult = ReflectionTestUtils.invokeMethod(nacosRouteRefresher, "applyRouteChanges", routes);
        StepVerifier.create(applyResult).verifyComplete();

        assertThat(lifecycleEvents).containsExactly(
                "start:route-a", "end:route-a",
                "start:route-b", "end:route-b",
                "start:route-c", "end:route-c");
        verify(dynamicRouteService, times(3)).updateRoute(any(RouteDefinition.class));
    }

    @Test
    @DisplayName("动态配置删减后 currentRouteIds 应同步")
    void shouldSyncCurrentRouteIdsAfterConfigRouteReduction() {
        setCurrentRouteIds(Set.of("route-a", "route-b"));
        when(dynamicRouteService.deleteRoute("route-b")).thenReturn(Mono.just("success"));
        when(dynamicRouteService.updateRoute(any(RouteDefinition.class))).thenReturn(Mono.just("success"));

        String configJson = """
                [
                  { "id": "route-a", "uri": "lb://svc-a", "predicates": [] }
                ]
                """;

        ReflectionTestUtils.invokeMethod(nacosRouteRefresher, "updateRoutes", configJson);

        verify(dynamicRouteService).deleteRoute("route-b");
        verify(dynamicRouteService).updateRoute(any(RouteDefinition.class));
        assertThat(currentRouteIds()).containsExactly("route-a");
    }

    private RouteDefinition routeDefinition(String routeId) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(routeId);
        routeDefinition.setUri(java.net.URI.create("lb://" + routeId));
        return routeDefinition;
    }

    @SuppressWarnings("unchecked")
    private Set<String> currentRouteIds() {
        return (Set<String>) ReflectionTestUtils.getField(nacosRouteRefresher, "currentRouteIds");
    }

    private void setCurrentRouteIds(Set<String> routeIds) {
        Set<String> currentRouteIds = currentRouteIds();
        currentRouteIds.clear();
        currentRouteIds.addAll(routeIds);
    }
}
