package com.basebackend.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignatureVerifyFilter 单元测试")
class SignatureVerifyFilterTest {

    private static final String SECRET_KEY = "gateway-signature-test-secret";
    private static final String APP_ID = "mall-client";
    private static final String TIMESTAMP = "1700000000000";
    private static final String NONCE = "nonce-1";

    @Mock
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    @Mock
    private GatewayFilterChain filterChain;

    private SignatureVerifyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SignatureVerifyFilter(reactiveRedisTemplate);
        ReflectionTestUtils.setField(filter, "signatureEnabled", true);
        ReflectionTestUtils.setField(filter, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(filter, "timestampValidity", Long.MAX_VALUE);
        ReflectionTestUtils.setField(filter, "signaturePaths", List.of("/api/protected/**"));
        ReflectionTestUtils.setField(filter, "signatureWhitelist", List.of());

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(reactiveRedisTemplate.delete(anyString())).thenReturn(Mono.just(1L));
        lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("同一 query 不同顺序应得到相同验签结果")
    void shouldAllowCanonicalizedQueryInDifferentOrder() {
        String canonicalQuery = "a=1&b=2";
        String signature = calculateSignature("GET", "/api/protected/orders", canonicalQuery,
                sha256Hex(new byte[0]));

        MockServerHttpRequest firstRequest = buildGetRequest("/api/protected/orders?b=2&a=1", signature);
        MockServerHttpRequest secondRequest = buildGetRequest("/api/protected/orders?a=1&b=2", signature);

        StepVerifier.create(filter.filter(MockServerWebExchange.from(firstRequest), filterChain))
                .verifyComplete();
        StepVerifier.create(filter.filter(MockServerWebExchange.from(secondRequest), filterChain))
                .verifyComplete();

        verify(filterChain, times(2)).filter(any());
    }

    @Test
    @DisplayName("query 被篡改时应拒绝请求")
    void shouldRejectTamperedQuery() {
        String signature = calculateSignature("GET", "/api/protected/orders", "a=1",
                sha256Hex(new byte[0]));
        MockServerHttpRequest request = buildGetRequest("/api/protected/orders?a=2", signature);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
        verify(reactiveRedisTemplate).delete("gateway:nonce:" + NONCE);
    }

    @Test
    @DisplayName("body 被篡改时应拒绝请求")
    void shouldRejectTamperedBody() {
        String actualBody = "{\"amount\":200}";
        String signature = calculateSignature("POST", "/api/protected/orders", "",
                sha256Hex("{\"amount\":100}".getBytes(StandardCharsets.UTF_8)));
        MockServerHttpRequest request = buildJsonRequest("/api/protected/orders", signature, actualBody);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("验签通过后应保留请求体供下游继续读取")
    void shouldForwardBodyAfterSuccessfulVerification() {
        String body = "{\"orderId\":1001,\"amount\":99}";
        String bodyDigest = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String signature = calculateSignature("POST", "/api/protected/orders", "", bodyDigest);
        MockServerHttpRequest request = buildJsonRequest("/api/protected/orders", signature, body);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> forwardedBody = new AtomicReference<>();

        GatewayFilterChain captureBodyChain = forwardedExchange -> org.springframework.core.io.buffer.DataBufferUtils
                .join(forwardedExchange.getRequest().getBody())
                .doOnNext(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                    forwardedBody.set(new String(bytes, StandardCharsets.UTF_8));
                })
                .then();

        StepVerifier.create(filter.filter(exchange, captureBodyChain))
                .verifyComplete();

        assertNotNull(forwardedBody.get());
        assertEquals(body, forwardedBody.get());
    }

    private MockServerHttpRequest buildGetRequest(String uri, String signature) {
        return MockServerHttpRequest.get(uri)
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", TIMESTAMP)
                .header("X-Nonce", NONCE)
                .header("X-Signature", signature)
                .build();
    }

    private MockServerHttpRequest buildJsonRequest(String uri, String signature, String body) {
        return MockServerHttpRequest.post(uri)
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", TIMESTAMP)
                .header("X-Nonce", NONCE)
                .header("X-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String calculateSignature(String method, String path, String canonicalQuery, String bodyDigest) {
        String data = String.join("|", APP_ID, TIMESTAMP, NONCE, method, path, canonicalQuery, bodyDigest);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("签名计算失败", exception);
        }
    }

    private String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("摘要计算失败", exception);
        }
    }
}
