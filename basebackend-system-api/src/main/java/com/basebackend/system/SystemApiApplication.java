package com.basebackend.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 系统服务启动类
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.system",
    "com.basebackend.common",
    "com.basebackend.database",
    "com.basebackend.cache",
    "com.basebackend.web",
    "com.basebackend.logging",
    "com.basebackend.jwt"
})
@EnableDiscoveryClient
public class SystemApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApiApplication.class, args);
    }
}
