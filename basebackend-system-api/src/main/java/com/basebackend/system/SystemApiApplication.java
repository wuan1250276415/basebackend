package com.basebackend.system;

import com.basebackend.system.config.SystemApiNativeHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 系统服务启动类
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.system",
        "com.basebackend.common",
        "com.basebackend.security",
})
@MapperScan({
        "com.basebackend.system.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ImportRuntimeHints(SystemApiNativeHints.class)
public class SystemApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApiApplication.class, args);
    }
}
