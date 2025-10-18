package com.basebackend.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.gateway",
        "com.basebackend.common",
        "com.basebackend.jwt"
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
