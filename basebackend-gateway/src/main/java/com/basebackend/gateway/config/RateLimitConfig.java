package com.basebackend.gateway.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 限流配置
 *
 * 统一定义 Redis 限流器与多种 KeyResolver，支撑 IP / 用户 / API 维度的限流策略。
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "gateway.rate-limit")
@Data
public class RateLimitConfig {

    /**
     * 是否开启限流
     */
    private boolean enabled = true;

    /**
     * 默认限流规则
     */
    private RateLimitRule defaultRule = new RateLimitRule(100, 200);

    /**
     * 服务级限流配置
     */
    private Map<String, RateLimitRule> services = new HashMap<>();

    /**
     * API 级限流配置
     */
    private Map<String, RateLimitRule> apis = new HashMap<>();

    /**
     * IP 维度限流器（默认）
     */
    @Bean
    @Primary
    public RedisRateLimiter ipRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * 用户维度限流器
     */
    @Bean
    public RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /**
     * API 维度限流器
     */
    @Bean
    public RedisRateLimiter apiRateLimiter() {
        return new RedisRateLimiter(200, 400, 1);
    }

    /**
     * 鉴权接口专用限流器
     */
    @Bean
    public RedisRateLimiter authApiRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * 文件上传限流器
     */
    @Bean
    public RedisRateLimiter fileUploadRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * IP KeyResolver（默认）
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            log.debug("IP 限流 Key: {}", ip);
            return Mono.just(ip);
        };
    }

    /**
     * 用户 KeyResolver
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null) {
                userId = exchange.getRequest().getHeaders().getFirst("Authorization");
            }
            String key = userId != null ? userId : "anonymous";
            log.debug("用户限流 Key: {}", key);
            return Mono.just(key);
        };
    }

    /**
     * API 路径 KeyResolver
     */
    @Bean
    public KeyResolver apiPathKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            String apiKey = path.split("\\?")[0];
            log.debug("API 限流 Key: {}", apiKey);
            return Mono.just(apiKey);
        };
    }

    /**
     * 组合 KeyResolver（IP+User+Path）
     */
    @Bean
    public KeyResolver compositeKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            userId = userId != null ? userId : "anonymous";
            String path = exchange.getRequest().getPath().value();
            String compositeKey = ip + ":" + userId + ":" + path;
            log.debug("组合限流 Key: {}", compositeKey);
            return Mono.just(compositeKey);
        };
    }

    /**
     * 初始化默认规则
     */
    public void initDefaultRules() {
        if (services.isEmpty()) {
            services.put("basebackend-admin-api", new RateLimitRule(100, 200));
            services.put("basebackend-demo-api", new RateLimitRule(200, 400));
            services.put("file-service", new RateLimitRule(10, 20));
            log.info("已初始化默认服务限流规则");
        }

        if (apis.isEmpty()) {
            apis.put("/api/auth/login", new RateLimitRule(5, 10));
            apis.put("/api/auth/register", new RateLimitRule(3, 6));
            apis.put("/api/files/upload", new RateLimitRule(5, 10));
            apis.put("/api/users/list", new RateLimitRule(200, 400));
            log.info("已初始化默认 API 限流规则");
        }
    }

    /**
     * 获取服务限流配置
     */
    public RateLimitRule getServiceRule(String serviceId) {
        return services.getOrDefault(serviceId, defaultRule);
    }

    /**
     * 获取 API 限流配置
     */
    public RateLimitRule getApiRule(String apiPath) {
        return apis.getOrDefault(apiPath, defaultRule);
    }

    /**
     * 限流规则定义
     */
    @Data
    public static class RateLimitRule {
        private int replenishRate;
        private int burstCapacity;
        private int requestedTokens = 1;

        public RateLimitRule() {
        }

        public RateLimitRule(int replenishRate, int burstCapacity) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
        }

        public RateLimitRule(int replenishRate, int burstCapacity, int requestedTokens) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
            this.requestedTokens = requestedTokens;
        }
    }
}
