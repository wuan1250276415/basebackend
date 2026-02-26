package com.basebackend.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 用户服务启动"
 * 
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.user",
        "com.basebackend.common",
})
@MapperScan({
    "com.basebackend.user.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class UserApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
