package com.basebackend.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.user",
    "com.basebackend.common",
    "com.basebackend.database",
    "com.basebackend.cache",
    "com.basebackend.security",
    "com.basebackend.web"
})
@EnableDiscoveryClient
public class UserApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
