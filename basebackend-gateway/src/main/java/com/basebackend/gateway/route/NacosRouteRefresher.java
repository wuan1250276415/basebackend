package com.basebackend.gateway.route;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Nacos路由刷新器
 * 从Nacos动态加载和刷新路由配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NacosRouteRefresher {

    private final DynamicRouteService dynamicRouteService;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}")
    private String serverAddr;

    @Value("${spring.cloud.nacos.config.namespace:}")
    private String namespace;

    @Value("${spring.cloud.nacos.config.username:}")
    private String username;

    @Value("${spring.cloud.nacos.config.password:}")
    private String password;

    private static final String ROUTE_DATA_ID = "gateway-routes";
    private static final String ROUTE_GROUP = "DEFAULT_GROUP";
    private static final Duration ROUTE_REFRESH_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 当前已加载的路由 ID 集合（用于删减同步）
     */
    private final Set<String> currentRouteIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        try {
            log.info("Nacos路由监听器初始化开始，serverAddr: {}, namespace: {}", serverAddr, namespace);
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);
            properties.put("namespace", namespace);
            properties.put("group", ROUTE_GROUP);
            properties.put("username", username);
            properties.put("password", password);
            ConfigService configService = NacosFactory.createConfigService(properties);

            // 初始加载路由配置
            String configInfo = configService.getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            if (configInfo != null && !configInfo.isEmpty()) {
                updateRoutes(configInfo);
            }

            // 监听路由配置变化
            configService.addListener(ROUTE_DATA_ID, ROUTE_GROUP, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("接收到Nacos路由配置变更");
                    updateRoutes(configInfo);
                }
            });

            log.info("Nacos路由监听器初始化成功");
        } catch (NacosException e) {
            log.error("Nacos路由监听器初始化失败", e);
        }
    }

    /**
     * 更新路由配置
     */
    private void updateRoutes(String configInfo) {
        try {
            List<RouteDefinition> routes = objectMapper.readValue(
                    configInfo,
                    new TypeReference<List<RouteDefinition>>() {}
            );

            applyRouteChanges(routes).block(ROUTE_REFRESH_TIMEOUT);
            log.info("更新路由配置成功，共{}条，当前生效路由{}条", routes.size(), currentRouteIds.size());
        } catch (Exception e) {
            log.error("更新路由配置失败", e);
        }
    }

    /**
     * 串行应用路由变更：
     * 1) 删除 Nacos 中已移除的路由
     * 2) 串行 upsert 当前路由
     */
    private Mono<Void> applyRouteChanges(List<RouteDefinition> routes) {
        Set<String> nextRouteIds = routes.stream()
                .map(RouteDefinition::getId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Set<String> staleRouteIds = new HashSet<>(currentRouteIds);
        staleRouteIds.removeAll(nextRouteIds);

        return Flux.fromIterable(staleRouteIds)
                .concatMap(routeId -> executeRouteOperation("删除", routeId, dynamicRouteService.deleteRoute(routeId)))
                .thenMany(Flux.fromIterable(routes)
                        .filter(route -> {
                            if (StringUtils.hasText(route.getId())) {
                                return true;
                            }
                            log.warn("忽略无效路由定义（缺少 id）: {}", route);
                            return false;
                        })
                        .concatMap(route -> executeRouteOperation("更新", route.getId(), dynamicRouteService.updateRoute(route))))
                .then(Mono.fromRunnable(() -> {
                    currentRouteIds.clear();
                    currentRouteIds.addAll(nextRouteIds);
                }));
    }

    private Mono<Void> executeRouteOperation(String operation, String routeId, Mono<String> resultMono) {
        return resultMono.flatMap(result -> {
            if (!"success".equalsIgnoreCase(result)) {
                return Mono.error(new IllegalStateException(
                        String.format("%s路由失败: routeId=%s, result=%s", operation, routeId, result)));
            }
            log.info("{}路由成功: {}", operation, routeId);
            return Mono.empty();
        });
    }
}
