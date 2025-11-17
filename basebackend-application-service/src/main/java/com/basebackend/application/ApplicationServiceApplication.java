package com.basebackend.application;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 应用服务启动类
 *
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.application",
        "com.basebackend.common",
        "com.basebackend.database",
        "com.basebackend.security",
        "com.basebackend.observability"
})
@EnableDiscoveryClient
@MapperScan("com.basebackend.application.mapper")
public class ApplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationServiceApplication.class, args);
    }
}
