package com.basebackend.dict;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 字典服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-13
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.dict",
        "com.basebackend.common",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.security",
        "com.basebackend.observability"
})
@EnableDiscoveryClient
@MapperScan("com.basebackend.dict.mapper")
public class DictServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DictServiceApplication.class, args);
    }
}
