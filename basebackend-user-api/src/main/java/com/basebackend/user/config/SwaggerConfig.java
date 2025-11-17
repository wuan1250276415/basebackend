package com.basebackend.user.config;

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
    public OpenAPI userApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("用户服务 API")
                .description("用户、角色、权限管理接口文档")
                .version("1.0.0")
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("用户管理")
            .pathsToMatch("/api/users/**")
            .build();
    }

    @Bean
    public GroupedOpenApi roleApi() {
        return GroupedOpenApi.builder()
            .group("角色管理")
            .pathsToMatch("/api/roles/**")
            .build();
    }

    @Bean
    public GroupedOpenApi permissionApi() {
        return GroupedOpenApi.builder()
            .group("权限管理")
            .pathsToMatch("/api/permissions/**")
            .build();
    }
}
