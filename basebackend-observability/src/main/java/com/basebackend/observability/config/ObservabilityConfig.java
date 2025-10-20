package com.basebackend.observability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 可观测性配置
 * 启用定时任务支持
 */
@Configuration
@EnableScheduling
public class ObservabilityConfig {
}
