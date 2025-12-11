package com.basebackend.security.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Web 安全基线配置项。
 * 支持配置验证和健康检查。
 */
@Slf4j
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.baseline")
public class SecurityBaselineProperties {

    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^https?://[\\w.-]+(:\\d+)?$");

    /**
     * 允许的请求来源（Origin 或 Referer 前缀），为空时不强制校验
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 是否强制校验 Referer，当 Origin 缺失时生效
     */
    private boolean enforceReferer = true;

    /**
     * 速率限制配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * JWT配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * 配置初始化后验证
     */
    @PostConstruct
    public void validate() {
        validateAllowedOrigins();
        validateRateLimitConfig();
        log.info("SecurityBaselineProperties 配置验证通过");
    }

    private void validateAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            log.warn("allowedOrigins 为空，Origin/Referer 校验将被跳过");
            return;
        }

        List<String> invalidOrigins = new ArrayList<>();
        for (String origin : allowedOrigins) {
            if (origin == null || origin.isBlank()) {
                continue;
            }
            // 允许通配符
            if ("*".equals(origin)) {
                log.warn("allowedOrigins 包含通配符 '*'，这在生产环境中不安全");
                continue;
            }
            if (!ORIGIN_PATTERN.matcher(origin).matches()) {
                invalidOrigins.add(origin);
            }
        }

        if (!invalidOrigins.isEmpty()) {
            log.warn("以下 allowedOrigins 格式可能不正确: {}", invalidOrigins);
        }
    }

    private void validateRateLimitConfig() {
        if (rateLimit.maxAttempts < 1) {
            log.warn("rateLimit.maxAttempts 应该 >= 1，当前值: {}", rateLimit.maxAttempts);
        }
        if (rateLimit.blockDuration.isNegative() || rateLimit.blockDuration.isZero()) {
            log.warn("rateLimit.blockDuration 应该 > 0，当前值: {}", rateLimit.blockDuration);
        }
    }

    /**
     * 检查配置是否健康
     */
    public boolean isHealthy() {
        return rateLimit.maxAttempts >= 1
                && !rateLimit.blockDuration.isNegative()
                && !rateLimit.blockDuration.isZero();
    }

    /**
     * 速率限制配置
     */
    @Getter
    @Setter
    public static class RateLimitConfig {
        /**
         * 最大尝试次数
         */
        @Min(1)
        private int maxAttempts = 5;

        /**
         * 封禁时长
         */
        private Duration blockDuration = Duration.ofMinutes(15);

        /**
         * 时间窗口
         */
        private Duration windowDuration = Duration.ofMinutes(5);

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * JWT配置
     */
    @Getter
    @Setter
    public static class JwtConfig {
        /**
         * Token黑名单TTL（小时）
         */
        @Min(1)
        private int blacklistTtlHours = 24;

        /**
         * 是否启用Token黑名单
         */
        private boolean blacklistEnabled = true;
    }
}

