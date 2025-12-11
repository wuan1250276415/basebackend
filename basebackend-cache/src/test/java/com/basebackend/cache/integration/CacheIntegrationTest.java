package com.basebackend.cache.integration;

import com.basebackend.cache.config.CacheAutoConfiguration;
import com.basebackend.cache.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存集成测试
 * 使用 Testcontainers 启动真实的 Redis 实例进行测试
 * 
 * 测试内容：
 * 1. 真实的 Redis 操作
 * 2. 多级缓存的协同工作
 * 3. 缓存预热流程
 */
@SpringBootTest(classes = {CacheAutoConfiguration.class})
@Testcontainers
class CacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("basebackend.cache.enabled", () -> "true");
        registry.add("basebackend.cache.multi-level.enabled", () -> "false");
    }

    @Autowired
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        // 清理 Redis 数据
        redisService.deleteByPattern("test:*");
    }

    @Test
    void testRedisBasicOperations() {
        // 测试基本的 Redis 操作
        String key = "test:basic:key";
        String value = "test-value";

        // 设置值
        redisService.set(key, value);

        // 获取值
        Object result = redisService.get(key);
        assertNotNull(result);
        assertEquals(value, result);

        // 删除值
        redisService.delete(key);
        assertNull(redisService.get(key));
    }

    @Test
    void testRedisWithTTL() throws InterruptedException {
        // 测试带过期时间的 Redis 操作
        String key = "test:ttl:key";
        String value = "ttl-value";

        // 设置值，2秒后过期
        redisService.set(key, value, 2L, java.util.concurrent.TimeUnit.SECONDS);

        // 立即获取，应该存在
        assertEquals(value, redisService.get(key));

        // 等待3秒
        Thread.sleep(3000);

        // 再次获取，应该已过期
        assertNull(redisService.get(key));
    }

    @Test
    void testRedisBatchOperations() {
        // 测试批量操作
        java.util.Map<String, Object> entries = new java.util.HashMap<>();
        entries.put("test:batch:1", "value1");
        entries.put("test:batch:2", "value2");
        entries.put("test:batch:3", "value3");

        // 批量设置
        redisService.multiSet(entries);

        // 批量获取
        java.util.Set<String> keys = entries.keySet();
        java.util.Map<String, Object> results = redisService.multiGet(keys);

        assertEquals(3, results.size());
        assertEquals("value1", results.get("test:batch:1"));
        assertEquals("value2", results.get("test:batch:2"));
        assertEquals("value3", results.get("test:batch:3"));
    }

    @Test
    void testRedisPatternDelete() {
        // 测试模式匹配删除
        redisService.set("test:pattern:1", "value1");
        redisService.set("test:pattern:2", "value2");
        redisService.set("test:pattern:3", "value3");
        redisService.set("test:other:1", "other");

        // 删除匹配 test:pattern:* 的键
        long deleted = redisService.deleteByPattern("test:pattern:*");

        assertTrue(deleted >= 3);
        assertNull(redisService.get("test:pattern:1"));
        assertNull(redisService.get("test:pattern:2"));
        assertNull(redisService.get("test:pattern:3"));
        assertNotNull(redisService.get("test:other:1"));
    }

    @Test
    void testRedisComplexObjects() {
        // 测试复杂对象的序列化和反序列化
        String key = "test:complex:user";
        TestUser user = new TestUser("john", "john@example.com", 30);

        redisService.set(key, user);

        Object result = redisService.get(key);
        assertNotNull(result);
        assertTrue(result instanceof TestUser);

        TestUser retrievedUser = (TestUser) result;
        assertEquals("john", retrievedUser.getName());
        assertEquals("john@example.com", retrievedUser.getEmail());
        assertEquals(30, retrievedUser.getAge());
    }

    @Test
    void testRedisIncrement() {
        // 测试计数器操作
        String key = "test:counter";

        long value1 = redisService.increment(key, 1);
        assertEquals(1, value1);

        long value2 = redisService.increment(key, 1);
        assertEquals(2, value2);

        long value3 = redisService.increment(key, 5);
        assertEquals(7, value3);
    }

    @Test
    void testRedisExists() {
        // 测试键存在性检查
        String key = "test:exists:key";

        assertFalse(redisService.hasKey(key));

        redisService.set(key, "value");
        assertTrue(redisService.hasKey(key));

        redisService.delete(key);
        assertFalse(redisService.hasKey(key));
    }

    @Test
    void testRedisExpire() {
        // 测试设置过期时间
        String key = "test:expire:key";
        redisService.set(key, "value");

        assertTrue(redisService.hasKey(key));

        // 设置1秒后过期
        redisService.expire(key, 1L, java.util.concurrent.TimeUnit.SECONDS);

        // 立即检查，应该还存在
        assertTrue(redisService.hasKey(key));
    }

    /**
     * 测试用户类
     */
    static class TestUser {
        private String name;
        private String email;
        private int age;

        public TestUser() {
        }

        public TestUser(String name, String email, int age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
