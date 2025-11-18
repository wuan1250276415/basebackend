package com.basebackend.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 可观测性服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.observability",
    "com.basebackend.common",
    "com.basebackend.web",
    "com.basebackend.database",
    "com.basebackend.cache",
    "com.basebackend.logging",
    "com.basebackend.security",
        "com.basebackend.jwt"
})
public class ObservabilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservabilityServiceApplication.class, args);
    }
}
