package com.basebackend.chat.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天模块 OpenAPI 文档配置
 */
@Configuration
public class ChatOpenApiConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI chatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("聊天微服务 API")
                        .version("1.0.0")
                        .description("basebackend-chat-api — 即时通讯、好友、群组管理接口文档"));
    }
}
