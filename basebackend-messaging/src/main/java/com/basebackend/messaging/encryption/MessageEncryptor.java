package com.basebackend.messaging.encryption;

/**
 * 消息加密服务接口
 * <p>
 * 提供消息内容的加密和解密功能，支持多种加密算法。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface MessageEncryptor {

    /**
     * 加密消息内容
     *
     * @param plainText 明文内容
     * @return 加密后的密文（Base64编码）
     */
    String encrypt(String plainText);

    /**
     * 解密消息内容
     *
     * @param cipherText 密文内容（Base64编码）
     * @return 解密后的明文
     */
    String decrypt(String cipherText);

    /**
     * 判断是否需要加密指定Topic
     *
     * @param topic 消息主题
     * @return true 如果需要加密
     */
    boolean shouldEncrypt(String topic);

    /**
     * 获取加密算法名称
     *
     * @return 算法名称
     */
    String getAlgorithm();
}
