package com.basebackend.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.auth",
    "com.basebackend.common",
    "com.basebackend.cache",
    "com.basebackend.security",
    "com.basebackend.jwt",
    "com.basebackend.web"
})
@EnableDiscoveryClient
@EnableFeignClients
public class AuthApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApiApplication.class, args);
    }
}
