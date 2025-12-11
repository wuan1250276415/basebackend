package com.basebackend.security.service;

import com.basebackend.jwt.JwtUtil;
import com.basebackend.security.exception.TokenBlacklistException;
import com.basebackend.security.service.impl.TokenBlacklistServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Token黑名单服务测试
 * 测试令牌黑名单管理功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistServiceImpl Token黑名单服务测试")
class TokenBlacklistServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    @Test
    @DisplayName("添加Token到黑名单成功")
    void shouldAddTokenToBlacklist() {
        // Given
        String token = "test-token-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 模拟JWT过期时间，返回24小时后过期
        long futureTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(new Date(futureTime));

        // When
        tokenBlacklistService.addToBlacklist(token);

        // Then
        verify(redisTemplate, times(1)).opsForValue();
        // 验证键使用哈希格式（SHA-256 hash）
        verify(valueOperations, times(1)).set(
            argThat(key -> key.startsWith("token:blacklist:") && ((String) key).length() == "token:blacklist:".length() + 64),
            eq("1"),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("检查Token在黑名单中")
    void shouldCheckTokenIsBlacklisted() {
        // Given
        String token = "test-token-456";
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn("1");
        // 由于使用哈希键，我们只验证hasKey被调用，不检查具体键值
        when(redisTemplate.hasKey(startsWith("token:blacklist:"))).thenReturn(true);

        // When
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(redisTemplate, times(1)).hasKey(startsWith("token:blacklist:"));
    }

    @Test
    @DisplayName("检查Token不在黑名单中")
    void shouldCheckTokenIsNotBlacklisted() {
        // Given
        String token = "test-token-789";
        when(redisTemplate.hasKey(startsWith("token:blacklist:"))).thenReturn(false);

        // When
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(redisTemplate, times(1)).hasKey(startsWith("token:blacklist:"));
    }

    @Test
    @DisplayName("从黑名单移除Token")
    void shouldRemoveTokenFromBlacklist() {
        // Given
        String token = "test-token-abc";

        // When
        tokenBlacklistService.removeFromBlacklist(token);

        // Then - 验证使用哈希键
        verify(redisTemplate, times(1)).delete(startsWith("token:blacklist:"));
    }

    @Test
    @DisplayName("添加用户会话")
    void shouldAddUserSession() {
        // Given
        String userId = "user123";
        String token = "test-token-xyz";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.addUserSession(userId, token);

        // Then
        verify(valueOperations, times(1)).set(
            eq("user:session:user123"),
            eq("test-token-xyz"),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("获取用户Token - 存在")
    void shouldGetUserTokenWhenExists() {
        // Given
        String userId = "user456";
        String token = "test-token-def";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:session:user456")).thenReturn(token);

        // When
        String result = tokenBlacklistService.getUserToken(userId);

        // Then
        assertThat(result).isEqualTo(token);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get("user:session:user456");
    }

    @Test
    @DisplayName("获取用户Token - 不存在")
    void shouldGetUserTokenWhenNotExists() {
        // Given
        String userId = "user789";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:session:user789")).thenReturn(null);

        // When
        String result = tokenBlacklistService.getUserToken(userId);

        // Then
        assertThat(result).isNull();
        verify(valueOperations, times(1)).get("user:session:user789");
    }

    @Test
    @DisplayName("移除用户会话")
    void shouldRemoveUserSession() {
        // Given
        String userId = "user000";

        // When
        tokenBlacklistService.removeUserSession(userId);

        // Then
        verify(redisTemplate, times(1)).delete("user:session:user000");
    }

    @Test
    @DisplayName("强制用户下线 - 有当前Token")
    void shouldForceLogoutUserWithToken() {
        // Given
        String userId = "user111";
        String token = "test-token-ghi";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:session:user111")).thenReturn(token);
        // 模拟JWT过期时间
        long futureTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(new Date(futureTime));

        // When
        tokenBlacklistService.forceLogoutUser(userId);

        // Then
        // 应该调用addToBlacklist和removeUserSession
        verify(redisTemplate, times(2)).opsForValue(); // getUserToken 和 addToBlacklist
        verify(redisTemplate, times(1)).delete("user:session:user111"); // removeUserSession
        verify(valueOperations, times(1)).set(
            startsWith("token:blacklist:"), // 验证使用哈希键
            eq("1"),
            eq(24L),
            eq(TimeUnit.HOURS)
        ); // addToBlacklist
    }

    @Test
    @DisplayName("强制用户下线 - 无当前Token")
    void shouldForceLogoutUserWithoutToken() {
        // Given
        String userId = "user222";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:session:user222")).thenReturn(null);

        // When
        tokenBlacklistService.forceLogoutUser(userId);

        // Then
        // 只需要移除会话，不需要加入黑名单
        verify(redisTemplate, times(1)).opsForValue(); // getUserToken
        verify(redisTemplate, times(1)).delete("user:session:user222"); // removeUserSession
        // 不应该调用set方法（addToBlacklist）
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("异常处理 - 添加到黑名单失败")
    void shouldHandleAddToBlacklistFailure() {
        // Given
        String token = "test-token-error";
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        // When - 不应抛出异常
        tokenBlacklistService.addToBlacklist(token);

        // Then - 应该捕获异常并记录日志
        verify(redisTemplate, times(1)).opsForValue();
    }

    @Test
    @DisplayName("异常处理 - 检查黑名单失败")
    void shouldHandleCheckBlacklistFailure() {
        // Given
        String token = "test-token-error";
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When & Then - 应该抛出TokenBlacklistException而不是返回false
        assertThatThrownBy(() -> tokenBlacklistService.isBlacklisted(token))
            .isInstanceOf(TokenBlacklistException.class)
            .hasMessageContaining("检查Token黑名单失败");
        verify(redisTemplate, times(1)).hasKey(startsWith("token:blacklist:"));
    }

    @Test
    @DisplayName("异常处理 - 获取用户Token失败")
    void shouldHandleGetUserTokenFailure() {
        // Given
        String userId = "user-error";
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis error"));

        // When
        String result = tokenBlacklistService.getUserToken(userId);

        // Then - 应该返回null而不是抛出异常
        assertThat(result).isNull();
        verify(redisTemplate, times(1)).opsForValue();
    }

    @Test
    @DisplayName("异常处理 - 强制下线失败")
    void shouldHandleForceLogoutFailure() {
        // Given
        String userId = "user-error";
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis error"));

        // When - 不应抛出异常
        tokenBlacklistService.forceLogoutUser(userId);

        // Then - 应该捕获异常并记录日志
        verify(redisTemplate, times(1)).opsForValue();
    }

    @Test
    @DisplayName("会话键格式正确")
    void shouldUseCorrectSessionKeyFormat() {
        // Given
        String userId = "user-123";
        String token = "test-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistService.addUserSession(userId, token);

        // Then - 验证键的格式
        verify(valueOperations).set(
            eq("user:session:user-123"),
            any(),
            anyLong(),
            any(TimeUnit.class)
        );
    }

    @Test
    @DisplayName("空Token处理")
    void shouldHandleEmptyToken() {
        // When
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted("");
        tokenBlacklistService.addToBlacklist("");
        tokenBlacklistService.removeFromBlacklist("");

        // Then - 应该跳过处理，不调用Redis
        assertThat(isBlacklisted).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("null Token处理")
    void shouldHandleNullToken() {
        // When - 不应抛出异常
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(null);
        tokenBlacklistService.addToBlacklist(null);
        tokenBlacklistService.removeFromBlacklist(null);

        // Then - 应该正常处理null值
        assertThat(isBlacklisted).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("包含特殊字符的Token处理")
    void shouldHandleTokenWithSpecialCharacters() {
        // Given
        String token = "token!@#$%^&*()";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long futureTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(new Date(futureTime));

        // When
        tokenBlacklistService.addToBlacklist(token);

        // Then - 应该正确处理特殊字符（使用哈希键）
        verify(valueOperations).set(
            startsWith("token:blacklist:"),
            any(),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("Redis异常时抛出TokenBlacklistException")
    void shouldThrowExceptionWhenRedisFails() {
        // Given
        String token = "test-token";
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertThatThrownBy(() -> tokenBlacklistService.isBlacklisted(token))
            .isInstanceOf(TokenBlacklistException.class)
            .hasMessageContaining("检查Token黑名单失败");
    }

    @Test
    @DisplayName("动态TTL - 短过期时间")
    void shouldUseDynamicTtlForShortExpiration() {
        // Given
        String token = "short-lived-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Token将在2小时后过期
        long futureTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000;
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(new Date(futureTime));

        // When
        tokenBlacklistService.addToBlacklist(token);

        // Then
        verify(valueOperations).set(
            startsWith("token:blacklist:"),
            eq("1"),
            eq(2L), // 2小时TTL
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("动态TTL - 长过期时间")
    void shouldUseDynamicTtlForLongExpiration() {
        // Given
        String token = "long-lived-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Token将在30小时后过期（超过默认24小时）
        long futureTime = System.currentTimeMillis() + 30 * 60 * 60 * 1000;
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(new Date(futureTime));

        // When
        tokenBlacklistService.addToBlacklist(token);

        // Then - 限制为默认TTL（24小时）
        verify(valueOperations).set(
            startsWith("token:blacklist:"),
            eq("1"),
            eq(24L), // 限制为24小时
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("JWT解析失败时使用默认TTL")
    void shouldUseDefaultTtlWhenJwtParsingFails() {
        // Given
        String token = "invalid-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // JWT解析失败
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(null);

        // When
        tokenBlacklistService.addToBlacklist(token);

        // Then - 使用默认TTL
        verify(valueOperations).set(
            startsWith("token:blacklist:"),
            eq("1"),
            eq(24L), // 默认TTL
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    @DisplayName("用户会话使用动态TTL")
    void shouldUseDynamicTtlForUserSession() {
        // Given
        String userId = "user123";
        String token = "session-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long futureTime = System.currentTimeMillis() + 12 * 60 * 60 * 1000; // 12小时后过期
        when(jwtUtil.getExpirationDateFromToken(token)).thenReturn(new Date(futureTime));

        // When
        tokenBlacklistService.addUserSession(userId, token);

        // Then
        verify(valueOperations).set(
            eq("user:session:user123"),
            eq("session-token"),
            eq(12L), // 12小时TTL
            eq(TimeUnit.HOURS)
        );
    }
}
