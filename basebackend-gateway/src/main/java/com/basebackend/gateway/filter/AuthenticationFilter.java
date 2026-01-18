package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.GatewaySecurityProperties;
import com.basebackend.gateway.constant.GatewayConstants;
import com.basebackend.gateway.enums.GatewayErrorCode;
import com.alibaba.fastjson2.JSON;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 认证过滤器
 * <p>
 * 实现 JWT Token 验证和 Redis 会话校验的全局认证过滤器。
 * 支持通过配置文件管理白名单路径，避免硬编码。
 * </p>
 * 
 * <h3>安全特性：</h3>
 * <ul>
 * <li>JWT Token 验证</li>
 * <li>Redis 会话双重校验（防止强制下线后仍能访问）</li>
 * <li>可配置的白名单路径</li>
 * <li>Redis 操作超时保护</li>
 * <li>默认受限的 actuator 端点访问</li>
 * <li>统一的错误码枚举（{@link GatewayErrorCode}）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final GatewaySecurityProperties securityProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 默认 Redis 超时时间（当配置未设置时使用）
     */
    private static final Duration DEFAULT_REDIS_TIMEOUT = Duration.ofSeconds(2);

    /**
     * Exchange 属性：标记认证是否已完成（防止响应式流重复触发）
     */
    private static final String AUTH_COMPLETED_ATTR = "auth.completed";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 幂等性保护：检查是否已经完成认证
        // 在响应式流中，filter 可能被多次订阅触发（特别是 multipart 上传请求）
        Boolean authCompleted = exchange.getAttribute(AUTH_COMPLETED_ATTR);
        if (Boolean.TRUE.equals(authCompleted)) {
            if (securityProperties.isDebugLogging()) {
                log.debug("认证已完成，跳过重复验证");
            }
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        if (securityProperties.isDebugLogging()) {
            log.debug("认证过滤器 - 请求路径: {}, 方法: {}", path, request.getMethod());
        }

        // 检查是否在白名单中
        if (isWhitelist(path)) {
            if (securityProperties.isDebugLogging()) {
                log.debug("路径 {} 在白名单中，跳过认证", path);
            }
            return chain.filter(exchange);
        }

        // 获取Token
        String token = getTokenFromRequest(request);

        // 验证Token
        if (!StringUtils.hasText(token)) {
            log.warn("请求路径 {} 缺少Token", path);
            return unauthorized(exchange.getResponse(), GatewayErrorCode.TOKEN_MISSING);
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("请求路径 {} 的Token无效", path);
            return unauthorized(exchange.getResponse(), GatewayErrorCode.TOKEN_INVALID);
        }

        // 从Token中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 防御性检查：userId不能为null
        if (userId == null) {
            log.warn("请求路径 {} 的Token中不包含有效的用户ID", path);
            return unauthorized(exchange.getResponse(), GatewayErrorCode.USER_ID_INVALID);
        }

        if (securityProperties.isDebugLogging()) {
            log.debug("Token验证成功，用户id: {}", userId);
        }

        // 获取超时时间
        Duration timeout = securityProperties.getRedisTimeout() != null
                ? securityProperties.getRedisTimeout()
                : DEFAULT_REDIS_TIMEOUT;

        // 检查Redis中的Token是否存在（防止强制下线后仍能访问）
        // 添加超时控制，防止 Redis 操作无限等待
        String redisTokenKey = LOGIN_TOKEN_KEY + userId;
        final Long finalUserId = userId;
        final String finalToken = token;

        return reactiveRedisTemplate.opsForValue().get(redisTokenKey)
                .timeout(timeout)
                .flatMap(redisToken -> {
                    // 防止NPE：显式检查null
                    if (redisToken == null) {
                        log.warn("用户 {} 的Token在Redis中不存在，可能已被强制下线", finalUserId);
                        return unauthorized(exchange.getResponse(), GatewayErrorCode.TOKEN_EXPIRED);
                    }

                    // 安全地转换为字符串进行比较
                    String redisTokenStr = String.valueOf(redisToken);
                    if (!finalToken.equals(redisTokenStr)) {
                        log.warn("用户 {} 的Token与Redis中的Token不一致", finalUserId);
                        return unauthorized(exchange.getResponse(), GatewayErrorCode.TOKEN_MISMATCH);
                    }

                    // 标记认证完成（防止响应式流重复触发时再次验证）
                    exchange.getAttributes().put(AUTH_COMPLETED_ATTR, Boolean.TRUE);

                    // Token验证通过，添加用户信息到请求头
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(finalUserId))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                // 处理Redis返回空值的情况（key不存在时）
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("用户 {} 的Token在Redis中不存在，可能已被强制下线", finalUserId);
                    return unauthorized(exchange.getResponse(), GatewayErrorCode.TOKEN_EXPIRED);
                }))
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("Redis验证Token超时，用户ID: {}", finalUserId);
                    return unauthorized(exchange.getResponse(), GatewayErrorCode.AUTH_SERVICE_BUSY);
                })
                .onErrorResume(e -> {
                    log.error("Redis验证Token失败: {}", e.getMessage(), e);
                    return unauthorized(exchange.getResponse(), GatewayErrorCode.AUTH_SERVICE_ERROR);
                })
                // 使用 cache() 确保 Mono 只被订阅执行一次
                .cache();
    }

    /**
     * 检查路径是否在白名单中
     * 使用配置文件中的白名单，支持动态更新
     */
    private boolean isWhitelist(String path) {
        List<String> whitelist = securityProperties.getFullWhitelist();

        // 如果白名单为空且是严格模式，所有路径都需要认证
        if (whitelist.isEmpty() && securityProperties.isStrictMode()) {
            return false;
        }

        return whitelist.stream().anyMatch(pattern -> {
            boolean matches = pathMatcher.match(pattern, path);
            if (securityProperties.isDebugLogging() && matches) {
                log.debug("路径匹配: 模式={}, 路径={}", pattern, path);
            }
            return matches;
        });
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(GatewayConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(GatewayConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(GatewayConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 返回未授权响应（使用错误码枚举）
     *
     * @param response  HTTP 响应
     * @param errorCode 网关错误码
     * @return Mono<Void>
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, GatewayErrorCode errorCode) {
        // 检查响应是否已经提交
        if (response.isCommitted()) {
            log.warn("Response already committed, cannot send unauthorized response");
            return Mono.empty();
        }

        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        response.setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));

        Result<?> result = Result.error(errorCode);
        String body = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
