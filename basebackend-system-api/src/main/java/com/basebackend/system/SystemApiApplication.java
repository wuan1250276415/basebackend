package com.basebackend.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 系统服务启动类
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.system",
        "com.basebackend.common",
        "com.basebackend.security",
        "com.basebackend.jwt",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.logging",
        "com.basebackend.observability",
        "com.basebackend.backup",
        "com.basebackend.feign",
        "com.basebackend.file",
})
@MapperScan({
        "com.basebackend.system.mapper",
        "com.basebackend.database.**.mapper",
        "com.basebackend.backup.**.mapper",
        "com.basebackend.file.mapper",
        "com.basebackend.file.**.mapper"
})
@EnableFeignClients(basePackages = {"com.basebackend.feign.client"})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class SystemApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApiApplication.class, args);
    }
}
