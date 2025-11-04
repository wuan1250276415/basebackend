package com.basebackend.scheduler.camunda.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI workflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("工作流管理 API")
                        .description("基于 Camunda BPM 的工作流管理系统 API 文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Base Backend Team")
                                .email("admin@basebackend.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8085")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://localhost:8081")
                                .description("网关环境")
                ));
    }
}
