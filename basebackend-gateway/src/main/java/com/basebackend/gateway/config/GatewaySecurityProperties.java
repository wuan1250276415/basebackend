package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 网关安全配置属性
 * <p>
 * 从配置文件中读取认证白名单、超时设置等安全相关配置，
 * 避免硬编码，提高配置灵活性。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private static final String USER_AUTH_PREFIX = "/basebackend-user-api/api/user/auth/";

    private static final LinkedHashSet<String> SAFE_USER_AUTH_WHITELIST = new LinkedHashSet<>(List.of(
            "/basebackend-user-api/api/user/auth/login",
            "/basebackend-user-api/api/user/auth/refresh",
            "/basebackend-user-api/api/user/auth/wechat-login"));

    /**
     * 基础白名单
     * 这些公开路径是网关的默认基线，避免被外部配置整表覆盖后误伤公开接口。
     */
    private static final List<String> BASELINE_WHITELIST = List.of(
            "/basebackend-user-api/api/user/auth/login",
            "/basebackend-user-api/api/user/auth/refresh",
            "/basebackend-user-api/api/user/auth/wechat-login",
            "/basebackend-user-api/swagger-ui/**",
            "/basebackend-user-api/v3/api-docs/**",
            "/basebackend-user-api/doc.html",
            "/basebackend-user-api/webjars/**",
            "/basebackend-user-api/favicon.ico",
            "/basebackend-system-api/api/system/depts/tree",
            "/basebackend-system-api/api/system/depts/by-name",
            "/basebackend-system-api/api/system/depts/by-code",
            "/basebackend-system-api/api/system/depts/batch",
            "/basebackend-system-api/api/system/depts/by-parent",
            "/basebackend-system-api/api/system/application/enabled",
            "/basebackend-system-api/api/system/application/code/**",
            "/basebackend-system-api/api/system/notifications/stream",
            "/basebackend-system-api/api/system/dicts/**",
            "/basebackend-system-api/swagger-ui/**",
            "/basebackend-system-api/v3/api-docs/**",
            "/basebackend-system-api/doc.html",
            "/basebackend-system-api/webjars/**",
            "/basebackend-ticket-api/swagger-ui/**",
            "/basebackend-ticket-api/v3/api-docs/**",
            "/basebackend-ticket-api/doc.html",
            "/basebackend-ticket-api/webjars/**",
            "/basebackend-notification-service/api/notifications/stream");

    /**
     * 基础 actuator 白名单
     * 只允许最小健康探针集合，确保容器健康检查不会被配置覆盖破坏。
     */
    private static final List<String> BASELINE_ACTUATOR_WHITELIST = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info");

    /**
     * 认证白名单路径
     * 支持 Ant 风格路径匹配
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * Actuator 端点白名单
     * 默认只允许 health 端点
     */
    private List<String> actuatorWhitelist = new ArrayList<>();

    /**
     * Redis 操作超时时间
     */
    private Duration redisTimeout = Duration.ofSeconds(2);

    /**
     * 是否启用严格模式
     * 严格模式下，未配置的路径都需要认证
     */
    private boolean strictMode = true;

    /**
     * 是否记录认证详细日志
     */
    private boolean debugLogging = false;

    /**
     * 获取合并后的完整白名单
     * 包括基础白名单、用户配置白名单和 actuator 白名单。
     */
    public List<String> getFullWhitelist() {
        LinkedHashSet<String> fullList = new LinkedHashSet<>(BASELINE_WHITELIST);
        whitelist.stream()
                .filter(this::isSafeWhitelistPattern)
                .forEach(fullList::add);
        fullList.addAll(BASELINE_ACTUATOR_WHITELIST);
        fullList.addAll(actuatorWhitelist);
        return new ArrayList<>(fullList);
    }

    private boolean isSafeWhitelistPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }

        if (!pattern.startsWith(USER_AUTH_PREFIX)) {
            return true;
        }

        return SAFE_USER_AUTH_WHITELIST.contains(pattern);
    }
}
