package com.basebackend.observability.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 可观测性自动配置
 */
@Slf4j
@Configuration
@ComponentScan("com.basebackend.observability")
@MapperScan("com.basebackend.observability.mapper")
@EnableScheduling
@ConditionalOnProperty(prefix = "observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    public ObservabilityAutoConfiguration() {
        log.info("Observability module initialized");
    }
}
