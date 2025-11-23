package com.basebackend.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
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
                    .url("http://www.apache.org/licenses/LICENSE-2.0")))
            .addServersItem(new Server()
                    .url("http://localhost:8080")
                    .description("Gateway (8080)"));
    }

    @Bean
    public GroupedOpenApi dictApi() {
        return GroupedOpenApi.builder()
            .group("字典管理")
            .pathsToMatch("/api/system/dicts/**")
            .build();
    }

    @Bean
    public GroupedOpenApi applicationApi() {
        return GroupedOpenApi.builder()
            .group("应用管理")
            .pathsToMatch("/api/system/application/**")
            .build();
    }
    @Bean
    public GroupedOpenApi applicationResourceApi() {
        return GroupedOpenApi.builder()
            .group("应用资源管理")
            .pathsToMatch("/api/system/application/resource/**")
            .build();
    }
    @Bean
    public GroupedOpenApi listOperationsApi() {
        return GroupedOpenApi.builder()
            .group("列表操作管理")
            .pathsToMatch("/api/admin/list-operations/**")
            .build();
    }


    @Bean
    public GroupedOpenApi deptApi() {
        return GroupedOpenApi.builder()
            .group("部门管理")
            .pathsToMatch("/api/system/depts/**")
            .build();
    }

    @Bean
    public GroupedOpenApi logApi() {
        return GroupedOpenApi.builder()
            .group("日志管理")
            .pathsToMatch("/api/system/logs/**")
            .build();
    }

    @Bean
    public GroupedOpenApi monitorApi() {
        return GroupedOpenApi.builder()
            .group("系统监控控制器")
            .pathsToMatch("/api/system/monitor/**")
            .build();
    }

    @Bean
    public GroupedOpenApi permissionsApi() {
        return GroupedOpenApi.builder()
            .group("权限管理")
            .pathsToMatch("/api/system/permissions/**")
            .build();
    }
}
