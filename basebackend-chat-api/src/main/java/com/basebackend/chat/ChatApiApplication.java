package com.basebackend.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 聊天微服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.chat",
        "com.basebackend.common",
})
@MapperScan("com.basebackend.chat.mapper")
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class ChatApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApiApplication.class, args);
    }
}
