package com.basebackend.service.client;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 内部服务请求签名工具。
 *
 * <p>使用共享密钥对「服务名 + 时间戳 + HTTP 方法 + 路径」做 HMAC-SHA256，
 * 避免外部用户仅靠伪造 header 就冒充内部调用来源。</p>
 */
public final class InternalRequestAuth {

    public static final String HEADER_INTERNAL_CALL = "X-Internal-Call";
    public static final String HEADER_SERVICE_NAME = "X-Internal-Service";
    public static final String HEADER_TIMESTAMP = "X-Internal-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Internal-Signature";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private InternalRequestAuth() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String sign(String sharedSecret, String serviceName, long timestamp,
                              String method, String path) {
        if (sharedSecret == null || sharedSecret.isBlank()) {
            throw new IllegalArgumentException("sharedSecret must not be blank");
        }
        String payload = buildPayload(serviceName, timestamp, method, path);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign internal request", e);
        }
    }

    public static boolean verify(String sharedSecret, String serviceName, long timestamp,
                                 String method, String path, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = sign(sharedSecret, serviceName, timestamp, method, path);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String buildPayload(String serviceName, long timestamp, String method, String path) {
        String normalizedServiceName = serviceName != null ? serviceName.trim() : "";
        String normalizedMethod = method != null ? method.trim().toUpperCase() : "GET";
        String normalizedPath = (path == null || path.isBlank()) ? "/" : path.trim();
        return normalizedServiceName + "\n" + timestamp + "\n" + normalizedMethod + "\n" + normalizedPath;
    }
}
