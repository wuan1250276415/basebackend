package com.basebackend.security.service.impl;

import com.basebackend.security.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_SESSION_PREFIX = "user:session:";

    @Override
    public void addToBlacklist(String token) {
        try {
            // 从Token中获取过期时间
            // 注意：这里简化处理，实际应该解析JWT获取exp字段
            String key = TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
            log.info("Token已加入黑名单: {}", token);
        } catch (Exception e) {
            log.error("添加Token到黑名单失败", e);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查Token黑名单失败", e);
            return false;
        }
    }

    @Override
    public void removeFromBlacklist(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("Token已从黑名单移除: {}", token);
        } catch (Exception e) {
            log.error("从黑名单移除Token失败", e);
        }
    }

    @Override
    public void addUserSession(String userId, String token) {
        try {
            String key = USER_SESSION_PREFIX + userId;
            redisTemplate.opsForValue().set(key, token, 24, TimeUnit.HOURS);
            log.info("用户会话已添加: userId={}, token={}", userId, token);
        } catch (Exception e) {
            log.error("添加用户会话失败", e);
        }
    }

    @Override
    public String getUserToken(String userId) {
        try {
            String key = USER_SESSION_PREFIX + userId;
            Object token = redisTemplate.opsForValue().get(key);
            return token != null ? token.toString() : null;
        } catch (Exception e) {
            log.error("获取用户Token失败", e);
            return null;
        }
    }

    @Override
    public void removeUserSession(String userId) {
        try {
            String key = USER_SESSION_PREFIX + userId;
            redisTemplate.delete(key);
            log.info("用户会话已移除: userId={}", userId);
        } catch (Exception e) {
            log.error("移除用户会话失败", e);
        }
    }

    @Override
    public void forceLogoutUser(String userId) {
        try {
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
}
