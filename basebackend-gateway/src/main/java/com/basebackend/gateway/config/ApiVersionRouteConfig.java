package com.basebackend.gateway.config;

import com.basebackend.gateway.filter.ApiVersionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 版本路由配置
 *
 * 根据不同版本的请求前缀将流量路由到对应的服务实例。
 * v1/v2/v3 分别对应稳定版、灰度版、实验版。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApiVersionRouteConfig {

    private final ApiVersionFilter apiVersionFilter;

    /**
     * 构建带版本控制的路由
     */
    @Bean
    public RouteLocator versionedRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ==================== V1 API · 稳定版 ====================
                .route("api-v1", r -> r
                        .path("/v1/**")
                        .filters(f -> f
                                // 加载版本解析过滤器
                                .filter(apiVersionFilter.apply(new ApiVersionFilter.Config()))
                                // 添加版本标识，方便下游审计
                                .addRequestHeader("X-API-Version", "v1")
                                .addRequestHeader("X-API-Stability", "stable")
                                // 将 /v1 前缀剥离后再转发
                                .rewritePath("/v1/(?<segment>.*)", "/${segment}")
                        )
                        .uri("lb://basebackend-admin-api")
                )

                // ==================== V2 API · 灰度版 ====================
                .route("api-v2", r -> r
                        .path("/v2/**")
                        .filters(f -> f
                                .filter(apiVersionFilter.apply(new ApiVersionFilter.Config()))
                                .addRequestHeader("X-API-Version", "v2")
                                .addRequestHeader("X-API-Stability", "beta")
                                .rewritePath("/v2/(?<segment>.*)", "/${segment}")
                                .addResponseHeader("X-API-Notice", "This is a beta version API")
                        )
                        .uri("lb://basebackend-admin-api-v2") // 可路由到不同的后端集群
                )

                // ==================== V3 API · 实验版 ====================
                .route("api-v3", r -> r
                        .path("/v3/**")
                        .filters(f -> f
                                .filter(apiVersionFilter.apply(new ApiVersionFilter.Config()))
                                .addRequestHeader("X-API-Version", "v3")
                                .addRequestHeader("X-API-Stability", "experimental")
                                .rewritePath("/v3/(?<segment>.*)", "/${segment}")
                                .addResponseHeader("X-API-Warning", "Experimental API - may change without notice")
                        )
                        .uri("lb://basebackend-admin-api-v3")
                )

                // ==================== 默认路由 · 无前缀 ====================
                .route("api-default", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                // 由于当前 Spring Cloud Gateway 版本尚未开放 DSL metadata 接口，这里直接通过过滤器注入默认版本
                                .filter(apiVersionFilter.apply(createDefaultConfig()))
                                .addRequestHeader("X-API-Version-Default", "true")
                        )
                        .uri("lb://basebackend-admin-api")
                )

                .build();
    }

    /**
     * 构建默认版本配置
     */
    private ApiVersionFilter.Config createDefaultConfig() {
        ApiVersionFilter.Config config = new ApiVersionFilter.Config();
        config.setDefaultVersion("v1");
        config.setStripVersionFromPath(true);
        return config;
    }
}
