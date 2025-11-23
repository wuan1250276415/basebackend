package com.basebackend.logging.audit.crypto;

import com.basebackend.logging.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计数字签名服务
 *
 * 提供审计日志的数字签名和验证功能，
 * 支持密钥轮换和多种签名算法。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class AuditSignatureService {

    /**
     * 签名算法
     */
    private final String algorithm;

    /**
     * 密钥存储（Key ID -> KeyPair）
     */
    private final Map<String, KeyPair> keyStore;

    /**
     * 证书存储（Key ID -> Certificate）
     */
    private final Map<String, Certificate> certificateStore;

    /**
     * 当前活跃的密钥 ID
     */
    private volatile String activeKeyId;

    /**
     * 密钥轮换检查器
     */
    private final KeyRotationChecker rotationChecker;

    public AuditSignatureService(String algorithm) {
        this(algorithm, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), null);
    }

    public AuditSignatureService(String algorithm, Map<String, KeyPair> initialKeys,
                                 Map<String, Certificate> certificates, String activeKeyId) {
        this.algorithm = algorithm;
        this.keyStore = new ConcurrentHashMap<>(initialKeys);
        this.certificateStore = new ConcurrentHashMap<>(certificates);
        this.activeKeyId = activeKeyId;
        this.rotationChecker = new KeyRotationChecker();

        if (this.keyStore.isEmpty()) {
            generateNewKeyPair();
        }
    }

    /**
     * 生成新的密钥对
     */
    private void generateNewKeyPair() {
        try {
            String keyId = "key-" + System.currentTimeMillis();
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(getKeyAlgorithm(algorithm));

            if (algorithm.contains("RSA")) {
                keyGen.initialize(3072); // 使用 3072 位 RSA 密钥
            } else if (algorithm.contains("ECDSA")) {
                keyGen.initialize(256); // 使用 256 位 EC 密钥
            } else {
                throw new IllegalArgumentException("不支持的签名算法: " + algorithm);
            }

            KeyPair keyPair = keyGen.generateKeyPair();
            keyStore.put(keyId, keyPair);
            activeKeyId = keyId;

            log.info("生成新的密钥对，算法: {}, 密钥 ID: {}", algorithm, keyId);
        } catch (Exception e) {
            log.error("生成密钥对失败", e);
            throw new RuntimeException("生成密钥对失败", e);
        }
    }

    /**
     * 轮换密钥
     */
    public void rotateKey() {
        generateNewKeyPair();
        log.info("密钥轮换完成，新活跃密钥: {}", activeKeyId);
    }

    /**
     * 为审计日志条目签名
     */
    public AuditLogEntry sign(AuditLogEntry entry) {
        if (entry == null) {
            return null;
        }

        try {
            KeyPair keyPair = keyStore.get(activeKeyId);
            if (keyPair == null) {
                throw new IllegalStateException("未找到活跃密钥: " + activeKeyId);
            }

            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(keyPair.getPrivate());

            // 使用稳定序列化的 JSON
            byte[] data = CryptoObjectMapper.INSTANCE.writeValueAsString(entry)
                    .getBytes(StandardCharsets.UTF_8);
            signature.update(data);

            byte[] signatureBytes = signature.sign();
            entry.setSignature(Base64Utils.encode(signatureBytes));
            entry.setCertificateId(activeKeyId);

            log.debug("审计日志签名完成，ID: {}, 密钥: {}", entry.getId(), activeKeyId);
            return entry;
        } catch (Exception e) {
            log.error("审计日志签名失败", e);
            throw new RuntimeException("审计日志签名失败", e);
        }
    }

    /**
     * 验证审计日志条目的签名
     */
    public boolean verify(AuditLogEntry entry) {
        if (entry == null || entry.getSignature() == null) {
            return false;
        }

        try {
            Certificate certificate = certificateStore.get(entry.getCertificateId());
            if (certificate == null) {
                log.warn("未找到证书: {}", entry.getCertificateId());
                return false;
            }

            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(certificate);

            byte[] data = CryptoObjectMapper.INSTANCE.writeValueAsString(entry)
                    .getBytes(StandardCharsets.UTF_8);
            signature.update(data);

            boolean valid = signature.verify(Base64Utils.decode(entry.getSignature()));

            if (valid) {
                log.debug("审计日志签名验证通过，ID: {}", entry.getId());
            } else {
                log.warn("审计日志签名验证失败，ID: {}", entry.getId());
            }

            return valid;
        } catch (Exception e) {
            log.error("审计日志签名验证异常", e);
            return false;
        }
    }

    /**
     * 批量验证签名
     */
    public boolean verifyBatch(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return true;
        }

        boolean allValid = true;
        int invalidCount = 0;

        for (AuditLogEntry entry : entries) {
            if (!verify(entry)) {
                allValid = false;
                invalidCount++;
                log.error("签名验证失败，审计日志 ID: {}", entry.getId());
            }
        }

        if (!allValid) {
            log.error("批量签名验证失败，失败数量: {}/{}", invalidCount, entries.size());
        } else {
            log.info("批量签名验证通过，共 {} 个条目", entries.size());
        }

        return allValid;
    }

    /**
     * 获取密钥算法
     */
    private String getKeyAlgorithm(String signatureAlgorithm) {
        if (signatureAlgorithm.contains("RSA")) {
            return "RSA";
        } else if (signatureAlgorithm.contains("ECDSA")) {
            return "EC";
        } else {
            throw new IllegalArgumentException("不支持的签名算法: " + signatureAlgorithm);
        }
    }

    /**
     * 获取活跃密钥 ID
     */
    public String getActiveKeyId() {
        return activeKeyId;
    }

    /**
     * 检查是否需要密钥轮换
     */
    public boolean needsKeyRotation() {
        return rotationChecker.needsRotation();
    }

    /**
     * 获取签名算法
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * 密钥轮换检查器
     */
    private static class KeyRotationChecker {

        private static final int DEFAULT_ROTATION_DAYS = 90;
        private final long creationTime;

        public KeyRotationChecker() {
            this.creationTime = System.currentTimeMillis();
        }

        public boolean needsRotation() {
            long daysSinceCreation = (System.currentTimeMillis() - creationTime) / (1000 * 60 * 60 * 24);
            return daysSinceCreation >= DEFAULT_ROTATION_DAYS;
        }
    }
}
