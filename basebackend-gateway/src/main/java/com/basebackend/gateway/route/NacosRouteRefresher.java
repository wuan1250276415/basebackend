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

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

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

            for (RouteDefinition route : routes) {
                dynamicRouteService.updateRoute(route);
            }

            log.info("更新路由配置成功，共{}条", routes.size());
        } catch (Exception e) {
            log.error("更新路由配置失败", e);
        }
    }
}
