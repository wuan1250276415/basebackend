package com.basebackend.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流路由配置
 * 将 /api/workflow/** 路由到 basebackend-scheduler 服务
 */
@Configuration
public class WorkflowRouteConfig {

    @Bean
    public RouteLocator workflowRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 工作流API路由
                .route("workflow-route", r -> r
                        .path("/api/workflow/**")
                        .filters(f -> f
                                .stripPrefix(0) // 不去除前缀，保持 /api/workflow/...
                                .rewritePath("/api/workflow/(?<segment>.*)", "/scheduler/api/workflow/${segment}") // 重写路径添加 /scheduler 前缀
                        )
                        .uri("lb://basebackend-scheduler") // 通过负载均衡路由到scheduler服务
                )
                .build();
    }
}
