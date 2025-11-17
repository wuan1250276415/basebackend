package com.basebackend.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 用户服务启动类
 *
 * 职责：
 * - 用户信息管理
 * - 用户 CRUD 操作
 * - 用户状态管理
 * - 部门信息管理（可选）
 *
 * @author 浮浮酱
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.user",
        "com.basebackend.common",
        "com.basebackend.database",
        "com.basebackend.cache",
        "com.basebackend.security",
        "com.basebackend.observability"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.basebackend")
@EnableCaching
@EnableTransactionManagement
@MapperScan("com.basebackend.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("""

                ========================================
                🎉 用户服务启动成功！
                📝 服务名称: basebackend-user-service
                🚀 服务端点: /api/users
                📖 API 文档: /swagger-ui.html
                ========================================
                """);
    }
}
