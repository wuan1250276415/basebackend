package com.basebackend.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 通知中心服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.notification",
    "com.basebackend.common",
    "com.basebackend.web",
    "com.basebackend.database",
    "com.basebackend.cache",
    "com.basebackend.messaging",
    "com.basebackend.logging",
    "com.basebackend.security",
    "com.basebackend.observability"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
