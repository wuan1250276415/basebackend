package com.basebackend.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo API 应用启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.demo",
        "com.basebackend.common",
        "com.basebackend.jwt",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.security",
        "com.basebackend.file",
        "com.basebackend.logging",
        "com.basebackend.observability"
})
@MapperScan("com.basebackend.demo.mapper")
public class DemoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApiApplication.class, args);
    }
}
