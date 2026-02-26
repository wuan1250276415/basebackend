package com.basebackend.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 系统服务启动类
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.system",
        "com.basebackend.common",
})
@MapperScan({
        "com.basebackend.system.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class SystemApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApiApplication.class, args);
    }
}
