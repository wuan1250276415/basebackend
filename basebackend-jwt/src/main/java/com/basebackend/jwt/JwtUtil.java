package com.basebackend.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 - 统一的JWT生成和验证
 *
 * 此工具类被Gateway和各个微服务共享使用，确保Token的生成和验证逻辑完全一致
 * 不依赖Spring Security，可以在WebFlux和Spring MVC环境中使用
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:basebackend-secret-key-for-jwt-token-generation-minimum-256-bits}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 生成密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     */
    public String generateToken(String subject) {
        return generateToken(subject, null);
    }

    /**
     * 生成Token（带自定义声明）
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey());

        if (claims != null && !claims.isEmpty()) {
            // 先添加自定义claims，再设置subject和时间，避免被覆盖
            builder.claims().add(claims);
        }

        return builder.compact();
    }

    /**
     * 从Token中获取主题
     */
    public String getSubjectFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从Token中获取声明
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        // 保留原有的自定义claims
        Map<String, Object> customClaims = new java.util.HashMap<>(claims);
        customClaims.remove("sub");
        customClaims.remove("iat");
        customClaims.remove("exp");

        return generateToken(claims.getSubject(), customClaims);
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 检查Token是否即将过期（1小时内）
     */
    public boolean isTokenExpiringSoon(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        if (expirationDate == null) {
            return true;
        }
        long diff = expirationDate.getTime() - System.currentTimeMillis();
        return diff < 3600000; // 1小时
    }
}
