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
    void sanitizeShouldStripDangerousAttributesAndProtocols() {
        String malicious = "<a href=\"javascript:alert(1)\" onclick=\"steal()\">click</a>";

        String sanitized = SanitizationUtils.sanitize(malicious);

        assertThat(sanitized)
                .contains("click")
                .doesNotContain("javascript:")
                .doesNotContain("onclick=");
    }

    @Test
    void containsUnsafeContentDetectsDangerousTags() {
        assertThat(SanitizationUtils.containsUnsafeContent("<script>alert(1)</script>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<iframe src='x'></iframe>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<object data='x'></object>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<embed src='x' />")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<svg><circle /></svg>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<math><mi>x</mi></math>")).isTrue();
    }

    @Test
    void containsUnsafeContentDetectsEventHandlersAndProtocols() {
        assertThat(SanitizationUtils.containsUnsafeContent("<img src='x' onerror='alert(1)' />")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<div onclick = 'run()'>x</div>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<a href='javascript:alert(1)'>x</a>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<a href='vbscript:msgbox(1)'>x</a>")).isTrue();
        assertThat(SanitizationUtils.containsUnsafeContent("<img src='data:text/html;base64,xxx' />")).isTrue();
    }

    @Test
    void containsUnsafeContentIgnoresSafeInputs() {
        assertThat(SanitizationUtils.containsUnsafeContent("normal text"))
                .isFalse();
        assertThat(SanitizationUtils.containsUnsafeContent("<p>safe <a href='https://example.com'>link</a></p>"))
                .isFalse();
        assertThat(SanitizationUtils.containsUnsafeContent("<strong>safe</strong>"))
                .isFalse();
        assertThat(SanitizationUtils.containsUnsafeContent("data center information"))
                .isFalse();
    }
}
