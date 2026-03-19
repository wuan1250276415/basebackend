package com.basebackend.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
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
                .addServersItem(new Server()
                        .url("/basebackend-user-api")
                        .description("通过网关访问 User API"))
                .addServersItem(new Server()
                        .url("http://localhost:8081")
                        .description("直连 User API"))
                .info(new Info()
                        .title("用户服务 API")
                        .description("用户认证、用户管理与角色授权接口")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BaseBackend Team")
                                .email("admin@basebackend.com")
                        )
                );
    }
}
