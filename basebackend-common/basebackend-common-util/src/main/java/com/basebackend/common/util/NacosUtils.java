package com.basebackend.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class NacosUtils {
    /**
     * <p>
     * nacos中JWT令牌密钥生成器<br>
     * 生成 nacos.core.auth.plugin.nacos.token.secret.key 的值
     * </p>
     * @author shigzh
     * @since 2024/1/10 13:41
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || args[0] == null || args[0].isBlank()) {
            System.out.println("用法：java com.basebackend.common.util.NacosUtils <明文密钥>");
            return;
        }
        String nacosSecret = args[0];
        // 输出密钥长度，要求不得低于32字符，否则无法启动节点。
        System.out.println("密钥长度》》》" + nacosSecret.length());
        System.out.println("密钥Base64编码》》》" + encodeToBase64(nacosSecret));
    }

    public static String encodeToBase64(String plainSecret) {
        if (plainSecret == null || plainSecret.isBlank()) {
            throw new IllegalArgumentException("明文密钥不能为空");
        }
        byte[] data = plainSecret.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(data);
    }

}
