package com.basebackend.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger API文档配置
 * 
 * @author BaseBackend Team
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI systemApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("系统服务 API")
                .description("字典、菜单、部门、日志管理接口文档")
                .version("1.0.0")
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi dictApi() {
        return GroupedOpenApi.builder()
            .group("字典管理")
            .pathsToMatch("/api/dicts/**")
            .build();
    }

    @Bean
    public GroupedOpenApi menuApi() {
        return GroupedOpenApi.builder()
            .group("菜单管理")
            .pathsToMatch("/api/menus/**")
            .build();
    }

    @Bean
    public GroupedOpenApi deptApi() {
        return GroupedOpenApi.builder()
            .group("部门管理")
            .pathsToMatch("/api/depts/**")
            .build();
    }

    @Bean
    public GroupedOpenApi logApi() {
        return GroupedOpenApi.builder()
            .group("日志管理")
            .pathsToMatch("/api/logs/**")
            .build();
    }
}
