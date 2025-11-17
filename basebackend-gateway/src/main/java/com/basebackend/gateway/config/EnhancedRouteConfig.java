package com.basebackend.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.time.Duration;

/**
 * Gateway 增强路由配置
 *
 * 通过路由级别的重试、熔断、头部标记等能力加强各业务入口的稳健性。
 */
@Slf4j
@Configuration
public class
EnhancedRouteConfig {

    /**
     * 定义增强路由
     */
    @Bean
    public RouteLocator enhancedRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ==================== Admin API · 增强策略 ====================
                .route("admin-api-enhanced", r -> r
                        .path("/admin-api/**")
                        .and()
                        .method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                        .filters(f -> f
                                .rewritePath("/admin-api/(?<segment>.*)", "/api/${segment}")
                                .addRequestHeader("X-Gateway-Source", "BaseBackend-Gateway")
                                .addRequestHeader("X-Request-From", "Gateway")
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.BAD_GATEWAY,
                                                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                                                org.springframework.http.HttpStatus.GATEWAY_TIMEOUT)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(
                                                Duration.ofMillis(100),
                                                Duration.ofMillis(1000),
                                                2,
                                                true
                                        )
                                )
                                .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                        )
                        .uri("lb://basebackend-admin-api")
                )

                // ==================== Demo API · 增强策略 ====================
                .route("demo-api-enhanced", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .retry(retryConfig -> retryConfig
                                        .setRetries(2)
                                        .setStatuses(org.springframework.http.HttpStatus.BAD_GATEWAY)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, true)
                                )
                                .addRequestHeader("X-Gateway-Source", "BaseBackend-Gateway")
                        )
                        .uri("lb://basebackend-demo-api")
                )

                // ==================== 文件服务 · 上传优化 ====================
                .route("file-service-enhanced", r -> r
                        .path("/api/files/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .retry(retryConfig -> retryConfig
                                        .setRetries(1)
                                        .setMethods(HttpMethod.GET)
                                )
                                .addRequestHeader("X-Gateway-Source", "BaseBackend-Gateway")
                        )
                        .uri("lb://file-service")
                )

                // ==================== 静态资源 · 缓存优化 ====================
                .route("static-resources", r -> r
                        .path("/static/**", "/assets/**", "/favicon.ico")
                        .filters(f -> f
                                .addResponseHeader("Cache-Control", "public, max-age=86400")
                        )
                        .uri("lb://static-service")
                )

                // ==================== 健康检查 · 高优级 ====================
                .route("health-check", r -> r
                        .path("/actuator/health", "/health")
                        .uri("lb://basebackend-admin-api")
                )

                .build();
    }

}
