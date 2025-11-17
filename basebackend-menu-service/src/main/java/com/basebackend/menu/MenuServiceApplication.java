package com.basebackend.menu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 菜单资源管理服务启动类
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.menu",
    "com.basebackend.common",
    "com.basebackend.web",
    "com.basebackend.database",
    "com.basebackend.cache",
    "com.basebackend.logging",
    "com.basebackend.security",
    "com.basebackend.nacos"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.basebackend.feign"})
@MapperScan("com.basebackend.menu.mapper")
public class MenuServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MenuServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("🎉 菜单资源管理服务启动成功！");
        System.out.println("📍 服务端口: 8088");
        System.out.println("📊 Druid 监控: http://localhost:8088/druid");
        System.out.println("🏥 健康检查: http://localhost:8088/actuator/health");
        System.out.println("========================================");
    }
}
