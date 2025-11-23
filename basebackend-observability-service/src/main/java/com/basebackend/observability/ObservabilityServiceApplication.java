package com.basebackend.observability;

import org.mybatis.spring.annotation.MapperScan;
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
        "com.basebackend.common",
        "com.basebackend.jwt",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.logging",
        "com.basebackend.observability",
})
@MapperScan({
        "com.basebackend.observability.mapper",
        "com.basebackend.database.**.mapper",
})
public class ObservabilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservabilityServiceApplication.class, args);
    }
}
