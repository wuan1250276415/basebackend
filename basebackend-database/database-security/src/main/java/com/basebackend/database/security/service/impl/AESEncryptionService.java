package com.basebackend.database.security.service.impl;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.EncryptionException;
import com.basebackend.database.security.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密服务实现
 * 使用AES算法进行数据加密和解密
 */
@Slf4j
@Service
public class AESEncryptionService implements EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ENCRYPTED_PREFIX = "ENC:";
    
    private final SecretKey secretKey;
    private final boolean enabled;
    
    public AESEncryptionService(DatabaseEnhancedProperties properties) {
        this.enabled = properties.getSecurity().getEncryption().isEnabled();
        
        if (enabled) {
            String secretKeyStr = properties.getSecurity().getEncryption().getSecretKey();
            if (!StringUtils.hasText(secretKeyStr)) {
                log.warn("Encryption is enabled but no secret key is configured. Using default key (NOT SECURE FOR PRODUCTION)");
                secretKeyStr = "default-secret-key-change-me";
            }
            this.secretKey = generateSecretKey(secretKeyStr);
        } else {
            this.secretKey = null;
        }
    }
    
    /**
     * 从字符串生成密钥
     */
    private SecretKey generateSecretKey(String keyStr) {
        try {
            // 使用SHA1PRNG算法生成固定的密钥
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(keyStr.getBytes(StandardCharsets.UTF_8));
            keyGenerator.init(128, secureRandom);
            SecretKey originalKey = keyGenerator.generateKey();
            
            // 转换为可序列化的密钥
            byte[] encodedKey = originalKey.getEncoded();
            return new SecretKeySpec(encodedKey, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to generate secret key", e);
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
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            return ENCRYPTED_PREFIX + encrypted;
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
        
        // 如果没有加密前缀，说明未加密，直接返回
        if (!isEncrypted(cipherText)) {
            return cipherText;
        }
        
        try {
            // 移除加密前缀
            String encryptedData = cipherText.substring(ENCRYPTED_PREFIX.length());
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
    
    @Override
    public boolean isEncrypted(String text) {
        return StringUtils.hasText(text) && text.startsWith(ENCRYPTED_PREFIX);
    }
}
