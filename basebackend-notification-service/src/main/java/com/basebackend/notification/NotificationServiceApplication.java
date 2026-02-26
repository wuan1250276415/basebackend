package com.basebackend.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 通知中心服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */

@SpringBootApplication(scanBasePackages = {
        "com.basebackend.notification",
        "com.basebackend.common",
})
@MapperScan({
        "com.basebackend.notification.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
