package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.AccessLogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogFilter implements GlobalFilter, Ordered {

    private final AccessLogProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        long startNano = System.nanoTime();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
                    log.info("{}", buildAccessLog(exchange, latencyMs));
                });
    }

    private String buildAccessLog(ServerWebExchange exchange, long latencyMs) {
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getPath().value();
        int status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value() : 0;

        String traceId = request.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        String userId = request.getHeaders().getFirst("X-User-Id");
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String routeId = resolveRouteId(exchange);
        String timestamp = Instant.now().toString();

        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"type\":\"ACCESS\"");
        appendField(sb, "traceId", traceId);
        appendField(sb, "method", method);
        appendField(sb, "path", path);
        sb.append(",\"status\":").append(status);
        sb.append(",\"latencyMs\":").append(latencyMs);
        appendField(sb, "userId", userId);
        appendField(sb, "clientIp", clientIp);
        appendField(sb, "userAgent", userAgent);
        appendField(sb, "routeId", routeId);
        appendField(sb, "timestamp", timestamp);
        sb.append('}');
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value) {
        sb.append(",\"").append(key).append("\":\"")
                .append(value != null ? escapeJson(value) : "").append('"');
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private String resolveRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "";
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
