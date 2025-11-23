package com.basebackend.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizationUtilsTest {

    @Test
    void sanitizeShouldRemoveScriptTags() {
        String malicious = "<script>alert('xss')</script>hello";

        String sanitized = SanitizationUtils.sanitize(malicious);

        assertThat(sanitized).doesNotContain("<script").contains("hello");
    }

    @Test
    void sanitizeShouldKeepSafeContent() {
        String safe = "Hello World!";

        String sanitized = SanitizationUtils.sanitize(safe);

        assertThat(sanitized).isEqualTo(safe);
    }

    @Test
    void containsUnsafeContentDetectsPayloads() {
        assertThat(SanitizationUtils.containsUnsafeContent("<script>alert(1)</script>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("normal text"))
                .isFalse();
    }
}
