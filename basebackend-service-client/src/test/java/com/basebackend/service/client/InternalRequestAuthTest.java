package com.basebackend.service.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InternalRequestAuth 测试")
class InternalRequestAuthTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("签名与校验应匹配同一请求")
    void shouldVerifyMatchingRequest() {
        long timestamp = 1_763_456_789_000L;
        String signature = InternalRequestAuth.sign(
                SECRET,
                "basebackend-user-api",
                timestamp,
                "GET",
                "/api/internal/operation-log/user/1"
        );

        boolean verified = InternalRequestAuth.verify(
                SECRET,
                "basebackend-user-api",
                timestamp,
                "GET",
                "/api/internal/operation-log/user/1",
                signature
        );

        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("路径变化时签名校验应失败")
    void shouldRejectTamperedPath() {
        long timestamp = 1_763_456_789_000L;
        String signature = InternalRequestAuth.sign(
                SECRET,
                "basebackend-user-api",
                timestamp,
                "POST",
                "/api/internal/operation-log/save"
        );

        boolean verified = InternalRequestAuth.verify(
                SECRET,
                "basebackend-user-api",
                timestamp,
                "POST",
                "/api/internal/operation-log/user/1",
                signature
        );

        assertThat(verified).isFalse();
    }
}
