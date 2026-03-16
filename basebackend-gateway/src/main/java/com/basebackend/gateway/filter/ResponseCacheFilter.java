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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class ResponseCacheFilter implements GlobalFilter, Ordered {

    private final ResponseCacheProperties properties;
    private final Cache<String, CachedResponse> cache;
    private final Cache<String, List<String>> varyHeadersByBaseKey;
    private final ConcurrentMap<String, Set<String>> variantKeysByBaseKey = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ResponseCacheFilter(ResponseCacheProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxCacheSize())
                .expireAfterWrite(properties.getDefaultTtl())
                .build();
        this.varyHeadersByBaseKey = Caffeine.newBuilder()
                .maximumSize(properties.getMaxCacheSize())
                .expireAfterWrite(properties.getDefaultTtl())
                .build();
        log.info("ResponseCacheFilter initialized - enabled={}, ttl={}, maxSize={}, maxBodyBytes={}",
                properties.isEnabled(), properties.getDefaultTtl(), properties.getMaxCacheSize(),
                properties.getMaxCacheableBodyBytes());
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

        String baseCacheKey = buildBaseCacheKey(request);
        List<String> varyHeaders = varyHeadersByBaseKey.getIfPresent(baseCacheKey);
        String cacheKey = buildVariantCacheKey(baseCacheKey, request.getHeaders(), varyHeaders);
        CachedResponse cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return writeCachedResponse(exchange, cached);
        }

        return chain.filter(exchange.mutate()
                .response(new CachingResponseDecorator(exchange.getResponse(), baseCacheKey, request.getHeaders()))
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

    private String buildBaseCacheKey(ServerHttpRequest request) {
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";
        String path = request.getPath().toString();
        String query = request.getURI().getRawQuery();
        String requestKey = query != null ? method + ":" + path + "?" + query : method + ":" + path;
        String authorizationKey = hashHeaderValues(request.getHeaders().get(HttpHeaders.AUTHORIZATION));
        String cookieKey = hashHeaderValues(request.getHeaders().get(HttpHeaders.COOKIE));
        return requestKey + "|auth=" + authorizationKey + "|cookie=" + cookieKey;
    }

    private String buildVariantCacheKey(String baseCacheKey, HttpHeaders requestHeaders, List<String> varyHeaders) {
        if (varyHeaders == null || varyHeaders.isEmpty()) {
            return baseCacheKey;
        }

        String varySignature = varyHeaders.stream()
                .map(varyHeader -> varyHeader + "=" + hashHeaderValues(requestHeaders.get(varyHeader)))
                .reduce((left, right) -> left + "|" + right)
                .orElse("none");
        String varyHash = DigestUtils.md5DigestAsHex(varySignature.getBytes(StandardCharsets.UTF_8));
        return baseCacheKey + "|vary=" + varyHash;
    }

    private String hashHeaderValues(List<String> headerValues) {
        if (headerValues == null || headerValues.isEmpty()) {
            return "none";
        }
        String mergedHeaderValue = String.join("\n", headerValues);
        return DigestUtils.md5DigestAsHex(mergedHeaderValue.getBytes(StandardCharsets.UTF_8));
    }

    private boolean shouldSkipResponseCache(HttpHeaders headers) {
        if (headers.getFirst(HttpHeaders.SET_COOKIE) != null) {
            return true;
        }
        return containsCacheControlDirective(headers, "private")
                || containsCacheControlDirective(headers, "no-store");
    }

    private boolean containsCacheControlDirective(HttpHeaders headers, String directive) {
        List<String> cacheControlHeaders = headers.get(HttpHeaders.CACHE_CONTROL);
        if (cacheControlHeaders == null || cacheControlHeaders.isEmpty()) {
            return false;
        }
        String directiveLowerCase = directive.toLowerCase(Locale.ROOT);
        for (String cacheControl : cacheControlHeaders) {
            if (cacheControl == null) {
                continue;
            }
            if (cacheControl.toLowerCase(Locale.ROOT).contains(directiveLowerCase)) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolveVaryHeaders(HttpHeaders responseHeaders) {
        List<String> varyHeaderValues = responseHeaders.get(HttpHeaders.VARY);
        if (varyHeaderValues == null || varyHeaderValues.isEmpty()) {
            return List.of();
        }

        return varyHeaderValues.stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(varyHeader -> !varyHeader.isEmpty())
                .map(varyHeader -> varyHeader.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    private void trackVariantKey(String baseCacheKey, String variantCacheKey) {
        variantKeysByBaseKey.computeIfAbsent(baseCacheKey, key -> ConcurrentHashMap.newKeySet())
                .add(variantCacheKey);
    }

    private void invalidateVariantKeys(String baseCacheKey) {
        Set<String> variantKeys = variantKeysByBaseKey.remove(baseCacheKey);
        if (variantKeys != null && !variantKeys.isEmpty()) {
            cache.invalidateAll(variantKeys);
        }
    }

    private void purgeBaseCache(String baseCacheKey) {
        varyHeadersByBaseKey.invalidate(baseCacheKey);
        invalidateVariantKeys(baseCacheKey);
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
        varyHeadersByBaseKey.invalidateAll();
        variantKeysByBaseKey.clear();
        log.info("Response cache evicted");
    }

    @Override
    public int getOrder() {
        return -30;
    }

    private class CachingResponseDecorator extends ServerHttpResponseDecorator {

        private final String baseCacheKey;
        private final HttpHeaders requestHeaders;

        CachingResponseDecorator(ServerHttpResponse delegate, String baseCacheKey, HttpHeaders requestHeaders) {
            super(delegate);
            this.baseCacheKey = baseCacheKey;
            this.requestHeaders = HttpHeaders.readOnlyHttpHeaders(requestHeaders);
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            getDelegate().getHeaders().set("X-Cache", "MISS");

            long maxCacheableBodyBytes = properties.getMaxCacheableBodyBytes();
            AtomicBoolean cacheable = new AtomicBoolean(maxCacheableBodyBytes > 0
                    && !shouldSkipResponseCache(getDelegate().getHeaders()));
            AtomicLong cachedBodySize = new AtomicLong(0L);
            ByteArrayOutputStream cachedBodyStream = new ByteArrayOutputStream();

            Flux<? extends DataBuffer> responseBody = Flux.from(body)
                    .doOnNext(dataBuffer -> {
                        if (!cacheable.get()) {
                            return;
                        }

                        int readableBytes = dataBuffer.readableByteCount();
                        long totalSize = cachedBodySize.addAndGet(readableBytes);
                        if (totalSize > maxCacheableBodyBytes) {
                            cacheable.set(false);
                            cachedBodyStream.reset();
                            return;
                        }

                        ByteBuffer byteBuffer = dataBuffer.asByteBuffer().asReadOnlyBuffer();
                        byte[] chunk = new byte[byteBuffer.remaining()];
                        byteBuffer.get(chunk);
                        cachedBodyStream.write(chunk, 0, chunk.length);
                    });

            return getDelegate().writeWith(responseBody)
                    .doOnSuccess(unused -> {
                        if (!cacheable.get()) {
                            return;
                        }
                        HttpHeaders responseHeaders = getDelegate().getHeaders();
                        if (shouldSkipResponseCache(responseHeaders)) {
                            return;
                        }

                        List<String> varyHeaders = resolveVaryHeaders(responseHeaders);
                        if (varyHeaders.contains("*")) {
                            purgeBaseCache(baseCacheKey);
                            return;
                        }

                        List<String> previousVaryHeaders = varyHeadersByBaseKey.getIfPresent(baseCacheKey);
                        if (!Objects.equals(previousVaryHeaders, varyHeaders)) {
                            invalidateVariantKeys(baseCacheKey);
                        }

                        if (varyHeaders.isEmpty()) {
                            varyHeadersByBaseKey.invalidate(baseCacheKey);
                        } else {
                            varyHeadersByBaseKey.put(baseCacheKey, varyHeaders);
                        }

                        String finalCacheKey = buildVariantCacheKey(baseCacheKey, requestHeaders, varyHeaders);
                        HttpHeaders cachedHeaders = new HttpHeaders();
                        cachedHeaders.putAll(responseHeaders);
                        HttpStatusCode statusCode = getDelegate().getStatusCode();
                        if (statusCode == null) {
                            statusCode = HttpStatus.OK;
                        }
                        cache.put(finalCacheKey, new CachedResponse(
                                statusCode, cachedHeaders, cachedBodyStream.toByteArray()));
                        trackVariantKey(baseCacheKey, finalCacheKey);
                    });
        }
    }
}
