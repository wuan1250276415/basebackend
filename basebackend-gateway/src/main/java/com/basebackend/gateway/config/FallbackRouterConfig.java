package com.basebackend.gateway.config;

import com.basebackend.gateway.handler.FallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Gateway 故障路由配置
 *
 * 统一暴露熔断后的兜底接口，便于调用方获取友好提示。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FallbackRouterConfig {

    private final FallbackHandler fallbackHandler;

    /**
     * 注册多个兜底路由
     */
    @Bean
    public RouterFunction<ServerResponse> fallbackRouterFunction() {
        return RouterFunctions
                // Admin API 的兜底路径
                .route(path("/fallback/admin-api"), fallbackHandler)
                // Demo API 的兜底路径
                .andRoute(path("/fallback/demo-api"), fallbackHandler)
                // 文件服务兜底路径
                .andRoute(path("/fallback/file-service"), fallbackHandler)
                // 兜底的最终 fallback
                .andRoute(path("/fallback/**"), fallbackHandler);
    }
}
