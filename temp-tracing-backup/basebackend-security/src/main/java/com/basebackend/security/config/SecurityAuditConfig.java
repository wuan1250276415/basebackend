package com.basebackend.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 安全审计配置
 * 配置安全审计拦截器、切面和异步处理
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class SecurityAuditConfig {

    /**
     * 创建异步执行器
     * 用于异步处理审计日志，避免影响业务性能
     */
    @Bean("auditExecutor")
    @ConditionalOnProperty(name = "security.audit.async.enabled", havingValue = "true", matchIfMissing = true)
    public Executor auditExecutor() {
        log.info("初始化安全审计异步执行器");
        return Executors.newFixedThreadPool(10);
    }

    /**
     * 创建审计消费者
     * 消费Kafka中的审计事件并存储到数据库
     */
    @Bean
    @ConditionalOnProperty(name = "security.audit.kafka.enabled", havingValue = "true")
    public SecurityAuditConsumer securityAuditConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new SecurityAuditConsumer(kafkaTemplate);
    }
}

/**
 * 审计消费者
 * 消费Kafka中的审计事件并存储到数据库
 */
class SecurityAuditConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SecurityAuditConsumer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
}
