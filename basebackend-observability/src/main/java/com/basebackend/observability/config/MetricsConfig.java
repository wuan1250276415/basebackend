//package com.basebackend.observability.config;
//
//import io.micrometer.core.instrument.MeterRegistry;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 指标监控配置
// */
//@Slf4j
//@Configuration
//public class MetricsConfig {
//
//    @Value("${spring.application.name:basebackend}")
//    private String applicationName;
//
//    /**
//     * 自定义 MeterRegistry
//     * 为所有指标添加通用标签
//     */
//    @Bean
//    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
//        return registry -> {
//            registry.config()
//                    .commonTags("application", applicationName)
//                    .commonTags("environment", getEnvironment());
//            log.info("配置指标监控，应用名称: {}, 环境: {}", applicationName, getEnvironment());
//        };
//    }
//
//    /**
//     * 获取环境信息
//     */
//    private String getEnvironment() {
//        String env = System.getProperty("spring.profiles.active");
//        return env != null ? env : "default";
//    }
//}
