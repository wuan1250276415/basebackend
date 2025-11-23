package com.basebackend.logging.audit.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM 加密器
 *
 * 使用 AES-256-GCM 模式对审计日志进行加密存储，
 * 提供机密性和完整性保护。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class AesEncryptor {

    /**
     * 加密算法
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * 认证标签长度（位）
     */
    private static final int TAG_BITS = 128;

    /**
     * 初始化向量长度（字节）
     */
    private static final int IV_BYTES = 12;

    /**
     * 最大明文长度（字节）
     */
    private static final int MAX_PLAINTEXT_BYTES = (1 << 30); // 1GB

    private final SecretKey key;
    private final SecureRandom random;

    public AesEncryptor(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 密钥必须是 32 字节长度");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.random = new SecureRandom();
    }

    /**
     * 加密字符串
     *
     * @param plaintext 明文
     * @return Base64 编码的密文（包含 IV 和认证标签）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        byte[] plaintextBytes = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        if (plaintextBytes.length > MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("明文长度超过最大限制");
        }

        try {
            // 生成随机 IV
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // 执行加密
            byte[] cipherText = cipher.doFinal(plaintextBytes);

            // 合并 IV + 密文
            byte[] combined = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, combined, IV_BYTES, cipherText.length);

            return Base64Utils.encode(combined);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param cipherBase64 Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String cipherBase64) {
        if (cipherBase64 == null) {
            return null;
        }

        try {
            // 解码 Base64
            byte[] combined = Base64Utils.decode(cipherBase64);

            if (combined.length < IV_BYTES) {
                throw new IllegalArgumentException("密文格式无效");
            }

            // 提取 IV
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);

            // 提取密文
            byte[] cipherText = Arrays.copyOfRange(combined, IV_BYTES, combined.length);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // 执行解密
            byte[] plaintextBytes = cipher.doFinal(cipherText);

            return new String(plaintextBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("AES 解密失败 - 认证标签不匹配，可能数据被篡改", e);
            throw new SecurityException("数据完整性校验失败，可能被篡改", e);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * 加密字节数组
     */
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null) {
            return null;
        }

        if (plaintext.length > MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("明文长度超过最大限制");
        }

        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext);

            byte[] combined = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, combined, IV_BYTES, cipherText.length);

            return combined;
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密字节数组
     */
    public byte[] decrypt(byte[] cipherBytes) {
        if (cipherBytes == null) {
            return null;
        }

        if (cipherBytes.length < IV_BYTES) {
            throw new IllegalArgumentException("密文格式无效");
        }

        try {
            byte[] iv = Arrays.copyOfRange(cipherBytes, 0, IV_BYTES);
            byte[] cipherText = Arrays.copyOfRange(cipherBytes, IV_BYTES, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            return cipher.doFinal(cipherText);
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("AES 解密失败 - 认证标签不匹配", e);
            throw new SecurityException("数据完整性校验失败", e);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * 验证密钥是否有效
     */
    public boolean validateKey() {
        try {
            // 尝试加密和解密一个测试字符串
            String test = "test";
            String encrypted = encrypt(test);
            String decrypted = decrypt(encrypted);
            return test.equals(decrypted);
        } catch (Exception e) {
            log.error("密钥验证失败", e);
            return false;
        }
    }
}
