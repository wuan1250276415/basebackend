package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.RequestSizeLimitProperties;
import com.basebackend.gateway.model.GatewayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

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
        long maxBodyBytes = properties.getMaxBodySize().toBytes();

        if (contentLength > 0 && contentLength > maxBodyBytes) {
            log.warn("Request body too large: path={}, contentLength={}, maxAllowed={}",
                    path, contentLength, maxBodyBytes);
            return payloadTooLargeResponse(exchange.getResponse());
        }

        if (!requiresStreamingCheck(request, contentLength)) {
            return chain.filter(exchange);
        }

        ServerWebExchange limitedExchange = decorateExchangeWithStreamingSizeCheck(exchange, path, maxBodyBytes);
        return chain.filter(limitedExchange)
                .onErrorResume(PayloadTooLargeException.class, ex -> payloadTooLargeResponse(exchange.getResponse()));
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

    private boolean requiresStreamingCheck(ServerHttpRequest request, long contentLength) {
        if (contentLength < 0) {
            return true;
        }
        return request.getHeaders()
                .getOrEmpty(HttpHeaders.TRANSFER_ENCODING)
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("chunked"));
    }

    private ServerWebExchange decorateExchangeWithStreamingSizeCheck(ServerWebExchange exchange,
                                                                     String path,
                                                                     long maxBodyBytes) {
        AtomicLong totalBytes = new AtomicLong(0);
        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody()
                        .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                        .handle((dataBuffer, sink) -> {
                            long receivedBytes = totalBytes.addAndGet(dataBuffer.readableByteCount());
                            if (receivedBytes > maxBodyBytes) {
                                log.warn("Request body too large (streaming): path={}, received={}, maxAllowed={}",
                                        path, receivedBytes, maxBodyBytes);
                                DataBufferUtils.release(dataBuffer);
                                sink.error(new PayloadTooLargeException());
                                return;
                            }
                            sink.next(dataBuffer);
                        });
            }
        };
        return exchange.mutate().request(decoratedRequest).build();
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

    private static final class PayloadTooLargeException extends RuntimeException {
    }
}
