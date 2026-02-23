package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.ApiVersionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiVersionFilter implements GlobalFilter, Ordered {

    public static final String API_VERSION_ATTR = "gateway.api-version";

    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("^/v(\\d+)/(.*)$");

    private final ApiVersionProperties properties;

    @Override
    public int getOrder() {
        return -40;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String version = null;

        String headerVersion = request.getHeaders().getFirst(properties.getHeaderName());
        if (StringUtils.hasText(headerVersion)) {
            version = headerVersion;
        }

        if (version == null) {
            String path = request.getURI().getRawPath();
            Matcher matcher = VERSION_PREFIX_PATTERN.matcher(path);
            if (matcher.matches()) {
                version = "v" + matcher.group(1);
                String rewrittenPath = "/" + matcher.group(2);
                ServerHttpRequest mutatedRequest = request.mutate().path(rewrittenPath).build();
                exchange = exchange.mutate().request(mutatedRequest).build();
                log.debug("API version path rewrite: {} -> {}, version={}", path, rewrittenPath, version);
            }
        }

        if (version == null) {
            version = properties.getDefaultVersion();
        }

        exchange.getAttributes().put(API_VERSION_ATTR, version);
        log.debug("API version resolved: {}", version);

        return chain.filter(exchange);
    }
}
