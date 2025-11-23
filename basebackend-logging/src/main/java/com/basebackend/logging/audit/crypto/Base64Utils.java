package com.basebackend.logging.audit.crypto;

import java.util.Base64;

/**
 * Base64 编码解码工具类
 *
 * 提供高效的 Base64 编解码功能。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public final class Base64Utils {

    private Base64Utils() {
        // 工具类，私有构造器
    }

    /**
     * Base64 编码
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 解码
     */
    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Base64 URL 安全编码
     */
    public static String encodeUrlSafe(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Base64 URL 安全解码
     */
    public static byte[] decodeUrlSafe(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }
}
