package com.basebackend.user;

import com.basebackend.user.config.UserApiNativeHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 用户服务启动"
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.user",
        "com.basebackend.common",
        "com.basebackend.security",
})
@MapperScan({
    "com.basebackend.user.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ImportRuntimeHints(UserApiNativeHints.class)
public class UserApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
