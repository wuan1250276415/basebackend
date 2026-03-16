package com.basebackend.gateway.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistFilter 单元测试")
class BlacklistFilterTest {

    @Mock
    private GatewayFilterChain filterChain;

    private BlacklistManager blacklistManager;
    private BlacklistFilter blacklistFilter;

    @BeforeEach
    void setUp() {
        blacklistManager = new BlacklistManager();
        blacklistManager.setEnabled(true);
        blacklistFilter = new BlacklistFilter(blacklistManager);
    }

    @Test
    @DisplayName("非可信来源伪造 X-Forwarded-For 不生效")
    void forgedXForwardedForFromUntrustedSourceShouldNotTakeEffect() {
        blacklistManager.getDeniedIps().add("203.0.113.100");
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("X-Forwarded-For", "203.0.113.100")
                .remoteAddress(new InetSocketAddress("10.10.10.10", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(blacklistFilter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verify(filterChain).filter(any());
    }

    @Test
    @DisplayName("可信来源下 X-Forwarded-For 生效")
    void xForwardedForFromTrustedSourceShouldTakeEffect() {
        blacklistManager.getDeniedIps().add("203.0.113.100");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("X-Forwarded-For", "203.0.113.100")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(blacklistFilter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(filterChain, never()).filter(any());
    }
}
