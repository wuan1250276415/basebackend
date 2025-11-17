package com.basebackend.oauth2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * OAuth2.0授权服务器启动类
 * 提供OAuth2.0和OpenID Connect完整支持
 */
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.oauth2",
    "com.basebackend.common",
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
public class OAuth2Application {

    public static void main(String[] args) {
        SpringApplication.run(OAuth2Application.class, args);
        System.out.println("""

                ╔═══════════════════════════════════════════════════════════════════╗
                ║               BaseBackend OAuth2.0 授权服务器启动成功                ║
                ╠═══════════════════════════════════════════════════════════════════╣
                ║  🌐 服务地址: http://localhost:8082                               ║
                ║  📖 文档地址: http://localhost:8082/swagger-ui.html               ║
                ║  🔐 授权端点: http://localhost:8082/oauth2/authorize               ║
                ║  🎫 令牌端点: http://localhost:8082/oauth2/token                   ║
                ║  👤 用户信息: http://localhost:8082/oauth2/userinfo                ║
                ║  🔑 JWK集端点: http://localhost:8082/oauth2/jwks                   ║
                ╠═══════════════════════════════════════════════════════════════════╣
                ║  支持的认证模式:                                                  ║
                ║  • 授权码模式 (Authorization Code)                                ║
                ║  • 客户端模式 (Client Credentials)                                ║
                ║  • 密码模式 (Password)                                            ║
                ║  • 简化模式 (Implicit) [已废弃]                                   ║
                ║  • 刷新令牌 (Refresh Token)                                       ║
                ╠═══════════════════════════════════════════════════════════════════╣
                ║  客户端配置:                                                      ║
                ║  • Web应用: basebackend-web / web-client-secret                   ║
                ║  • 移动应用: basebackend-mobile / mobile-client-secret            ║
                ║  • 微服务: basebackend-service / service-client-secret            ║
                ╠═══════════════════════════════════════════════════════════════════╣
                ║  📝 OAuth2.0客户端注册地址: http://localhost:8082/connect/register ║
                ║  📊 健康检查: http://localhost:8082/actuator/health               ║
                ╚═══════════════════════════════════════════════════════════════════╝
                """);
    }
}
