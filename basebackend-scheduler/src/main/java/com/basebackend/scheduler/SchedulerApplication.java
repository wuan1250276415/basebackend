package com.basebackend.scheduler;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

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
 * <p>依赖原则：
 * <ul>
 *   <li>只依赖基础模块，不被业务模块依赖</li>
 *   <li>包扫描限制在 scheduler 和 common 模块，避免不合理依赖</li>
 *   <li>通过 REST API / OpenFeign 对外提供服务</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.scheduler",
        "com.basebackend.common",
        "com.basebackend.security",
        "com.basebackend.jwt",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.observability",
        "com.basebackend.feign",
        "com.basebackend.messaging",
        "com.basebackend.scheduler.camunda"
})
@MapperScan({
        "com.basebackend.database.**.mapper",
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.basebackend.feign.client"})
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
