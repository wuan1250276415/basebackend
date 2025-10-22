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
 */
@SpringBootApplication(
        exclude = {
                DispatcherServletAutoConfiguration.class,
                WebMvcAutoConfiguration.class,
                ErrorMvcAutoConfiguration.class
        }
)
@Import(GatewayComponentScanConfig.class)
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
