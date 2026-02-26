package com.basebackend.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class HeaderSanitizationFilter implements GlobalFilter, Ordered {

    private static final List<String> SANITIZED_HEADERS = List.of(
            "X-User-Id",
            "X-Tenant-Id",
            "X-User-Roles",
            "X-Internal-Call"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder mutatedBuilder = exchange.getRequest().mutate();
        mutatedBuilder.headers(headers -> {
            for (String header : SANITIZED_HEADERS) {
                if (headers.getFirst(header) != null) {
                    log.debug("Sanitized spoofed header: {}", header);
                    headers.remove(header);
                }
            }
        });

        return chain.filter(exchange.mutate().request(mutatedBuilder.build()).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
