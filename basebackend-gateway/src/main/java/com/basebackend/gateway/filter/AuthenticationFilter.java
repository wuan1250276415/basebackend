package com.basebackend.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
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

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 白名单路径
     */
    private static final List<String> WHITELIST = Arrays.asList(
            "/basebackend-demo-api/api/auth/**",
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

        // 从Token中获取用户信息并添加到请求头
        String subject = jwtUtil.getSubjectFromToken(token);
        log.debug("Token验证成功，用户: {}", subject);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", subject)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
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
        String bearerToken = request.getHeaders().getFirst(CommonConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        Result<?> result = Result.error(HttpStatus.UNAUTHORIZED.value(), message);
        String body = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
