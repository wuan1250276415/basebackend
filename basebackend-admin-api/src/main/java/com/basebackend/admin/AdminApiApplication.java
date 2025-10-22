package com.basebackend.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 后台管理API应用启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.admin",
        "com.basebackend.common",
        "com.basebackend.jwt",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.logging",
        "com.basebackend.observability",
        "com.basebackend.messaging",
        "com.basebackend.nacos",
        "com.basebackend.file"
})
@MapperScan("com.basebackend.admin.mapper")
@EnableDiscoveryClient
@EnableFeignClients
public class AdminApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
