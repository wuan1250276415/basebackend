package com.basebackend.messaging.webhook;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook签名服务
 */
@Slf4j
@Service
public class WebhookSignatureService {

    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";

    /**
     * 生成HMAC-SHA256签名
     *
     * @param payload 请求体
     * @param secret  密钥
     * @param timestamp 时间戳
     * @return 签名字符串
     */
    public String generateSignature(String payload, String secret, long timestamp) {
        try {
            String signContent = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signContent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to generate signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * 验证签名
     *
     * @param payload    请求体
     * @param secret     密钥
     * @param timestamp  时间戳
     * @param signature  签名
     * @return true-验证通过，false-验证失败
     */
    public boolean verifySignature(String payload, String secret, long timestamp, String signature) {
        String expectedSignature = generateSignature(payload, secret, timestamp);
        return expectedSignature.equals(signature);
    }

    /**
     * 为HTTP请求添加签名头
     *
     * @param headers HTTP头
     * @param payload 请求体
     * @param secret  密钥
     */
    public void addSignatureHeaders(HttpHeaders headers, String payload, String secret) {
        long timestamp = System.currentTimeMillis();
        String signature = generateSignature(payload, secret, timestamp);
        headers.set(SIGNATURE_HEADER, signature);
        headers.set(TIMESTAMP_HEADER, String.valueOf(timestamp));
    }
}
