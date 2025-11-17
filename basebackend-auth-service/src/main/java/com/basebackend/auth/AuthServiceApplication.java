package com.basebackend.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证授权服务启动类
 *
 * 负责角色、权限、菜单管理等认证授权相关功能
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.auth",
    "com.basebackend.database",
    "com.basebackend.cache",
    "com.basebackend.common",
    "com.basebackend.web",
    "com.basebackend.observability"
})
@EnableDiscoveryClient
@EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
