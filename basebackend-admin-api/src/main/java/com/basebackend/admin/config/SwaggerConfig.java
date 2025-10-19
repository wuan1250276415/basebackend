package com.basebackend.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("后台管理系统API")
                        .description("基于RBAC权限模型的后台管理系统")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BaseBackend Team")
                                .email("admin@basebackend.com")
                        )
                );
    }
}