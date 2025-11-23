package com.basebackend.database.security.service;

/**
 * 加密服务接口
 * 提供数据加密和解密功能
 */
public interface EncryptionService {
    
    /**
     * 加密数据
     * 
     * @param plainText 明文
     * @return 密文
     */
    String encrypt(String plainText);
    
    /**
     * 解密数据
     * 
     * @param cipherText 密文
     * @return 明文
     */
    String decrypt(String cipherText);
    
    /**
     * 检查是否已加密
     * 
     * @param text 待检查的文本
     * @return true表示已加密，false表示未加密
     */
    boolean isEncrypted(String text);
}
