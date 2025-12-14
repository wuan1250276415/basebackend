package com.basebackend.common.util;

import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;

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
            // 自定义生成JWT令牌的密钥
            String nacosSecret = "nacos_20251021_wuan_nacos_token";
            // 输出密钥长度，要求不得低于32字符，否则无法启动节点。
            System.out.println("密钥长度》》》" + nacosSecret.length());
            // 密钥进行Base64编码
            byte[] data = nacosSecret.getBytes(StandardCharsets.UTF_8);
            System.out.println("密钥Base64编码》》》" + Base64Utils.encodeToString(data));
        }

}
