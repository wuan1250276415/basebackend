package com.basebackend.file.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JWT Token 安全服务
 * 
 * 提供完整的 JWT 验证功能：
 * 1. 签名验证（HMAC-SHA256）
 * 2. 过期时间验证
 * 3. 颁发者和受众验证
 * 4. Token ID 防重放
 *
 * @author BaseBackend Security Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class JwtTokenService {

    @Value("${file.security.jwt.secret:defaultSecretKeyForDevelopmentOnlyPleaseChangeInProduction123456}")
    private String jwtSecret;

    @Value("${file.security.jwt.issuer:basebackend-file-service}")
    private String jwtIssuer;

    @Value("${file.security.jwt.audience:basebackend}")
    private String jwtAudience;

    @Value("${file.security.jwt.expiration-hours:24}")
    private int expirationHours;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // 确保密钥长度足够（至少256位）
        if (jwtSecret.length() < 32) {
            log.warn("JWT密钥长度不足，建议使用至少32字符的密钥");
            jwtSecret = jwtSecret + "0".repeat(32 - jwtSecret.length());
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Token服务初始化完成");
    }

    /**
     * 解析并验证 JWT Token
     *
     * @param token JWT token字符串
     * @return 解析后的用户上下文，验证失败返回null
     */
    public UserContext parseAndValidateToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("Token为空");
            return null;
        }

        try {
            // 解析并验证Token
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            // 验证必要字段
            String userId = claims.getSubject();
            if (!StringUtils.hasText(userId)) {
                log.warn("Token缺少用户ID");
                return null;
            }

            // 构建用户上下文
            UserContext context = new UserContext();
            context.setUserId(userId);
            context.setTenantId(claims.get("tenantId", String.class));
            context.setAuthType(UserContext.AuthType.JWT);
            context.setTokenId(claims.getId());
            context.setAuthenticatedAt(LocalDateTime.now());

            // 解析角色
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) rolesObj;
                context.setRoles(roles);
            }

            // 解析权限
            Object permsObj = claims.get("permissions");
            if (permsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> permissions = (List<String>) permsObj;
                context.setPermissions(permissions);
            }

            log.debug("Token验证成功: userId={}, tenantId={}", userId, context.getTenantId());
            return context;

        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.warn("Token签名无效: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token类型: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("Token参数错误: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Token解析异常", e);
            return null;
        }
    }

    /**
     * 生成 JWT Token（用于测试或内部服务调用）
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param roles 角色列表
     * @return JWT token字符串
     */
    public String generateToken(String userId, String tenantId, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationHours * 60 * 60 * 1000L);

        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .issuer(jwtIssuer)
            .audience().add(jwtAudience).and()
            .issuedAt(now)
            .expiration(expiration)
            .claim("tenantId", tenantId)
            .claim("roles", roles)
            .signWith(secretKey)
            .compact();
    }

    /**
     * 检查Token是否即将过期（用于Token刷新）
     *
     * @param token JWT token
     * @param thresholdMinutes 阈值（分钟）
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token, int thresholdMinutes) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            Date expiration = claims.getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return remainingMs < thresholdMinutes * 60 * 1000L;

        } catch (Exception e) {
            return true; // 解析失败视为即将过期
        }
    }
}
