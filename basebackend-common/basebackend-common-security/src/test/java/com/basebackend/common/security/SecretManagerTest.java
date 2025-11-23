package com.basebackend.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretManagerTest {

    @Test
    void shouldReturnSecretFromEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("jwt.secret", "env-secret");
        SecretManagerProperties properties = new SecretManagerProperties();
        properties.setCacheTtl(Duration.ofMinutes(5));

        SecretManager secretManager = new SecretManager(environment, properties);

        assertThat(secretManager.getRequiredSecret("jwt.secret")).isEqualTo("env-secret");
    }

    @Test
    void shouldUseFallbackWhenMissing() {
        MockEnvironment environment = new MockEnvironment();
        SecretManagerProperties properties = new SecretManagerProperties();
        properties.setCacheTtl(Duration.ofMinutes(5));

        SecretManager secretManager = new SecretManager(environment, properties);

        String secret = secretManager.getRequiredSecret("custom.secret", () -> "fallback-value");

        assertThat(secret).isEqualTo("fallback-value");
    }

    @Test
    void shouldThrowWhenMissingAndNoFallback() {
        MockEnvironment environment = new MockEnvironment();
        SecretManagerProperties properties = new SecretManagerProperties();
        properties.setCacheTtl(Duration.ofMinutes(5));

        SecretManager secretManager = new SecretManager(environment, properties);

        assertThatThrownBy(() -> secretManager.getRequiredSecret("missing.secret"))
                .isInstanceOf(IllegalStateException.class);
    }
}
