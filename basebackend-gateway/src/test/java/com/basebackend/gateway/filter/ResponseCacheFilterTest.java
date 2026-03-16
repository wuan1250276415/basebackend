package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.ResponseCacheProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResponseCacheFilter 单元测试")
class ResponseCacheFilterTest {

    @Test
    @DisplayName("Vary Accept-Language 下同语言请求应命中缓存")
    void shouldHitCacheForSameLanguageWhenVaryAcceptLanguage() {
        ResponseCacheFilter filter = createFilter();
        AtomicInteger downstreamCalls = new AtomicInteger();
        GatewayFilterChain chain = buildChain(downstreamCalls, (invocation, exchange) -> {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
            return new DownstreamReply(headers, "payload-" + invocation);
        });

        MockServerWebExchange firstExchange = createExchange("zh-CN");
        StepVerifier.create(filter.filter(firstExchange, chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(1);
        assertThat(firstExchange.getResponse().getHeaders().getFirst("X-Cache")).isEqualTo("MISS");

        MockServerWebExchange secondExchange = createExchange("zh-CN");
        StepVerifier.create(filter.filter(secondExchange, chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(1);
        assertThat(secondExchange.getResponse().getHeaders().getFirst("X-Cache")).isEqualTo("HIT");
    }

    @Test
    @DisplayName("Vary Accept-Language 下不同语言不应共享缓存")
    void shouldNotShareCacheAcrossDifferentLanguages() {
        ResponseCacheFilter filter = createFilter();
        AtomicInteger downstreamCalls = new AtomicInteger();
        GatewayFilterChain chain = buildChain(downstreamCalls, (invocation, exchange) -> {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
            return new DownstreamReply(headers, "payload-" + invocation);
        });

        StepVerifier.create(filter.filter(createExchange("zh-CN"), chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(1);

        MockServerWebExchange differentLanguageExchange = createExchange("en-US");
        StepVerifier.create(filter.filter(differentLanguageExchange, chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(2);
        assertThat(differentLanguageExchange.getResponse().getHeaders().getFirst("X-Cache")).isEqualTo("MISS");

        MockServerWebExchange repeatedEnExchange = createExchange("en-US");
        StepVerifier.create(filter.filter(repeatedEnExchange, chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(2);
        assertThat(repeatedEnExchange.getResponse().getHeaders().getFirst("X-Cache")).isEqualTo("HIT");
    }

    @Test
    @DisplayName("Vary * 响应不应被缓存")
    void shouldNotCacheResponseWhenVaryWildcardPresent() {
        ResponseCacheFilter filter = createFilter();
        AtomicInteger downstreamCalls = new AtomicInteger();
        GatewayFilterChain chain = buildChain(downstreamCalls, (invocation, exchange) -> {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.VARY, "*");
            return new DownstreamReply(headers, "no-cache-" + invocation);
        });

        StepVerifier.create(filter.filter(createExchange("zh-CN"), chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(1);

        MockServerWebExchange secondExchange = createExchange("zh-CN");
        StepVerifier.create(filter.filter(secondExchange, chain)).verifyComplete();
        assertThat(downstreamCalls.get()).isEqualTo(2);
        assertThat(secondExchange.getResponse().getHeaders().getFirst("X-Cache")).isEqualTo("MISS");
    }

    private ResponseCacheFilter createFilter() {
        ResponseCacheProperties properties = new ResponseCacheProperties();
        properties.setEnabled(true);
        properties.setDefaultTtl(Duration.ofMinutes(1));
        properties.setMaxCacheSize(256);
        properties.setMaxCacheableBodyBytes(1024 * 1024);
        properties.setCachePaths(List.of("/api/cache/**"));
        properties.setExcludePaths(List.of());
        return new ResponseCacheFilter(properties);
    }

    private MockServerWebExchange createExchange(String acceptLanguage) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/cache/items")
                .header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage)
                .build();
        return MockServerWebExchange.from(request);
    }

    private GatewayFilterChain buildChain(AtomicInteger callCounter,
                                          BiFunction<Integer, ServerWebExchange, DownstreamReply> replyProvider) {
        return exchange -> {
            int invocation = callCounter.incrementAndGet();
            DownstreamReply reply = replyProvider.apply(invocation, exchange);

            exchange.getResponse().setStatusCode(HttpStatus.OK);
            reply.headers().forEach((name, values) -> exchange.getResponse().getHeaders().put(name, values));

            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }

    private record DownstreamReply(HttpHeaders headers, String body) {
    }
}
