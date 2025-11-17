package com.basebackend.observability.metrics;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 指标配置类
 * 初始化业务指标和自定义指标
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "observability.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MetricsConfiguration {

    private final BusinessMetrics businessMetrics;

    /**
     * 初始化业务指标
     */
    @PostConstruct
    public void initMetrics() {
        try {
            log.info("Initializing business metrics...");
            businessMetrics.initBusinessMetrics();
            log.info("Business metrics initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize business metrics", e);
        }
    }
}
