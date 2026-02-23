package com.basebackend.nacos.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CredentialEncryptionService 凭证加密服务测试")
class CredentialEncryptionServiceTest {

    @Test
    @DisplayName("加密后解密应返回原文")
    void encrypt_decrypt_roundTrip() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-key-seed");

        String original = "my-secret-password";
        String encrypted = service.encrypt(original);
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
        assertThat(encrypted).startsWith(CredentialEncryptionService.ENCRYPTED_PREFIX);
        assertThat(encrypted).endsWith(CredentialEncryptionService.ENCRYPTED_SUFFIX);
    }

    @Test
    @DisplayName("非加密文本解密应原样返回")
    void decrypt_returnsPlaintext_whenNotEncrypted() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-key-seed");

        String plaintext = "just-a-plain-password";
        String result = service.decrypt(plaintext);

        assertThat(result).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("未配置密钥时应使用随机密钥且不崩溃")
    void constructor_usesRandomKey_whenNoKeyConfigured() {
        assertThatCode(() -> new CredentialEncryptionService(""))
                .doesNotThrowAnyException();

        assertThatCode(() -> new CredentialEncryptionService(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("isEncrypted 应正确识别 ENC(...) 格式")
    void isEncrypted_detectsPrefix() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-key-seed");

        assertThat(service.isEncrypted("ENC(abc123)")).isTrue();
        assertThat(service.isEncrypted("plain-text")).isFalse();
        assertThat(service.isEncrypted(null)).isFalse();
        assertThat(service.isEncrypted("")).isFalse();
        assertThat(service.isEncrypted("ENC(")).isFalse();
    }

    @Test
    @DisplayName("decryptIfNeeded 对加密值解密，对普通值原样返回")
    void decryptIfNeeded_handlesEncryptedAndPlain() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-key-seed");

        String plain = "not-encrypted";
        assertThat(service.decryptIfNeeded(plain)).isEqualTo(plain);

        String original = "secret";
        String encrypted = service.encrypt(original);
        assertThat(service.decryptIfNeeded(encrypted)).isEqualTo(original);
    }

    @Test
    @DisplayName("加密 null 或空字符串应原样返回")
    void encrypt_nullOrEmpty_returnsAsIs() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-key-seed");

        assertThat(service.encrypt(null)).isNull();
        assertThat(service.encrypt("")).isEmpty();
    }

    @Test
    @DisplayName("已加密文本再次加密应原样返回")
    void encrypt_alreadyEncrypted_returnsAsIs() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-key-seed");

        String encrypted = service.encrypt("password");
        String doubleEncrypted = service.encrypt(encrypted);

        assertThat(doubleEncrypted).isEqualTo(encrypted);
    }

    @Test
    @DisplayName("相同密钥种子应产生一致的加解密结果")
    void sameKeySeed_producesConsistentResults() {
        CredentialEncryptionService service1 = new CredentialEncryptionService("same-seed");
        CredentialEncryptionService service2 = new CredentialEncryptionService("same-seed");

        String encrypted = service1.encrypt("test-value");
        String decrypted = service2.decrypt(encrypted);

        assertThat(decrypted).isEqualTo("test-value");
    }
}
