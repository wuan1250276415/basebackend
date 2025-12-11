package com.basebackend.security.service.impl;

import com.basebackend.jwt.JwtUtil;
import com.basebackend.security.exception.TokenBlacklistException;
import com.basebackend.security.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务实现
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_SESSION_PREFIX = "user:session:";
    private static final String BLACKLIST_PLACEHOLDER = "1";
    private static final long DEFAULT_TTL_HOURS = 24L;

    @Override
    public void addToBlacklist(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                log.debug("跳过空Token的黑名单写入");
                return;
            }
            // 解析Token过期时间并计算TTL
            long ttlHours = computeTtlHours(token);
            String key = buildBlacklistKey(token);
            valueOps().set(key, BLACKLIST_PLACEHOLDER, ttlHours, TimeUnit.HOURS);
            log.info("Token已加入黑名单，TTL={}小时: {}", ttlHours, maskToken(token));
        } catch (Exception e) {
            log.error("添加Token到黑名单失败", e);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                return false;
            }
            String key = buildBlacklistKey(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查Token黑名单失败，Redis不可用", e);
            // 抛出异常而不是返回false，避免fail-open安全问题
            throw new TokenBlacklistException("检查Token黑名单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeFromBlacklist(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                return;
            }
            String key = buildBlacklistKey(token);
            redisTemplate.delete(key);
            log.info("Token已从黑名单移除: {}", maskToken(token));
        } catch (Exception e) {
            log.error("从黑名单移除Token失败", e);
        }
    }

    @Override
    public void addUserSession(String userId, String token) {
        try {
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(token)) {
                log.debug("跳过空用户会话写入");
                return;
            }
            // 解析Token过期时间并计算TTL
            long ttlHours = computeTtlHours(token);
            String key = buildSessionKey(userId);
            valueOps().set(key, token, ttlHours, TimeUnit.HOURS);
            log.info("用户会话已添加，TTL={}小时: userId={}, token={}", ttlHours, userId, maskToken(token));
        } catch (Exception e) {
            log.error("添加用户会话失败", e);
        }
    }

    @Override
    public String getUserToken(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                return null;
            }
            String key = buildSessionKey(userId);
            Object token = valueOps().get(key);
            return token != null ? token.toString() : null;
        } catch (Exception e) {
            log.error("获取用户Token失败", e);
            return null;
        }
    }

    @Override
    public void removeUserSession(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                return;
            }
            String key = buildSessionKey(userId);
            redisTemplate.delete(key);
            log.info("用户会话已移除: userId={}", userId);
        } catch (Exception e) {
            log.error("移除用户会话失败", e);
        }
    }

    @Override
    public void forceLogoutUser(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                log.debug("跳过空用户ID的强制下线");
                return;
            }
            // 获取当前Token
            String currentToken = getUserToken(userId);
            if (currentToken != null) {
                // 将当前Token加入黑名单
                addToBlacklist(currentToken);
            }
            // 移除用户会话
            removeUserSession(userId);
            log.info("用户已被强制下线: userId={}", userId);
        } catch (Exception e) {
            log.error("强制用户下线失败", e);
        }
    }

    private ValueOperations<String, Object> valueOps() {
        return redisTemplate.opsForValue();
    }

    private String buildBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + hashToken(token);
    }

    private String buildSessionKey(String userId) {
        return USER_SESSION_PREFIX + userId;
    }

    /**
     * 计算Token的TTL（基于JWT过期时间）
     * 如果无法解析JWT或已过期，使用默认TTL
     */
    private long computeTtlHours(String token) {
        try {
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            if (expiration != null) {
                long now = System.currentTimeMillis();
                long expireTime = expiration.getTime();
                long ttlMillis = expireTime - now;

                if (ttlMillis > 0) {
                    // 转换为小时，向上取整到分钟，确保不会过早过期
                    long ttlMinutes = (ttlMillis + 59999) / 60000; // 向上取整到分钟
                    long ttlHours = (ttlMinutes + 59) / 60; // 向上取整到小时

                    // 至少保持1小时，最长不超过默认TTL
                    return Math.max(1, Math.min(ttlHours, DEFAULT_TTL_HOURS));
                }
            }
        } catch (Exception e) {
            log.debug("解析Token过期时间失败，使用默认TTL: {}", e.getMessage());
        }

        // 解析失败或已过期，使用默认TTL
        return DEFAULT_TTL_HOURS;
    }

    /**
     * 对Token进行SHA-256哈希处理
     * 使用不可逆哈希避免在Redis键和日志中暴露原始Token
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Token哈希失败，使用原始值: {}", e.getMessage());
            // 如果哈希失败，回退到使用原始Token（不应该发生）
            return token;
        }
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "<empty>";
        }
        int prefixLen = Math.min(6, token.length());
        return token.substring(0, prefixLen) + "...";
    }
}
