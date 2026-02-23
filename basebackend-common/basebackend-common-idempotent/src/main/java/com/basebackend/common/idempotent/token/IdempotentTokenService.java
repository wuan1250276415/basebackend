package com.basebackend.common.idempotent.token;

import com.basebackend.common.idempotent.config.IdempotentProperties;
import com.basebackend.common.idempotent.exception.IdempotentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 幂等 Token 服务
 * <p>
 * 提供 Token 的生成与验证功能。
 * Token 生成后存入 Redis（或内存），客户端携带 Token 请求时进行验证。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class IdempotentTokenService {

    private static final String TOKEN_PREFIX = "idempotent:token:";

    private final StringRedisTemplate redisTemplate;
    private final IdempotentProperties properties;

    public IdempotentTokenService(StringRedisTemplate redisTemplate, IdempotentProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 生成幂等 Token
     *
     * @return 幂等 Token 字符串
     */
    public String generateToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", properties.getTokenTimeout(), TimeUnit.SECONDS);
        log.debug("生成幂等Token: {}", token);
        return token;
    }

    /**
     * 验证并消费幂等 Token
     * <p>
     * 验证成功后 Token 立即失效（一次性使用）。
     * </p>
     *
     * @param token 幂等 Token
     * @return true 表示验证通过（首次使用），false 表示 Token 无效或已使用
     */
    public boolean validateAndConsume(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String key = TOKEN_PREFIX + token;
        Boolean deleted = redisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted);
    }
}
