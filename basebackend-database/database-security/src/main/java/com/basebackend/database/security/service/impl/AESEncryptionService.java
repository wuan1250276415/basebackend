package com.basebackend.database.security.service.impl;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.EncryptionException;
import com.basebackend.database.security.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES加密服务实现
 * 使用AES算法进行数据加密和解密
 */
@Slf4j
@Service
public class AESEncryptionService implements EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String LEGACY_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ENCRYPTED_PREFIX = "ENC:";
    private static final String V2_PREFIX = "v2:";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    
    private final SecretKey secretKey;
    private final SecretKey legacySecretKey;
    private final SecretKey sha1PrngLegacySecretKey;
    private final boolean enabled;
    private final SecureRandom secureRandom = new SecureRandom();
    
    public AESEncryptionService(DatabaseEnhancedProperties properties) {
        this.enabled = properties.getSecurity().getEncryption().isEnabled();
        
        if (enabled) {
            String secretKeyStr = properties.getSecurity().getEncryption().getSecretKey();
            if (!StringUtils.hasText(secretKeyStr)) {
                throw new EncryptionException("Encryption is enabled but no secret key is configured");
            }
            this.secretKey = generateSecretKey(secretKeyStr);
            this.legacySecretKey = generateLegacySecretKey(secretKeyStr);
            this.sha1PrngLegacySecretKey = generateSha1PrngLegacySecretKey(secretKeyStr);
        } else {
            this.secretKey = null;
            this.legacySecretKey = null;
            this.sha1PrngLegacySecretKey = null;
        }
    }
    
    /**
     * 从字符串生成密钥
     */
    private SecretKey generateSecretKey(String keyStr) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(keyStr.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = Arrays.copyOf(digest, 32);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to generate secret key", e);
        }
    }

    /**
     * B3: Derive the legacy AES key using PBKDF2WithHmacSHA256 with a fixed salt.
     * <p>
     * This is the stable legacy path used by recently written ECB ciphertext.
     * The fixed salt value must never be changed after initial deployment.
     */
    private SecretKey generateLegacySecretKey(String keyStr) {
        try {
            // Fixed salt — changing this invalidates all existing legacy ciphertext
            byte[] salt = "basebackend-legacy-v1-salt".getBytes(StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(keyStr.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new EncryptionException("Failed to generate legacy secret key", e);
        }
    }

    /**
     * 兼容更早期的 SHA1PRNG 派生方式，用于解密历史遗留 ECB 密文。
     */
    private SecretKey generateSha1PrngLegacySecretKey(String keyStr) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(keyStr.getBytes(StandardCharsets.UTF_8));
            keyGenerator.init(128, secureRandom);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new EncryptionException("Failed to generate SHA1PRNG legacy secret key", e);
        }
    }
    
    @Override
    public String encrypt(String plainText) {
        if (!enabled) {
            return plainText;
        }
        
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        
        // 如果已经加密，直接返回
        if (isEncrypted(plainText)) {
            return plainText;
        }
        
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, payload, iv.length, encryptedBytes.length);

            String encrypted = Base64.getEncoder().encodeToString(payload);
            return ENCRYPTED_PREFIX + V2_PREFIX + encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }
    
    @Override
    public String decrypt(String cipherText) {
        if (!enabled) {
            return cipherText;
        }
        
        if (!StringUtils.hasText(cipherText)) {
            return cipherText;
        }
        
        if (!cipherText.startsWith(ENCRYPTED_PREFIX)) {
            return cipherText;
        }
        
        try {
            String encryptedData = cipherText.substring(ENCRYPTED_PREFIX.length());

            if (encryptedData.startsWith(V2_PREFIX)) {
                return decryptV2(encryptedData.substring(V2_PREFIX.length()));
            }

            return decryptLegacy(encryptedData);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
    
    @Override
    public boolean isEncrypted(String text) {
        if (!StringUtils.hasText(text) || !text.startsWith(ENCRYPTED_PREFIX)) {
            return false;
        }

        String encryptedData = text.substring(ENCRYPTED_PREFIX.length());
        if (!StringUtils.hasText(encryptedData)) {
            return false;
        }

        if (encryptedData.startsWith(V2_PREFIX)) {
            String payload = encryptedData.substring(V2_PREFIX.length());
            return isValidV2Payload(payload);
        }

        return isValidBase64(encryptedData);
    }

    private String decryptV2(String encodedPayload) throws Exception {
        byte[] payload = Base64.getDecoder().decode(encodedPayload);
        if (payload.length <= GCM_IV_LENGTH_BYTES) {
            throw new EncryptionException("Invalid encrypted payload format");
        }

        byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
        byte[] encryptedBytes = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private String decryptLegacy(String encodedPayload) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encodedPayload);

        try {
            Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, legacySecretKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception primaryFailure) {
            if (sha1PrngLegacySecretKey == null) {
                throw primaryFailure;
            }

            Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, sha1PrngLegacySecretKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }
    }

    private boolean isValidV2Payload(String encodedPayload) {
        if (!StringUtils.hasText(encodedPayload)) {
            return false;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            return payload.length > GCM_IV_LENGTH_BYTES;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidBase64(String encodedPayload) {
        if (!StringUtils.hasText(encodedPayload)) {
            return false;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            return payload.length > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
