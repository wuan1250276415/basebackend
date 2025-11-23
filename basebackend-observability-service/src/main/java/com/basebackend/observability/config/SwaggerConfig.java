package com.basebackend.observability.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability 服务 Swagger/OpenAPI 配置。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI observabilityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Observability Service API")
                        .description("日志、指标、链路、告警相关接口")
                        .version("1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Gateway (8080)"));
    }

    @Bean
    public GroupedOpenApi logApi() {
        return GroupedOpenApi.builder()
                .group("日志查询")
                .pathsToMatch("/api/logs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi metricsApi() {
        return GroupedOpenApi.builder()
                .group("指标查询")
                .pathsToMatch("/api/metrics/**")
                .build();
    }

    @Bean
    public GroupedOpenApi traceApi() {
        return GroupedOpenApi.builder()
                .group("链路查询")
                .pathsToMatch("/api/traces/**")
                .build();
    }

    @Bean
    public GroupedOpenApi alertApi() {
        return GroupedOpenApi.builder()
                .group("告警管理")
                .pathsToMatch("/api/alerts/**")
                .build();
    }
}
