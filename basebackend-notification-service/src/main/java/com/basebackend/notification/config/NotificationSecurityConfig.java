package com.basebackend.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;

/**
 * 通知服务安全配置
 * P0: 外部化敏感配置，支持环境变量注入
 *
 * @author BaseBackend Team
 * @since 2025-12-08
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "notification.security")
public class NotificationSecurityConfig {

    /**
     * 允许创建通知的角色列表
     * 默认只有管理员可以创建通知
     */
    private Set<String> allowedRoles = Set.of("ADMIN", "SYSTEM_ADMIN");

    /**
     * SSE 最大连接数限制
     * 防止内存溢出
     */
    @Min(100)
    @Max(100000)
    private int sseMaxConnections = 10000;

    /**
     * 邮件发送频率限制（每分钟）
     */
    @Min(1)
    @Max(1000)
    private int emailRateLimitPerMinute = 100;

    /**
     * 通知创建频率限制（每分钟）
     */
    @Min(1)
    @Max(10000)
    private int notificationRateLimitPerMinute = 1000;

    /**
     * 是否启用XSS过滤
     */
    private boolean xssFilterEnabled = true;

    /**
     * 邮件内容最大长度
     */
    @Min(100)
    @Max(1000000)
    private int emailContentMaxLength = 50000;

    /**
     * 通知内容最大长度
     */
    @Min(100)
    @Max(100000)
    private int notificationContentMaxLength = 10000;
}
