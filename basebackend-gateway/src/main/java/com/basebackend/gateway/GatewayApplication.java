package com.basebackend.gateway;

import com.basebackend.gateway.config.GatewayComponentScanConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * 网关启动类
 * <p>
 * 注意：Gateway 使用 Spring WebFlux（反应式），需要排除所有 Servlet 相关的自动配置。
 * </p>
 */
@SpringBootApplication(scanBasePackages = { "com.basebackend.gateway" }, exclude = {
        DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class
},
        // 排除 common-starter 中的 Servlet 相关自动配置
        excludeName = {
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
