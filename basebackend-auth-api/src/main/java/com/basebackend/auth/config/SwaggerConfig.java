package com.basebackend.auth.config;

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
    public OpenAPI authApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("认证服务 API")
                .description("登录、认证、授权、会话管理接口文档")
                .version("1.0.0")
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("认证授权")
            .pathsToMatch("/api/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi sessionApi() {
        return GroupedOpenApi.builder()
            .group("会话管理")
            .pathsToMatch("/api/sessions/**")
            .build();
    }
}
