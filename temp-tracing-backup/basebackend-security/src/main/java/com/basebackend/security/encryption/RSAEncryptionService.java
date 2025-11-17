package com.basebackend.security.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA加密服务
 * 支持RSA-2048/4096加密，用于加密敏感数据和数字签名
 * 适合加密小量数据或用于加密AES密钥
 */
@Slf4j
@Service
public class RSAEncryptionService {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int KEY_SIZE = 2048;

    /**
     * 生成RSA密钥对
     *
     * @return KeyPair包含公钥和私钥
     */
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            log.info("RSA密钥对生成成功，密钥长度: {}位", KEY_SIZE);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            log.error("RSA密钥生成失败", e);
            throw new SecurityException("RSA密钥生成失败", e);
        }
    }

    /**
     * 获取公钥(Base64编码)
     *
     * @param keyPair 密钥对
     * @return Base64编码的公钥
     */
    public String getPublicKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * 获取私钥(Base64编码)
     *
     * @param keyPair 密钥对
     * @return Base64编码的私钥
     */
    public String getPrivateKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /**
     * 从Base64公钥字符串创建公钥对象
     *
     * @param publicKeyBase64 Base64编码的公钥
     * @return PublicKey对象
     */
    public PublicKey getPublicKey(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            log.error("从Base64创建公钥失败", e);
            throw new SecurityException("公钥创建失败", e);
        }
    }

    /**
     * 从Base64私钥字符串创建私钥对象
     *
     * @param privateKeyBase64 Base64编码的私钥
     * @return PrivateKey对象
     */
    public PrivateKey getPrivateKey(String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            log.error("从Base64创建私钥失败", e);
            throw new SecurityException("私钥创建失败", e);
        }
    }

    /**
     * 公钥加密
     *
     * @param plainText 明文
     * @param publicKeyBase64 Base64编码的公钥
     * @return 加密后的数据(Base64编码)
     */
    public String encrypt(String plainText, String publicKeyBase64) {
        try {
            PublicKey publicKey = getPublicKey(publicKeyBase64);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] plainBytes = plainText.getBytes();
            int maxLength = KEY_SIZE / 8 - 11; // RSA加密最大长度限制
            StringBuilder encryptedBuilder = new StringBuilder();

            // 分块加密
            for (int i = 0; i < plainBytes.length; i += maxLength) {
                int length = Math.min(maxLength, plainBytes.length - i);
                byte[] block = new byte[length];
                System.arraycopy(plainBytes, i, block, 0, length);

                byte[] encryptedBlock = cipher.doFinal(block);
                String encryptedBlockBase64 = Base64.getEncoder().encodeToString(encryptedBlock);
                encryptedBuilder.append(encryptedBlockBase64);
            }

            String encryptedText = encryptedBuilder.toString();
            log.debug("RSA加密成功，明文长度: {}, 密文长度: {}", plainText.length(), encryptedText.length());
            return encryptedText;

        } catch (Exception e) {
            log.error("RSA加密失败", e);
            throw new SecurityException("RSA加密失败", e);
        }
    }

    /**
     * 私钥解密
     *
     * @param encryptedText 加密的数据(Base64编码)
     * @param privateKeyBase64 Base64编码的私钥
     * @return 解密后的明文
     */
    public String decrypt(String encryptedText, String privateKeyBase64) {
        try {
            PrivateKey privateKey = getPrivateKey(privateKeyBase64);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // 按块解密，每块长度为256字节(RSA-2048)或512字节(RSA-4096)
            int blockLength = KEY_SIZE / 8;
            StringBuilder decryptedBuilder = new StringBuilder();

            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            for (int i = 0; i < encryptedBytes.length; i += blockLength) {
                int length = Math.min(blockLength, encryptedBytes.length - i);
                byte[] block = new byte[length];
                System.arraycopy(encryptedBytes, i, block, 0, length);

                byte[] decryptedBlock = cipher.doFinal(block);
                decryptedBuilder.append(new String(decryptedBlock));
            }

            String plainText = decryptedBuilder.toString();
            log.debug("RSA解密成功，密文长度: {}, 明文长度: {}", encryptedText.length(), plainText.length());
            return plainText;

        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new SecurityException("RSA解密失败", e);
        }
    }

    /**
     * 私钥签名
     *
     * @param data 要签名的数据
     * @param privateKeyBase64 Base64编码的私钥
     * @return 签名(Base64编码)
     */
    public String sign(String data, String privateKeyBase64) {
        try {
            PrivateKey privateKey = getPrivateKey(privateKeyBase64);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes());
            byte[] signBytes = signature.sign();
            String sign = Base64.getEncoder().encodeToString(signBytes);
            log.debug("RSA签名成功，数据长度: {}, 签名长度: {}", data.length(), sign.length());
            return sign;
        } catch (Exception e) {
            log.error("RSA签名失败", e);
            throw new SecurityException("RSA签名失败", e);
        }
    }

    /**
     * 公钥验签
     *
     * @param data 原始数据
     * @param sign 签名(Base64编码)
     * @param publicKeyBase64 Base64编码的公钥
     * @return 验签结果
     */
    public boolean verify(String data, String sign, String publicKeyBase64) {
        try {
            PublicKey publicKey = getPublicKey(publicKeyBase64);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());
            byte[] signBytes = Base64.getDecoder().decode(sign);
            boolean result = signature.verify(signBytes);
            log.debug("RSA验签成功: {}", result);
            return result;
        } catch (Exception e) {
            log.error("RSA验签失败", e);
            return false;
        }
    }
}
