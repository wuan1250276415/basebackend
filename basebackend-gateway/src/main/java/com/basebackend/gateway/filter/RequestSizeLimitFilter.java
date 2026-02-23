package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.RequestSizeLimitProperties;
import com.basebackend.gateway.model.GatewayResult;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestSizeLimitFilter implements GlobalFilter, Ordered {

    private final RequestSizeLimitProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        long contentLength = request.getHeaders().getContentLength();
        if (contentLength > 0 && contentLength > properties.getMaxBodySize().toBytes()) {
            log.warn("Request body too large: path={}, contentLength={}, maxAllowed={}",
                    path, contentLength, properties.getMaxBodySize().toBytes());
            return payloadTooLargeResponse(exchange.getResponse());
        }

        return chain.filter(exchange);
    }

    private boolean isExcluded(String path) {
        if (properties.getExcludePaths() == null) {
            return false;
        }
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> payloadTooLargeResponse(ServerHttpResponse response) {
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        GatewayResult<?> result = new GatewayResult<>(413, "请求体超过最大限制");
        DataBuffer buffer = response.bufferFactory()
                .wrap(result.toJsonString().getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -180;
    }
}
