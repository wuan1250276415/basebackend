package com.basebackend.nacos.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialEncryptionServiceTest {
    private static final String KEY_PROPERTY = "nacos.encryption.key";
    private static final String FAIL_ON_DEFAULT_PROPERTY = "nacos.encryption.fail-on-default-key";
    private static final String ACTIVE_PROFILE_PROPERTY = "spring.profiles.active";
    private static final String DEFAULT_KEY_SEED = "basebackend-nacos-encryption-key";

    private String previousKeyProperty;
    private String previousFailOnDefaultProperty;
    private String previousActiveProfileProperty;

    @BeforeEach
    void setUp() {
        this.previousKeyProperty = System.getProperty(KEY_PROPERTY);
        this.previousFailOnDefaultProperty = System.getProperty(FAIL_ON_DEFAULT_PROPERTY);
        this.previousActiveProfileProperty = System.getProperty(ACTIVE_PROFILE_PROPERTY);

        System.setProperty(KEY_PROPERTY, "test-key-seed");
        System.setProperty(FAIL_ON_DEFAULT_PROPERTY, "false");
        System.clearProperty(ACTIVE_PROFILE_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        restoreProperty(KEY_PROPERTY, this.previousKeyProperty);
        restoreProperty(FAIL_ON_DEFAULT_PROPERTY, this.previousFailOnDefaultProperty);
        restoreProperty(ACTIVE_PROFILE_PROPERTY, this.previousActiveProfileProperty);
    }

    @Test
    void shouldEncryptAndDecryptRoundTrip() {
        CredentialEncryptionService service = new CredentialEncryptionService();

        String encrypted = service.encrypt("nacos-secret");

        assertThat(encrypted).startsWith(CredentialEncryptionService.ENCRYPTED_PREFIX);
        assertThat(service.decrypt(encrypted)).isEqualTo("nacos-secret");
    }

    @Test
    void shouldReturnPlainTextWhenNotEncrypted() {
        CredentialEncryptionService service = new CredentialEncryptionService();

        assertThat(service.decryptIfNeeded("plain-text")).isEqualTo("plain-text");
    }

    @Test
    void shouldRejectDefaultKeyWhenStrictModeEnabled() {
        System.setProperty(KEY_PROPERTY, DEFAULT_KEY_SEED);
        System.setProperty(FAIL_ON_DEFAULT_PROPERTY, "true");

        assertThatThrownBy(CredentialEncryptionService::new)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Default nacos encryption key is forbidden");
    }

    @Test
    void shouldRejectDefaultKeyInProdProfileByDefault() {
        System.setProperty(KEY_PROPERTY, DEFAULT_KEY_SEED);
        System.clearProperty(FAIL_ON_DEFAULT_PROPERTY);
        System.setProperty(ACTIVE_PROFILE_PROPERTY, "prod");

        assertThatThrownBy(CredentialEncryptionService::new)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Default nacos encryption key is forbidden");
    }

    @Test
    void shouldRejectDefaultKeyWhenStrictModeEnabledFromSpringEnvironment() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(KEY_PROPERTY, DEFAULT_KEY_SEED)
            .withProperty(FAIL_ON_DEFAULT_PROPERTY, "true");

        assertThatThrownBy(() -> new CredentialEncryptionService(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Default nacos encryption key is forbidden");
    }

    @Test
    void shouldRejectDefaultKeyWhenProdProfileComesFromSpringEnvironment() {
        System.clearProperty(FAIL_ON_DEFAULT_PROPERTY);
        MockEnvironment environment = new MockEnvironment()
            .withProperty(KEY_PROPERTY, DEFAULT_KEY_SEED);
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> new CredentialEncryptionService(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Default nacos encryption key is forbidden");
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }
}
