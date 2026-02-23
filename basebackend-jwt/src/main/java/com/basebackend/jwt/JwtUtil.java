package com.basebackend.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类 - 统一的JWT生成和验证
 * <p>
 * 此工具类被Gateway和各个微服务共享使用，确保Token的生成和验证逻辑完全一致。
 * 不依赖Spring Security，可以在WebFlux和Spring MVC环境中使用。
 * <p>
 * 支持双 Token 机制（Access + Refresh）、黑名单吊销、issuer/audience 验证、
 * 结构化异常处理、密钥轮换（P2）、多设备管理（P3-1）、事件审计（P3-2）。
 */
@Slf4j
@Component
public class JwtUtil {

    /** Token 类型 claim 键 */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    /** Access Token 类型值 */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** Refresh Token 类型值 */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final JwtKeyManager keyManager;
    private final JwtTokenBlacklist blacklist;
    @Nullable
    private final JwtDeviceManager deviceManager;
    @Nullable
    private final JwtAuditLogger auditLogger;

    public JwtUtil(JwtProperties properties,
                   JwtKeyManager keyManager,
                   JwtTokenBlacklist blacklist,
                   @Nullable JwtDeviceManager deviceManager,
                   @Nullable JwtAuditLogger auditLogger) {
        this.properties = properties;
        this.keyManager = keyManager;
        this.blacklist = blacklist;
        this.deviceManager = deviceManager;
        this.auditLogger = auditLogger;
    }

    // ========== Token 生成（向后兼容） ==========

    /**
     * 生成Token（向后兼容，使用通用过期时间）
     */
    public String generateToken(String subject) {
        return generateToken(subject, null);
    }

    /**
     * 生成Token（带自定义声明，向后兼容）
     * <p>
     * P0-1 修复：先添加自定义 claims，再设置 subject/issuedAt/expiration，
     * 确保核心字段不被自定义 claims 中的 sub/iat/exp 键覆盖。
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        String token = buildToken(subject, claims, properties.getExpiration());
        auditTokenGenerated(subject, token, null);
        return token;
    }

    // ========== 双 Token 机制 ==========

    /**
     * 生成 Access Token（短过期，默认 30 分钟）
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Map<String, Object> enrichedClaims = claims != null ? new HashMap<>(claims) : new HashMap<>();
        enrichedClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        String token = buildToken(subject, enrichedClaims, properties.getAccessTokenExpiration());
        auditTokenGenerated(subject, token, TOKEN_TYPE_ACCESS);
        return token;
    }

    /**
     * 生成 Access Token 并注册设备会话（P3-1 多设备管理）
     *
     * @param subject    Token 主题
     * @param claims     自定义 claims
     * @param deviceInfo 设备信息（可为 null，若为 null 则不注册设备）
     * @return 生成的 Access Token
     */
    public String generateAccessToken(String subject, Map<String, Object> claims,
                                      @Nullable DeviceInfo deviceInfo) {
        Map<String, Object> enrichedClaims = claims != null ? new HashMap<>(claims) : new HashMap<>();
        enrichedClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        if (deviceInfo != null) {
            enrichedClaims.put("deviceId", deviceInfo.getDeviceId());
        }
        String token = buildToken(subject, enrichedClaims, properties.getAccessTokenExpiration());

        // 注册设备
        if (deviceInfo != null && deviceManager != null) {
            Long userId = extractUserId(claims);
            if (userId != null) {
                String jti = getJtiFromToken(token);
                deviceManager.registerDevice(userId, deviceInfo, jti);
                auditDeviceRegistered(userId, deviceInfo.getDeviceId());
            }
        }

        auditTokenGenerated(subject, token, TOKEN_TYPE_ACCESS);
        return token;
    }

    /**
     * 生成 Refresh Token（长过期，默认 7 天）
     */
    public String generateRefreshToken(String subject) {
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        String token = buildToken(subject, refreshClaims, properties.getRefreshTokenExpiration());
        auditTokenGenerated(subject, token, TOKEN_TYPE_REFRESH);
        return token;
    }

    /**
     * 用 Refresh Token 换取新的 Access Token
     *
     * @param refreshToken 有效的 Refresh Token
     * @return 新的 Access Token
     * @throws JwtException 如果 refreshToken 无效或类型不匹配
     */
    public String refreshAccessToken(String refreshToken) {
        Claims claims = parseClaimsStrict(refreshToken);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
            auditValidationFailed(claims.getSubject(), "TOKEN_TYPE_MISMATCH",
                    "Expected refresh token but got: " + tokenType);
            throw new JwtException(JwtException.ErrorType.TOKEN_TYPE_MISMATCH,
                    "Expected refresh token but got: " + tokenType);
        }

        // 检查黑名单
        String jti = claims.getId();
        if (jti != null && blacklist.isRevoked(jti)) {
            auditValidationFailed(claims.getSubject(), "REVOKED", "Refresh token has been revoked");
            throw new JwtException(JwtException.ErrorType.REVOKED, "Refresh token has been revoked");
        }

        String newToken = generateAccessToken(claims.getSubject(), extractCustomClaims(claims));
        auditTokenRefreshed(claims.getSubject(), newToken);
        return newToken;
    }

    // ========== Token 内部构建 ==========

    /**
     * 核心 Token 构建方法
     * <p>
     * P0-1：先添加自定义 claims，再设置核心字段（subject/iat/exp/jti/issuer/audience），
     * 防止自定义 claims 覆盖核心字段。
     * <p>
     * P2：使用 JwtKeyManager 获取签名密钥，并在多密钥模式下设置 kid header。
     */
    private String buildToken(String subject, Map<String, Object> customClaims,
                              long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);
        String jti = UUID.randomUUID().toString();

        var builder = Jwts.builder();

        // P2: 多密钥模式下设置 kid header
        if (!keyManager.isSingleKeyMode()) {
            builder.header().keyId(keyManager.getActiveKeyId()).and();
        }

        // 先添加自定义 claims（低优先级）
        if (customClaims != null && !customClaims.isEmpty()) {
            builder.claims().add(customClaims);
        }

        // 再设置核心字段（高优先级，覆盖同名自定义 claims）
        builder.id(jti)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate);

        // issuer
        String issuer = properties.getIssuer();
        if (issuer != null && !issuer.isBlank()) {
            builder.issuer(issuer);
        }

        // audience
        String audience = properties.getAudience();
        if (audience != null && !audience.isBlank()) {
            builder.audience().add(audience);
        }

        // P2: 使用 JwtKeyManager 获取活跃密钥签名
        builder.signWith(keyManager.getActiveKey());

        return builder.compact();
    }

    // ========== Token 解析 ==========

    /**
     * 从Token中获取声明（向后兼容：解析失败返回 null）
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return parseClaimsStrict(token);
        } catch (JwtException e) {
            log.error("解析Token失败: {} ({})", e.getMessage(), e.getErrorType());
            return null;
        }
    }

    /**
     * 严格解析 Token，失败时抛出分类异常。
     * <p>
     * P2: 先从 Token header 中提取 kid，然后使用 JwtKeyManager 获取对应密钥验证。
     * 如果 Token 没有 kid header（旧 Token），使用默认密钥验证（向后兼容）。
     *
     * @throws JwtException 携带 ErrorType 的结构化异常
     */
    public Claims parseClaimsStrict(String token) {
        try {
            // P2: 从 Token header 提取 kid，选择对应密钥
            SecretKey verificationKey = resolveVerificationKey(token);

            var parserBuilder = Jwts.parser()
                    .verifyWith(verificationKey);

            // issuer 验证
            String issuer = properties.getIssuer();
            if (issuer != null && !issuer.isBlank()) {
                parserBuilder.requireIssuer(issuer);
            }

            // audience 验证
            String audience = properties.getAudience();
            if (audience != null && !audience.isBlank()) {
                parserBuilder.requireAudience(audience);
            }

            Claims claims = parserBuilder.build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 黑名单检查
            String jti = claims.getId();
            if (jti != null && blacklist.isRevoked(jti)) {
                throw new JwtException(JwtException.ErrorType.REVOKED,
                        "Token has been revoked (jti=" + jti + ")");
            }

            return claims;
        } catch (JwtException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            throw new JwtException(JwtException.ErrorType.EXPIRED,
                    "Token has expired", e);
        } catch (SecurityException e) {
            throw new JwtException(JwtException.ErrorType.INVALID_SIGNATURE,
                    "Token signature is invalid", e);
        } catch (MalformedJwtException e) {
            throw new JwtException(JwtException.ErrorType.MALFORMED,
                    "Token is malformed", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtException(JwtException.ErrorType.UNSUPPORTED,
                    "Token type is unsupported", e);
        } catch (Exception e) {
            throw new JwtException(JwtException.ErrorType.MALFORMED,
                    "Token parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析已过期的 Token 的 claims（用于 Token 刷新场景）
     * <p>
     * P1-1：从 ExpiredJwtException 中提取 claims，但限制最大允许过期时长。
     *
     * @param token 可能已过期的 Token
     * @return claims（如果 Token 签名有效且在最大允许过期时长内）
     * @throws JwtException 如果签名无效、格式错误或过期太久
     */
    public Claims parseClaimsAllowExpired(String token) {
        try {
            return parseClaimsStrict(token);
        } catch (JwtException e) {
            if (e.getErrorType() != JwtException.ErrorType.EXPIRED || e.getCause() == null) {
                throw e;
            }
            ExpiredJwtException expiredEx = (ExpiredJwtException) e.getCause();
            Claims claims = expiredEx.getClaims();

            // 检查过期时长限制
            long expiredAt = claims.getExpiration().getTime();
            long overdueMillis = System.currentTimeMillis() - expiredAt;
            if (overdueMillis > properties.getMaxRefreshableExpiredDuration()) {
                throw new JwtException(JwtException.ErrorType.EXPIRED,
                        "Token expired too long ago (" + overdueMillis + "ms), " +
                                "max allowed: " + properties.getMaxRefreshableExpiredDuration() + "ms");
            }

            // 黑名单检查
            String jti = claims.getId();
            if (jti != null && blacklist.isRevoked(jti)) {
                throw new JwtException(JwtException.ErrorType.REVOKED,
                        "Expired token has been revoked (jti=" + jti + ")");
            }

            return claims;
        }
    }

    // ========== 安全验证方法返回结果（无异常） ==========

    /**
     * 验证 Token，返回结构化结果而非抛异常
     */
    public JwtValidationResult validateTokenSafe(String token) {
        try {
            Claims claims = parseClaimsStrict(token);
            return JwtValidationResult.success(claims);
        } catch (JwtException e) {
            return JwtValidationResult.failure(e.getErrorType(), e.getMessage());
        }
    }

    /**
     * 验证 Token 并检查类型
     */
    public JwtValidationResult validateTokenSafe(String token, String expectedTokenType) {
        JwtValidationResult result = validateTokenSafe(token);
        if (!result.isValid()) {
            return result;
        }
        String actualType = result.getClaims().get(CLAIM_TOKEN_TYPE, String.class);
        if (expectedTokenType != null && !expectedTokenType.equals(actualType)) {
            return JwtValidationResult.failure(JwtException.ErrorType.TOKEN_TYPE_MISMATCH,
                    "Expected token type '" + expectedTokenType + "' but got '" + actualType + "'");
        }
        return result;
    }

    // ========== 向后兼容的便捷方法 ==========

    /**
     * 从Token中获取主题
     */
    public String getSubjectFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }

        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }

        String subject = claims.getSubject();
        if (subject != null && subject.matches("\\d+")) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: subject={}", subject);
            }
        }

        log.warn("Token中未找到有效的用户ID");
        return null;
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }

        Object usernameObj = claims.get("username");
        if (usernameObj instanceof String) {
            return (String) usernameObj;
        }

        return claims.getSubject();
    }

    /**
     * 从Token中获取部门ID
     */
    public Long getDeptIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }

        Object deptIdObj = claims.get("deptId");
        if (deptIdObj instanceof Integer) {
            return ((Integer) deptIdObj).longValue();
        } else if (deptIdObj instanceof Long) {
            return (Long) deptIdObj;
        }

        return null;
    }

    /**
     * 验证Token是否有效（向后兼容）
     */
    public boolean validateToken(String token) {
        return validateTokenSafe(token).isValid();
    }

    /**
     * 刷新Token（向后兼容）
     * <p>
     * P1-1 修复：使用 parseClaimsAllowExpired 允许刷新刚过期的 Token
     */
    public String refreshToken(String token) {
        try {
            Claims claims = parseClaimsAllowExpired(token);
            Map<String, Object> customClaims = extractCustomClaims(claims);
            String newToken = generateToken(claims.getSubject(), customClaims);
            auditTokenRefreshed(claims.getSubject(), newToken);
            return newToken;
        } catch (JwtException e) {
            log.error("刷新Token失败: {} ({})", e.getMessage(), e.getErrorType());
            return null;
        }
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 检查Token是否即将过期
     * <p>
     * P1-6：阈值改为可配置（jwt.expiring-soon-threshold）
     */
    public boolean isTokenExpiringSoon(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        if (expirationDate == null) {
            return true;
        }
        long diff = expirationDate.getTime() - System.currentTimeMillis();
        return diff < properties.getExpiringSoonThreshold();
    }

    // ========== 黑名单操作 ==========

    /**
     * 吊销 Token
     */
    public void revokeToken(String token) {
        try {
            // 先尝试正常解析
            Claims claims = parseClaimsStrict(token);
            doRevoke(claims);
            auditTokenRevoked(claims.getSubject(), claims.getId());
        } catch (JwtException e) {
            if (e.getErrorType() == JwtException.ErrorType.EXPIRED && e.getCause() instanceof ExpiredJwtException ex) {
                // 已过期的 Token 也能吊销（阻止刷新）
                doRevoke(ex.getClaims());
                auditTokenRevoked(ex.getClaims().getSubject(), ex.getClaims().getId());
            } else {
                throw e;
            }
        }
    }

    private void doRevoke(Claims claims) {
        String jti = claims.getId();
        if (jti == null) {
            throw new JwtException(JwtException.ErrorType.MALFORMED,
                    "Cannot revoke token without jti claim");
        }
        blacklist.revoke(jti, claims.getExpiration().getTime());
    }

    /**
     * 检查 Token 是否已被吊销
     */
    public boolean isTokenRevoked(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        String jti = claims.getId();
        return jti != null && blacklist.isRevoked(jti);
    }

    // ========== P2: 密钥管理 ==========

    /**
     * 获取密钥管理器（供外部使用密钥轮换 API）
     */
    public JwtKeyManager getKeyManager() {
        return keyManager;
    }

    // ========== P3-1: 多设备管理 ==========

    /**
     * 踢下线指定设备：移除设备记录 + 吊销该设备的 Token
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     */
    public void kickDevice(Long userId, String deviceId) {
        if (deviceManager == null) {
            log.warn("DeviceManager not available, cannot kick device");
            return;
        }
        DeviceSession session = deviceManager.getDeviceSession(userId, deviceId);
        if (session != null && session.getTokenJti() != null) {
            // 吊销设备对应的 Token（通过 jti 加入黑名单，TTL 使用 access token 过期时间）
            long expiresAt = System.currentTimeMillis() + properties.getAccessTokenExpiration();
            blacklist.revoke(session.getTokenJti(), expiresAt);
        }
        deviceManager.removeDevice(userId, deviceId);
        auditDeviceKicked(userId, deviceId);
    }

    /**
     * 获取设备管理器（供外部使用多设备管理 API）
     */
    @Nullable
    public JwtDeviceManager getDeviceManager() {
        return deviceManager;
    }

    // ========== P2: 内部 - 密钥解析 ==========

    /**
     * 从 Token 的 header 中提取 kid，然后从 JwtKeyManager 获取对应密钥。
     * 如果 Token 没有 kid header（旧 Token），使用默认密钥（向后兼容）。
     */
    private SecretKey resolveVerificationKey(String token) {
        String kid = extractKidFromHeader(token);
        SecretKey key = keyManager.getKeyByKid(kid);
        if (key == null) {
            throw new JwtException(JwtException.ErrorType.INVALID_SIGNATURE,
                    "No key found for kid: " + kid);
        }
        return key;
    }

    /**
     * 从 JWT 未验证的 header 中提取 kid（不验证签名，仅读 header）
     */
    private String extractKidFromHeader(String token) {
        try {
            // JWT 格式: header.payload.signature
            int firstDot = token.indexOf('.');
            if (firstDot < 0) {
                return null;
            }
            String headerJson = new String(
                    java.util.Base64.getUrlDecoder().decode(token.substring(0, firstDot)),
                    java.nio.charset.StandardCharsets.UTF_8);
            // 简单提取 kid — 避免引入额外 JSON 解析依赖
            int kidIdx = headerJson.indexOf("\"kid\"");
            if (kidIdx < 0) {
                return null;
            }
            int colonIdx = headerJson.indexOf(':', kidIdx);
            int quoteStart = headerJson.indexOf('"', colonIdx + 1);
            int quoteEnd = headerJson.indexOf('"', quoteStart + 1);
            if (quoteStart < 0 || quoteEnd < 0) {
                return null;
            }
            return headerJson.substring(quoteStart + 1, quoteEnd);
        } catch (Exception e) {
            log.debug("Failed to extract kid from token header: {}", e.getMessage());
            return null;
        }
    }

    // ========== P3-2: 审计辅助方法 ==========

    private void auditTokenGenerated(String subject, String token, @Nullable String tokenType) {
        if (auditLogger == null) return;
        String jti = getJtiFromToken(token);
        Map<String, Object> details = new HashMap<>();
        if (tokenType != null) {
            details.put("tokenType", tokenType);
        }
        auditLogger.log(JwtAuditEvent.TOKEN_GENERATED, subject, null, jti, details);
    }

    private void auditTokenRefreshed(String subject, String newToken) {
        if (auditLogger == null) return;
        String jti = getJtiFromToken(newToken);
        auditLogger.log(JwtAuditEvent.TOKEN_REFRESHED, subject, null, jti, null);
    }

    private void auditTokenRevoked(String subject, String jti) {
        if (auditLogger == null) return;
        auditLogger.log(JwtAuditEvent.TOKEN_REVOKED, subject, null, jti, null);
    }

    private void auditValidationFailed(String subject, String reason, String message) {
        if (auditLogger == null) return;
        Map<String, Object> details = Map.of("reason", reason, "message", message);
        auditLogger.log(JwtAuditEvent.VALIDATION_FAILED, subject, null, null, details);
    }

    private void auditDeviceRegistered(Long userId, String deviceId) {
        if (auditLogger == null) return;
        auditLogger.log(JwtAuditEvent.DEVICE_REGISTERED, String.valueOf(userId), deviceId, null, null);
    }

    private void auditDeviceKicked(Long userId, String deviceId) {
        if (auditLogger == null) return;
        auditLogger.log(JwtAuditEvent.DEVICE_KICKED, String.valueOf(userId), deviceId, null, null);
    }

    // ========== 内部工具 ==========

    /**
     * 从 Claims 中提取自定义字段（排除 JWT 标准字段）
     */
    private Map<String, Object> extractCustomClaims(Claims claims) {
        Map<String, Object> customClaims = new HashMap<>(claims);
        customClaims.remove("sub");
        customClaims.remove("iat");
        customClaims.remove("exp");
        customClaims.remove("jti");
        customClaims.remove("iss");
        customClaims.remove("aud");
        return customClaims;
    }

    /**
     * 从已生成 Token 中提取 jti（内部使用，不做完整验证）
     */
    private String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getId() : null;
    }

    /**
     * 从 claims map 中提取 userId
     */
    private Long extractUserId(@Nullable Map<String, Object> claims) {
        if (claims == null) return null;
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        }
        return null;
    }
}
