package com.basebackend.messaging.encryption;

import com.basebackend.messaging.config.MessagingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * AES-GCM消息加密服务实现
 * <p>
 * 使用AES-256-GCM模式进行消息加密，提供：
 * - 机密性保护
 * - 完整性验证
 * - 认证加密
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "messaging.encryption", name = "enabled", havingValue = "true")
public class AesGcmMessageEncryptor implements MessageEncryptor {

    private final SecretKey secretKey;
    private final List<String> encryptTopics;
    private final SecureRandom secureRandom;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    public AesGcmMessageEncryptor(MessagingProperties properties) {
        MessagingProperties.Encryption encryption = properties.getEncryption();

        // 初始化密钥
        if (encryption.getSecretKey() != null && !encryption.getSecretKey().isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(encryption.getSecretKey());
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            log.info("MessageEncryptor initialized with provided secret key");
        } else {
            // 生成随机密钥（仅用于开发，生产环境必须配置密钥）
            this.secretKey = generateRandomKey();
            log.warn(
                    "MessageEncryptor using generated random key. Configure 'messaging.encryption.secret-key' for production!");
        }

        this.encryptTopics = encryption.getEncryptTopics();
        this.secureRandom = new SecureRandom();

        log.info("AES-GCM MessageEncryptor initialized, encrypt topics: {}",
                encryptTopics.isEmpty() ? "ALL" : encryptTopics);
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

            // 加密
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            // 组合IV和密文
            byte[] combined = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, GCM_IV_LENGTH, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Message encryption failed", e);
            throw new RuntimeException("消息加密失败", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 分离IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            // 初始化Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);

            // 解密
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Message decryption failed", e);
            throw new RuntimeException("消息解密失败", e);
        }
    }

    @Override
    public boolean shouldEncrypt(String topic) {
        // 如果没有指定特定Topic，则加密所有消息
        if (encryptTopics == null || encryptTopics.isEmpty()) {
            return true;
        }
        return encryptTopics.contains(topic);
    }

    @Override
    public String getAlgorithm() {
        return TRANSFORMATION;
    }

    /**
     * 生成随机AES密钥
     */
    private SecretKey generateRandomKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256, new SecureRandom());
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    /**
     * 生成密钥字符串（用于配置）
     *
     * @return Base64编码的密钥
     */
    public static String generateKeyString() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }
}
