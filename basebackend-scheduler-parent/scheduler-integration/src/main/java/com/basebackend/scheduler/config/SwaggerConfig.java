package com.basebackend.scheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 配置
 *
 * <p>为 basebackend-scheduler 服务提供 API 文档功能，支持：
 * <ul>
 *   <li>Camunda 工作流 API 文档</li>
 *   <li>PowerJob 任务调度 API 文档</li>
 *   <li>延迟队列 API 文档</li>
 * </ul>
 *
 * <p>访问地址：
 * <ul>
 *   <li>Knife4j 文档：http://localhost:8085/doc.html</li>
 *   <li>Swagger UI：http://localhost:8085/swagger-ui/index.html</li>
 *   <li>OpenAPI JSON：http://localhost:8085/v3/api-docs</li>
 * </ul>
 *
 * <p>安全认证：
 * <ul>
 *   <li>所有 API 默认需要 JWT Bearer Token 认证</li>
 *   <li>可通过 Knife4j UI 右上角"授权"按钮配置 Token</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Configuration
public class SwaggerConfig {

    /**
     * 安全认证方案名称
     */
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * 配置全局 OpenAPI 信息
     *
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI basebackendSchedulerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BaseBackend Scheduler API")
                        .description("""
                                任务调度与工作流编排服务 API 文档

                                **核心功能：**
                                - PowerJob 分布式任务调度
                                - Camunda BPMN 工作流引擎
                                - 延迟任务队列（Redis + RocketMQ）
                                - 企业级治理特性（多租户、安全、监控、审计）
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Scheduler Team")
                                .email("scheduler@basebackend.com")
                                .url("https://basebackend.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("请输入 JWT Token（格式：Bearer {token}）")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * Camunda 工作流 API 分组
     *
     * <p>包含流程定义、流程实例、任务、历史记录等相关 API。
     *
     * @return GroupedOpenApi 实例
     */
    @Bean
    public GroupedOpenApi camundaWorkflowApi() {
        return GroupedOpenApi.builder()
                .group("1. Camunda 工作流")
                .pathsToMatch("/api/camunda/**", "/camunda/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                    return operation;
                })
                .build();
    }

    /**
     * PowerJob 任务调度 API 分组
     *
     * <p>包含任务注册、调度、执行、监控等相关 API。
     *
     * @return GroupedOpenApi 实例
     */
    @Bean
    public GroupedOpenApi powerJobSchedulerApi() {
        return GroupedOpenApi.builder()
                .group("2. PowerJob 任务调度")
                .pathsToMatch("/api/powerjob/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                    return operation;
                })
                .build();
    }

    /**
     * 延迟队列 API 分组
     *
     * <p>包含延迟任务的创建、查询、取消等相关 API。
     *
     * @return GroupedOpenApi 实例
     */
    @Bean
    public GroupedOpenApi delayQueueApi() {
        return GroupedOpenApi.builder()
                .group("3. 延迟任务队列")
                .pathsToMatch("/api/delay/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                    return operation;
                })
                .build();
    }

    /**
     * 公共 API 分组
     *
     * <p>包含健康检查、监控指标等公共 API。
     *
     * @return GroupedOpenApi 实例
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("4. 公共接口")
                .pathsToMatch("/actuator/**", "/api/health/**", "/api/metrics/**")
                .build();
    }
}
