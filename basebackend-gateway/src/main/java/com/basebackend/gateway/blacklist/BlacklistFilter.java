package com.basebackend.gateway.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 黑白名单全局过滤器
 * <p>
 * 在请求入口检查 IP 和路径是否被封禁，被封禁的请求直接返回 403。
 * 优先级极高（Ordered.HIGHEST_PRECEDENCE + 5），在所有业务 Filter 之前执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistFilter implements GlobalFilter, Ordered {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"
    };

    private final BlacklistManager blacklistManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!blacklistManager.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String ip = resolveClientIp(request);
        String path = request.getURI().getPath();

        // IP 黑名单检查
        if (blacklistManager.isIpDenied(ip)) {
            String reason = blacklistManager.getBanReason(ip);
            log.warn("IP 被封禁, ip={}, path={}, reason={}", ip, path, reason);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 路径黑名单检查
        if (blacklistManager.isPathDenied(path)) {
            log.warn("路径被封禁, ip={}, path={}", ip, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        for (String header : IP_HEADERS) {
            String ip = headers.getFirst(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
