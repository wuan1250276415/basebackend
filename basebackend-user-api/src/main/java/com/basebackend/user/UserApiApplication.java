package com.basebackend.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 用户服务启动"
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.user",
        "com.basebackend.common",
        "com.basebackend.security",
        "com.basebackend.jwt",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.logging",
        "com.basebackend.observability",
        "com.basebackend.backup",
        "com.basebackend.feign",
        "com.basebackend.messaging"
})
@MapperScan({
    "com.basebackend.user.mapper",
    "com.basebackend.database.**.mapper",
    "com.basebackend.backup.**.mapper",
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.basebackend.feign.client"})
@EnableAspectJAutoProxy
public class UserApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
