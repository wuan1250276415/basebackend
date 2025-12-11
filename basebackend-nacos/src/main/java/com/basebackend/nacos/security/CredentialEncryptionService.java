package com.basebackend.nacos.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 凭证加密服务
 * <p>
 * 提供敏感信息（如密码）的加密和解密功能。
 * 使用AES-256-GCM算法，支持加密前缀识别。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class CredentialEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    /** 加密标识前缀 */
    public static final String ENCRYPTED_PREFIX = "ENC(";
    public static final String ENCRYPTED_SUFFIX = ")";

    /** 默认加密密钥种子（生产环境应使用环境变量） */
    private static final String DEFAULT_KEY_SEED = "basebackend-nacos-encryption-key";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public CredentialEncryptionService() {
        // 从环境变量获取密钥种子，如果没有则使用默认值
        String keySeed = System.getenv("NACOS_ENCRYPTION_KEY");
        if (keySeed == null || keySeed.isEmpty()) {
            keySeed = System.getProperty("nacos.encryption.key", DEFAULT_KEY_SEED);
            log.warn(
                    "Using default encryption key seed. Set NACOS_ENCRYPTION_KEY environment variable for production!");
        }

        this.secretKey = deriveKey(keySeed);
        this.secureRandom = new SecureRandom();
        log.info("CredentialEncryptionService initialized");
    }

    /**
     * 加密敏感信息
     *
     * @param plainText 明文
     * @return 加密后的密文（带前缀）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        // 如果已经加密，直接返回
        if (isEncrypted(plainText)) {
            return plainText;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合IV和密文
            byte[] combined = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, GCM_IV_LENGTH, cipherBytes.length);

            String encrypted = Base64.getEncoder().encodeToString(combined);
            return ENCRYPTED_PREFIX + encrypted + ENCRYPTED_SUFFIX;
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密敏感信息
     *
     * @param cipherText 密文（带前缀）
     * @return 解密后的明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        // 如果不是加密的,直接返回
        if (!isEncrypted(cipherText)) {
            return cipherText;
        }

        try {
            // 移除前缀和后缀
            String encrypted = cipherText.substring(
                    ENCRYPTED_PREFIX.length(),
                    cipherText.length() - ENCRYPTED_SUFFIX.length());

            byte[] combined = Base64.getDecoder().decode(encrypted);

            // 分离IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 判断是否是加密的密文
     *
     * @param text 文本
     * @return true 如果是加密的
     */
    public boolean isEncrypted(String text) {
        return text != null &&
                text.startsWith(ENCRYPTED_PREFIX) &&
                text.endsWith(ENCRYPTED_SUFFIX);
    }

    /**
     * 处理可能加密的值（自动解密）
     *
     * @param value 可能加密的值
     * @return 解密后的值
     */
    public String decryptIfNeeded(String value) {
        if (isEncrypted(value)) {
            return decrypt(value);
        }
        return value;
    }

    /**
     * 从密钥种子派生AES密钥
     */
    private SecretKey deriveKey(String keySeed) {
        try {
            // 使用固定的salt（生产环境应使用随机salt并存储）
            byte[] salt = "nacos-credential-salt".getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(keySeed.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }
}
