package com.basebackend.web.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 配置类
 * 简化版本，仅用于初始化基本配置
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SentinelConfig {

    private final MeterRegistry meterRegistry;

    /**
     * 初始化 Sentinel 指标监控
     */
    @PostConstruct
    public void initSentinelMetrics() {
        Counter counter = Counter.builder("sentinel.request.total")
                .description("Total requests handled by Sentinel")
                .tag("type", "flow")
                .register(meterRegistry);

        log.info("Sentinel 指标监控初始化完成");
    }
}
