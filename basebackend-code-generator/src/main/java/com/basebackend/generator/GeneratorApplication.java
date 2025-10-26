package com.basebackend.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 代码生成器服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.basebackend")
public class GeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeneratorApplication.class, args);
    }
}
