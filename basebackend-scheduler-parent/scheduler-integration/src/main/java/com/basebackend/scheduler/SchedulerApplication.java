package com.basebackend.scheduler;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;

/**
 * BaseBackend Scheduler Service
 *
 * 任务调度与工作流编排服务
 *
 * <p>核心功能：
 * <ul>
 *   <li>PowerJob 分布式任务调度</li>
 *   <li>Camunda BPMN 工作流引擎</li>
 *   <li>延迟任务队列（Redis + RocketMQ）</li>
 *   <li>企业级治理特性（多租户、安全、监控、审计）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class
})
@ComponentScan(
        basePackages = {
                "com.basebackend.scheduler",
                "com.basebackend.common",
                "com.basebackend.security",
                "com.basebackend.jwt",
                "com.basebackend.database",
                "com.basebackend.cache",
                "com.basebackend.observability",
                "com.basebackend.feign",
                "com.basebackend.messaging"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.basebackend\\.database\\.migration\\..*")
        }
)
@MapperScan({
        "com.basebackend.scheduler.**.mapper",
        "com.basebackend.database.**.mapper",
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.basebackend.feign.client"})
public class SchedulerApplication {

    private static final Logger log = LoggerFactory.getLogger(SchedulerApplication.class);

    public static void main(String[] args) {
        log.info("========== Starting BaseBackend Scheduler Service ==========");
        SpringApplication.run(SchedulerApplication.class, args);
        log.info("========== BaseBackend Scheduler Service Started ==========");
    }

    /**
     * 应用启动完成后的回调
     */
    @Bean
    public ApplicationRunner applicationRunner(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String appName = env.getProperty("spring.application.name", "scheduler");
            
            log.info("========================================");
            log.info("  Application: {} is running!", appName);
            log.info("  Local URL: http://localhost:{}{}", port, contextPath);
            log.info("  Swagger UI: http://localhost:{}{}/doc.html", port, contextPath);
            log.info("  Actuator: http://localhost:{}{}/actuator/health", port, contextPath);
            log.info("========================================");
        };
    }
}
