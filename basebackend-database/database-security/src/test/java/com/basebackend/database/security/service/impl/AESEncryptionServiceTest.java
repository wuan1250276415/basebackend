package com.basebackend.database.security.service.impl;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AESEncryptionService 测试")
class AESEncryptionServiceTest {

    private static final String SECRET_KEY = "unit-test-secret-key";
    private DatabaseEnhancedProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DatabaseEnhancedProperties();
        properties.getSecurity().getEncryption().setEnabled(true);
        properties.getSecurity().getEncryption().setSecretKey(SECRET_KEY);
    }

    @Test
    @DisplayName("启用加密但未配置密钥时应直接失败")
    void shouldFailWhenSecretKeyMissing() {
        properties.getSecurity().getEncryption().setSecretKey(" ");

        assertThatThrownBy(() -> new AESEncryptionService(properties))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("no secret key");
    }

    @Test
    @DisplayName("应使用 v2 格式完成加密与解密")
    void shouldEncryptAndDecryptUsingV2Format() {
        AESEncryptionService encryptionService = new AESEncryptionService(properties);

        String encrypted = encryptionService.encrypt("hello-world");
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(encrypted).startsWith("ENC:v2:");
        assertThat(encrypted).isNotEqualTo("hello-world");
        assertThat(encryptionService.isEncrypted(encrypted)).isTrue();
        assertThat(decrypted).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("伪造 ENC 前缀不应绕过加密")
    void shouldNotBypassEncryptionWithFakePrefix() {
        AESEncryptionService encryptionService = new AESEncryptionService(properties);
        String fakeCipher = "ENC:not_base64_payload";

        assertThat(encryptionService.isEncrypted(fakeCipher)).isFalse();

        String encrypted = encryptionService.encrypt(fakeCipher);
        assertThat(encrypted).startsWith("ENC:v2:");
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo(fakeCipher);
    }

    @Test
    @DisplayName("应兼容解密历史 ECB 格式密文")
    void shouldDecryptLegacyEcbCiphertext() throws Exception {
        AESEncryptionService encryptionService = new AESEncryptionService(properties);
        String legacyCipher = legacyEncrypt("legacy-data", SECRET_KEY);

        assertThat(encryptionService.isEncrypted(legacyCipher)).isTrue();
        assertThat(encryptionService.decrypt(legacyCipher)).isEqualTo("legacy-data");
    }

    @Test
    @DisplayName("非法 v2 密文应抛出异常")
    void shouldThrowWhenV2PayloadInvalid() {
        AESEncryptionService encryptionService = new AESEncryptionService(properties);

        assertThatThrownBy(() -> encryptionService.decrypt("ENC:v2:not-base64"))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    private String legacyEncrypt(String plainText, String keyStr) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(keyStr.getBytes(StandardCharsets.UTF_8));
        keyGenerator.init(128, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return "ENC:" + Base64.getEncoder().encodeToString(encryptedBytes);
    }
}

