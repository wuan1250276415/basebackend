package com.basebackend.dept;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 部门服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-13
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.dept",
        "com.basebackend.common",
        "com.basebackend.database",
        "com.basebackend.security",
        "com.basebackend.observability"
})
@EnableDiscoveryClient
@MapperScan("com.basebackend.dept.mapper")
public class DeptServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeptServiceApplication.class, args);
    }
}
