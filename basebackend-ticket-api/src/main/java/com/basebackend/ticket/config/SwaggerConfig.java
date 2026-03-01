package com.basebackend.ticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/Knife4j 配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url("/")
                        .description("通过网关访问 Ticket API"))
                .addServersItem(new Server()
                        .url("http://localhost:8085")
                        .description("直连 Ticket API"))
                .info(new Info()
                        .title("工单管理系统API")
                        .description("企业工单管理系统 - 工单CRUD、审批流、SLA监控")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BaseBackend Team")
                                .email("admin@basebackend.com")
                        )
                );
    }
}
