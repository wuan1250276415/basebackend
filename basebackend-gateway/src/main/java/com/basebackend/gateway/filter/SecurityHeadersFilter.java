package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.SecurityHeadersProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    private final SecurityHeadersProperties headersProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!headersProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = exchange.getResponse().getHeaders();

        String hstsValue = "max-age=" + headersProperties.getHstsMaxAge();
        if (headersProperties.isHstsIncludeSubdomains()) {
            hstsValue += "; includeSubDomains";
        }
        headers.set("Strict-Transport-Security", hstsValue);

        headers.set("Content-Security-Policy", headersProperties.getContentSecurityPolicy());
        headers.set("X-Frame-Options", headersProperties.getFrameOptions());

        if (headersProperties.isContentTypeOptions()) {
            headers.set("X-Content-Type-Options", "nosniff");
        }

        headers.set("Referrer-Policy", headersProperties.getReferrerPolicy());

        if (StringUtils.hasText(headersProperties.getPermissionsPolicy())) {
            headers.set("Permissions-Policy", headersProperties.getPermissionsPolicy());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -190;
    }
}
