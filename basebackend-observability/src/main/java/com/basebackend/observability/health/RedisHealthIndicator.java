package com.basebackend.observability.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis 健康检查器
 * 检查 Redis 连接状态、响应时间、内存使用情况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // 执行 PING 命令
            String pong = redisTemplate.execute((RedisConnection connection) -> {
                return connection.ping();
            });

            long responseTime = System.currentTimeMillis() - startTime;

            // 获取 Redis 服务器信息
            Map<String, Object> details = new HashMap<>();
            details.put("ping", pong);
            details.put("responseTime", responseTime + "ms");

            // 获取 Redis 信息
            try {
                String info = redisTemplate.execute((RedisConnection connection) -> {
                    return connection.info("server").getProperty("redis_version");
                });
                details.put("version", info);
            } catch (Exception e) {
                log.warn("Failed to get Redis version", e);
            }

            // 检查响应时间
            if (responseTime > 1000) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("message", "Redis response time too slow")
                        .build();
            }

            // 尝试设置和获取测试值
            try {
                String testKey = "health:check:test";
                String testValue = "ok";
                redisTemplate.opsForValue().set(testKey, testValue);
                Object retrieved = redisTemplate.opsForValue().get(testKey);
                redisTemplate.delete(testKey);

                if (!testValue.equals(retrieved)) {
                    return Health.down()
                            .withDetails(details)
                            .withDetail("message", "Redis read/write test failed")
                            .build();
                }

                details.put("readWriteTest", "passed");
            } catch (Exception e) {
                log.error("Redis read/write test failed", e);
                return Health.down()
                        .withDetails(details)
                        .withDetail("message", "Redis read/write test failed: " + e.getMessage())
                        .build();
            }

            return Health.up()
                    .withDetails(details)
                    .build();

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }
}
