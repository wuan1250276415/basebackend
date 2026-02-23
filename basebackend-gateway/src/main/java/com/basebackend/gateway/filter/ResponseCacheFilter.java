package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.ResponseCacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class ResponseCacheFilter implements GlobalFilter, Ordered {

    private final ResponseCacheProperties properties;
    private final Cache<String, CachedResponse> cache;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ResponseCacheFilter(ResponseCacheProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxCacheSize())
                .expireAfterWrite(properties.getDefaultTtl())
                .build();
        log.info("ResponseCacheFilter initialized - enabled={}, ttl={}, maxSize={}",
                properties.isEnabled(), properties.getDefaultTtl(), properties.getMaxCacheSize());
    }

    record CachedResponse(HttpStatusCode status, HttpHeaders headers, byte[] body) {}

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() != HttpMethod.GET) {
            return chain.filter(exchange);
        }

        String cacheControl = request.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        if (cacheControl != null && (cacheControl.contains("no-cache") || cacheControl.contains("no-store"))) {
            return chain.filter(exchange);
        }

        String path = request.getPath().toString();
        if (!matchesAny(path, properties.getCachePaths()) || matchesAny(path, properties.getExcludePaths())) {
            return chain.filter(exchange);
        }

        String cacheKey = buildCacheKey(request);
        CachedResponse cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return writeCachedResponse(exchange, cached);
        }

        return chain.filter(exchange.mutate()
                .response(new CachingResponseDecorator(exchange.getResponse(), cacheKey))
                .build());
    }

    private boolean matchesAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String buildCacheKey(ServerHttpRequest request) {
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";
        String path = request.getPath().toString();
        String query = request.getURI().getRawQuery();
        return query != null ? method + ":" + path + "?" + query : method + ":" + path;
    }

    private Mono<Void> writeCachedResponse(ServerWebExchange exchange, CachedResponse cached) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(cached.status());
        cached.headers().forEach((name, values) -> {
            if (!name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                response.getHeaders().put(name, values);
            }
        });
        response.getHeaders().set("X-Cache", "HIT");
        response.getHeaders().setContentLength(cached.body().length);

        DataBuffer buffer = response.bufferFactory().wrap(cached.body());
        return response.writeWith(Mono.just(buffer));
    }

    public void evictAll() {
        cache.invalidateAll();
        log.info("Response cache evicted");
    }

    @Override
    public int getOrder() {
        return -30;
    }

    private class CachingResponseDecorator extends ServerHttpResponseDecorator {

        private final String cacheKey;

        CachingResponseDecorator(ServerHttpResponse delegate, String cacheKey) {
            super(delegate);
            this.cacheKey = cacheKey;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            getDelegate().getHeaders().set("X-Cache", "MISS");

            return DataBufferUtils.join(Flux.from(body))
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        HttpHeaders cachedHeaders = new HttpHeaders();
                        cachedHeaders.putAll(getDelegate().getHeaders());
                        cache.put(cacheKey, new CachedResponse(
                                getDelegate().getStatusCode(), cachedHeaders, bytes));

                        DataBufferFactory factory = getDelegate().bufferFactory();
                        return getDelegate().writeWith(Mono.just(factory.wrap(bytes)));
                    });
        }
    }
}
