package com.basebackend.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.basebackend.gateway.constant.GatewayConstants;
import com.basebackend.gateway.model.GatewayResult;
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
import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 白名单路径
     */
    private static final List<String> WHITELIST = Arrays.asList(
            "/admin-api/api/admin/auth/**",
            "/admin-api/swagger-ui/**",
            "/admin-api/v3/api-docs/**",
            "/admin-api/doc.html",
            "/admin-api/webjars/**",
            "/admin-api/favicon.ico",
            "/api/public/**",
            "/actuator/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        log.debug("认证过滤器 - 请求路径: {}, 方法: {}", path, request.getMethod());

        // 检查是否在白名单中
        if (isWhitelist(path)) {
            log.debug("路径 {} 在白名单中，跳过认证", path);
            return chain.filter(exchange);
        }

        // 获取Token
        String token = getTokenFromRequest(request);

        // 验证Token
        if (!StringUtils.hasText(token)) {
            log.warn("请求路径 {} 缺少Token", path);
            return unauthorized(exchange.getResponse(), "认证失败，缺少Token");
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("请求路径 {} 的Token无效", path);
            return unauthorized(exchange.getResponse(), "认证失败，Token无效");
        }

        // 从Token中获取用户名
        String username = jwtUtil.getSubjectFromToken(token);
        log.debug("Token验证成功，用户名: {}", username);

        // 检查Redis中的Token是否存在（防止强制下线后仍能访问）
        String redisTokenKey = LOGIN_TOKEN_KEY + username;
        return reactiveRedisTemplate.hasKey(redisTokenKey)
                .flatMap(exists -> {
                    if (!exists) {
                        log.warn("用户 {} 的Token在Redis中不存在，可能已被强制下线", username);
                        return unauthorized(exchange.getResponse(), "认证失败，Token已失效");
                    }

                    // 验证Redis中的Token是否与当前Token一致
                    return reactiveRedisTemplate.opsForValue().get(redisTokenKey)
                            .flatMap(redisToken -> {
                                if (!token.equals(redisToken.toString())) {
                                    log.warn("用户 {} 的Token与Redis中的Token不一致", username);
                                    return unauthorized(exchange.getResponse(), "认证失败，Token已失效");
                                }

                                // Token验证通过，添加用户信息到请求头
                                ServerHttpRequest mutatedRequest = request.mutate()
                                        .header("X-User-Id", username)
                                        .build();

                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            });
//                            .switchIfEmpty(Mono.defer(() -> {
//                                log.warn("用户 {} 的Token在Redis中为空", username);
//                                return unauthorized(exchange.getResponse(), "认证失败，Token已失效");
//                            }));
                })
                .onErrorResume(e -> {
                    log.error("Redis验证Token失败: {}", e.getMessage());
                    return unauthorized(exchange.getResponse(), "认证失败，服务异常");
                });
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelist(String path) {
        boolean result = WHITELIST.stream().anyMatch(pattern -> {
            boolean matches = pathMatcher.match(pattern, path);
            log.debug("路径匹配检查: 模式={}, 路径={}, 匹配={}", pattern, path, matches);
            return matches;
        });
        return result;
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
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        // 检查响应是否已经提交
        if (response.isCommitted()) {
            log.warn("Response already committed, cannot send unauthorized response");
            return Mono.empty();
        }
        
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        GatewayResult<?> result = GatewayResult.error(HttpStatus.UNAUTHORIZED.value(), message);
        String body = result.toJsonString();
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
