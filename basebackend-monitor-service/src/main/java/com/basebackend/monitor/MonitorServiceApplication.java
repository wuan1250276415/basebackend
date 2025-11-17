package com.basebackend.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 监控服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.monitor",
    "com.basebackend.web",
    "com.basebackend.cache",
    "com.basebackend.logging",
    "com.basebackend.nacos"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.basebackend.feign")
public class MonitorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorServiceApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  监控服务启动成功！");
        System.out.println("  Monitor Service Started Successfully!");
        System.out.println("  Port: 8089");
        System.out.println("  API Docs: http://localhost:8089/swagger-ui.html");
        System.out.println("========================================\n");
    }
}
