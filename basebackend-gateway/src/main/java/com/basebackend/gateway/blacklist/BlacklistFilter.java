package com.basebackend.gateway.blacklist;

import com.basebackend.gateway.util.IpAddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

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

    private static final String UNKNOWN = "unknown";

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
        String remoteIp = resolveRemoteIp(request);
        if (!isFromTrustedProxy(remoteIp)) {
            return remoteIp;
        }

        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        String forwardedIp = extractHeaderIp(xForwardedFor);
        if (StringUtils.hasText(forwardedIp)) {
            return forwardedIp;
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        String realIp = normalizeIp(xRealIp);
        return StringUtils.hasText(realIp) ? realIp : remoteIp;
    }

    private boolean isFromTrustedProxy(String remoteIp) {
        if (!StringUtils.hasText(remoteIp) || UNKNOWN.equalsIgnoreCase(remoteIp)) {
            return false;
        }

        List<String> trustedProxyCidrs = blacklistManager.getTrustedProxyCidrs();
        if (trustedProxyCidrs == null || trustedProxyCidrs.isEmpty()) {
            return false;
        }

        return trustedProxyCidrs.stream()
                .filter(StringUtils::hasText)
                .anyMatch(cidr -> IpAddressUtil.matchesCidr(remoteIp, cidr.trim()));
    }

    private String resolveRemoteIp(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            return UNKNOWN;
        }

        if (remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        String hostString = remoteAddress.getHostString();
        return StringUtils.hasText(hostString) ? IpAddressUtil.stripPort(hostString) : UNKNOWN;
    }

    private String extractHeaderIp(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }

        String firstIp = headerValue.contains(",")
                ? headerValue.substring(0, headerValue.indexOf(','))
                : headerValue;
        return normalizeIp(firstIp);
    }

    private String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }

        String trimmed = ip.trim();
        if (UNKNOWN.equalsIgnoreCase(trimmed)) {
            return null;
        }

        return IpAddressUtil.stripPort(trimmed);
    }
}
