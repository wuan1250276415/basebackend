package com.basebackend.gateway;

import com.basebackend.gateway.config.GatewayComponentScanConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 网关启动类
 * <p>
 * 注意：Gateway 使用 Spring WebFlux（反应式），需要排除所有 Servlet 相关的自动配置。
 * Boot 4 中 Servlet 自动配置类路径变更，使用 excludeName 字符串形式避免编译依赖。
 * </p>
 */
@SpringBootApplication(scanBasePackages = { "com.basebackend.gateway" },
        excludeName = {
                "org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration",
                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
                "org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration",
                "com.basebackend.common.starter.CommonAutoConfiguration",
                "com.basebackend.common.starter.config.UserContextAutoConfiguration"
        })
@Import(GatewayComponentScanConfig.class)
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
