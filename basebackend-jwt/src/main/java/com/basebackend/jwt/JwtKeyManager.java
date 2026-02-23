package com.basebackend.jwt;

import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 密钥管理器 — 支持多密钥管理和平滑密钥轮换
 * <p>
 * 维护一组 kid -> SecretKey 映射。当前活跃密钥用于签名新 Token，
 * 历史密钥在过渡期内仍可用于验证旧 Token。
 * <p>
 * 单密钥模式（向后兼容）：当 JwtProperties.keys 为空时，
 * 使用 jwt.secret 作为默认密钥，kid 为 {@value #DEFAULT_KID}。
 */
@Slf4j
public class JwtKeyManager {

    /** 单密钥模式下的默认 kid */
    static final String DEFAULT_KID = "default";

    /** kid -> SecretKey */
    private final Map<String, SecretKey> keyStore = new ConcurrentHashMap<>();

    /** kid -> 停用时间戳（毫秒），用于判断过渡期 */
    private final Map<String, Long> retiredAt = new ConcurrentHashMap<>();

    /** 当前活跃 kid（用于签名新 Token） */
    private volatile String activeKeyId;

    /** 旧密钥保留时间（毫秒） */
    private final long gracePeriodMillis;

    /**
     * 通过 JwtProperties 初始化密钥管理器
     */
    public JwtKeyManager(JwtProperties properties) {
        this.gracePeriodMillis = properties.getKeyRotationGracePeriod();

        if (properties.getKeys().isEmpty()) {
            // 单密钥模式：使用 jwt.secret
            SecretKey key = deriveKey(properties.getSecret());
            keyStore.put(DEFAULT_KID, key);
            activeKeyId = DEFAULT_KID;
            log.info("JwtKeyManager initialized in single-key mode (kid={})", DEFAULT_KID);
        } else {
            // 多密钥模式
            for (Map.Entry<String, String> entry : properties.getKeys().entrySet()) {
                keyStore.put(entry.getKey(), deriveKey(entry.getValue()));
            }
            activeKeyId = properties.getActiveKeyId();
            log.info("JwtKeyManager initialized with {} keys, active kid={}",
                    keyStore.size(), activeKeyId);
        }
    }

    // ========== 签名用 ==========

    /**
     * 获取当前活跃密钥（用于签名新 Token）
     */
    public SecretKey getActiveKey() {
        SecretKey key = keyStore.get(activeKeyId);
        if (key == null) {
            throw new IllegalStateException(
                    "Active key not found: kid=" + activeKeyId);
        }
        return key;
    }

    /**
     * 获取当前活跃密钥的 kid
     */
    public String getActiveKeyId() {
        return activeKeyId;
    }

    // ========== 验证用 ==========

    /**
     * 根据 kid 获取密钥（用于验证 Token）。
     * <p>
     * 如果 kid 对应的密钥已被停用且超过过渡期，返回 null。
     *
     * @param kid Token header 中的 kid；null 时返回默认密钥
     * @return 对应的 SecretKey，或 null 表示密钥不可用
     */
    public SecretKey getKeyByKid(String kid) {
        if (kid == null) {
            // 旧 Token 没有 kid header — 尝试默认密钥兼容
            return keyStore.get(DEFAULT_KID);
        }

        // 检查过渡期
        Long retiredTime = retiredAt.get(kid);
        if (retiredTime != null) {
            long elapsed = System.currentTimeMillis() - retiredTime;
            if (elapsed > gracePeriodMillis) {
                log.debug("Key kid={} retired and grace period expired ({}ms > {}ms), removing",
                        kid, elapsed, gracePeriodMillis);
                keyStore.remove(kid);
                retiredAt.remove(kid);
                return null;
            }
        }

        return keyStore.get(kid);
    }

    /**
     * 判断是否为单密钥模式（向后兼容）
     */
    public boolean isSingleKeyMode() {
        return keyStore.size() == 1 && keyStore.containsKey(DEFAULT_KID);
    }

    // ========== 密钥轮换操作 ==========

    /**
     * 添加新密钥并设为活跃密钥。原活跃密钥进入过渡期。
     *
     * @param newKeyId 新密钥的 kid
     * @param newSecret 新密钥原始字符串（>= 32 字符）
     */
    public void rotateKey(String newKeyId, String newSecret) {
        if (newKeyId == null || newKeyId.isBlank()) {
            throw new IllegalArgumentException("newKeyId must not be blank");
        }
        if (newSecret == null || newSecret.length() < 32) {
            throw new IllegalArgumentException(
                    "newSecret must be at least 32 characters (256 bits)");
        }

        String oldKeyId = this.activeKeyId;

        // 添加新密钥
        keyStore.put(newKeyId, deriveKey(newSecret));

        // 旧密钥标记为停用（进入过渡期）
        if (!oldKeyId.equals(newKeyId)) {
            retiredAt.put(oldKeyId, System.currentTimeMillis());
        }

        // 切换活跃密钥
        this.activeKeyId = newKeyId;

        log.info("Key rotated: {} -> {}. Old key enters grace period ({}ms)",
                oldKeyId, newKeyId, gracePeriodMillis);
    }

    /**
     * 立即停用指定密钥（不再可用于验证）
     *
     * @param keyId 要停用的 kid
     */
    public void retireKey(String keyId) {
        if (keyId.equals(activeKeyId)) {
            throw new IllegalStateException(
                    "Cannot retire the active key (kid=" + keyId + "). Rotate first.");
        }
        keyStore.remove(keyId);
        retiredAt.remove(keyId);
        log.info("Key retired immediately: kid={}", keyId);
    }

    /**
     * 列出所有可用的密钥 ID
     */
    public Set<String> listKeyIds() {
        return Collections.unmodifiableSet(keyStore.keySet());
    }

    // ========== 内部方法 ==========

    private static SecretKey deriveKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
