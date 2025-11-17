package com.basebackend.profile;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户档案服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.profile",
    "com.basebackend.web",
    "com.basebackend.database",
    "com.basebackend.security",
    "com.basebackend.logging",
    "com.basebackend.nacos",
    "com.basebackend.observability"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.basebackend.feign")
@MapperScan("com.basebackend.profile.mapper")
public class ProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  用户档案服务启动成功！");
        System.out.println("  Profile Service Started Successfully!");
        System.out.println("  Port: 8090");
        System.out.println("  API Docs: http://localhost:8090/swagger-ui.html");
        System.out.println("========================================\n");
    }
}
