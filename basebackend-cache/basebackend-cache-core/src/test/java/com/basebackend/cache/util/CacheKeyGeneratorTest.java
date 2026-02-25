package com.basebackend.cache.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheKeyGenerator 单元测试
 */
class CacheKeyGeneratorTest {
    
    private CacheKeyGenerator keyGenerator;
    
    @BeforeEach
    void setUp() {
        keyGenerator = new CacheKeyGenerator();
    }
    
    @Test
    void testGenerateSimpleKey() {
        String key = keyGenerator.generateSimpleKey("cache", "user", "123");
        assertEquals("cache:user:123", key);
    }
    
    @Test
    void testGenerateSimpleKeyWithEmptyPrefix() {
        String key = keyGenerator.generateSimpleKey("", "user", "123");
        assertEquals("user:123", key);
    }
    
    @Test
    void testGenerateSimpleKeyWithEmptyCacheName() {
        String key = keyGenerator.generateSimpleKey("cache", "", "123");
        assertEquals("cache:123", key);
    }
    
    @Test
    void testGeneratePatternKey() {
        String pattern = keyGenerator.generatePatternKey("cache", "UserService");
        assertEquals("cache:UserService:*", pattern);
    }
    
    @Test
    void testGeneratePatternKeyWithEmptyPrefix() {
        String pattern = keyGenerator.generatePatternKey("", "UserService");
        assertEquals("UserService:*", pattern);
    }
    
    @Test
    void testGenerateHashKey() {
        String shortKey = "short_key";
        String result = keyGenerator.generateHashKey(shortKey);
        assertEquals(shortKey, result); // 短键不需要哈希
        
        // 生成一个超过 200 字符的长键
        String longKey = "a".repeat(250);
        String hashedKey = keyGenerator.generateHashKey(longKey);
        assertNotEquals(longKey, hashedKey);
        assertTrue(hashedKey.contains(":")); // 包含哈希分隔符
    }
    
    @Test
    void testGenerateHashKeyWithCustomLength() {
        String key = "a".repeat(150);
        String hashedKey = keyGenerator.generateHashKey(key, 100);
        assertNotEquals(key, hashedKey);
        assertTrue(hashedKey.length() < key.length());
    }
    
    @Test
    void testGenerateSHA256Key() {
        String key = "test_key";
        String sha256Key = keyGenerator.generateSHA256Key(key);
        assertNotNull(sha256Key);
        assertEquals(64, sha256Key.length()); // SHA-256 产生 64 个十六进制字符
    }
    
    @Test
    void testGenerateJsonKey() {
        Map<String, Object> obj = new HashMap<>();
        obj.put("name", "张三");
        obj.put("age", 25);
        
        String jsonKey = keyGenerator.generateJsonKey("cache", "user", obj);
        assertNotNull(jsonKey);
        assertTrue(jsonKey.startsWith("cache:user:"));
    }
    
    @Test
    void testGenerateVersionedKey() {
        String key = keyGenerator.generateVersionedKey("cache", "api", "data", "1.0");
        assertEquals("cache:api:v1.0:data", key);
    }
    
    @Test
    void testGenerateNamespacedKey() {
        String key = keyGenerator.generateNamespacedKey("prod", "cache", "user", "123");
        assertEquals("prod:cache:user:123", key);
    }
    
    @Test
    void testGenerateTenantKey() {
        String key = keyGenerator.generateTenantKey("tenant001", "cache", "user", "123");
        assertEquals("tenant:tenant001:cache:user:123", key);
    }
    
    @Test
    void testGenerateCollectionKey() {
        String key = keyGenerator.generateCollectionKey("cache", "users", "activeUsers");
        assertEquals("cache:users:collection:activeUsers", key);
    }
    
    @Test
    void testGeneratePageKey() {
        String key = keyGenerator.generatePageKey("cache", "userList", 1, 20);
        assertEquals("cache:userList:page:1_20", key);
    }
    
    @Test
    void testGeneratePageKeyWithQueryParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", "active");
        params.put("role", "admin");
        
        String key = keyGenerator.generatePageKey("cache", "userList", 1, 20, params);
        assertTrue(key.contains("cache:userList:page:1_20"));
        assertTrue(key.contains("role=admin"));
        assertTrue(key.contains("status=active"));
    }
    
    @Test
    void testGenerateListKey() {
        String key = keyGenerator.generateListKey("cache", "orders", "recentOrders");
        assertEquals("cache:orders:list:recentOrders", key);
    }
    
    @Test
    void testGenerateMapKey() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("city", "北京");
        params.put("age", 25);
        params.put("gender", "male");
        
        String mapKey = keyGenerator.generateMapKey(params);
        // 应该按键排序
        assertEquals("age=25&city=北京&gender=male", mapKey);
    }
    
    @Test
    void testGenerateMapKeyWithEmptyMap() {
        String mapKey = keyGenerator.generateMapKey(new HashMap<>());
        assertEquals("empty", mapKey);
        
        String nullMapKey = keyGenerator.generateMapKey(null);
        assertEquals("empty", nullMapKey);
    }
    
    @Test
    void testGenerateTimestampKey() {
        long timestamp = System.currentTimeMillis();
        String key = keyGenerator.generateTimestampKey("cache", "report", "daily", timestamp);
        assertEquals("cache:report:daily:" + timestamp, key);
    }
    
    @Test
    void testGenerateTTLKey() {
        String key = keyGenerator.generateTTLKey("cache", "session", "sessionId123", 3600);
        assertEquals("cache:session:ttl3600:sessionId123", key);
    }
    
    @Test
    void testParseKey() {
        String key = "cache:user:v1.0:123";
        List<String> parts = keyGenerator.parseKey(key);
        
        assertEquals(4, parts.size());
        assertEquals("cache", parts.get(0));
        assertEquals("user", parts.get(1));
        assertEquals("v1.0", parts.get(2));
        assertEquals("123", parts.get(3));
    }
    
    @Test
    void testParseEmptyKey() {
        List<String> parts = keyGenerator.parseKey("");
        assertTrue(parts.isEmpty());
        
        List<String> nullParts = keyGenerator.parseKey(null);
        assertTrue(nullParts.isEmpty());
    }
    
    @Test
    void testExtractPrefix() {
        String key = "cache:user:123";
        String prefix = keyGenerator.extractPrefix(key);
        assertEquals("cache", prefix);
    }
    
    @Test
    void testExtractCacheName() {
        String key = "cache:user:123";
        String cacheName = keyGenerator.extractCacheName(key);
        assertEquals("user", cacheName);
    }
    
    @Test
    void testIsValidKey() {
        assertTrue(keyGenerator.isValidKey("cache:user:123"));
        assertFalse(keyGenerator.isValidKey("")); // 空键
        assertFalse(keyGenerator.isValidKey(null)); // null 键
        assertFalse(keyGenerator.isValidKey("cache user 123")); // 包含空格
        assertFalse(keyGenerator.isValidKey("cache:user\n123")); // 包含换行
        assertFalse(keyGenerator.isValidKey("a".repeat(1025))); // 过长
    }
    
    @Test
    void testSanitizeKey() {
        String key = "cache: user :123";
        String sanitized = keyGenerator.sanitizeKey(key);
        assertEquals("cache:_user_:123", sanitized);
        
        String keyWithNewline = "cache:user\n123";
        String sanitizedNewline = keyGenerator.sanitizeKey(keyWithNewline);
        assertEquals("cache:user_123", sanitizedNewline);
    }
    
    @Test
    void testSanitizeLongKey() {
        String longKey = "cache:" + "a".repeat(250);
        String sanitized = keyGenerator.sanitizeKey(longKey);
        assertNotEquals(longKey, sanitized);
        assertTrue(sanitized.length() < longKey.length());
    }
    
    @Test
    void testGenerateBatchKeys() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        List<String> keys = keyGenerator.generateBatchKeys("cache", "user", ids);
        
        assertEquals(5, keys.size());
        assertEquals("cache:user:1", keys.get(0));
        assertEquals("cache:user:2", keys.get(1));
        assertEquals("cache:user:3", keys.get(2));
        assertEquals("cache:user:4", keys.get(3));
        assertEquals("cache:user:5", keys.get(4));
    }
    
    @Test
    void testGenerateRangePatternKey() {
        String pattern = keyGenerator.generateRangePatternKey(
            "cache", "orders", "2024-01-01", "2024-12-31");
        assertEquals("cache:orders:range:2024-01-01_2024-12-31:*", pattern);
    }
    
    @Test
    void testKeyConsistency() {
        // 相同的参数应该生成相同的键
        Map<String, Object> params1 = new LinkedHashMap<>();
        params1.put("a", 1);
        params1.put("b", 2);
        
        Map<String, Object> params2 = new LinkedHashMap<>();
        params2.put("a", 1);
        params2.put("b", 2);
        
        String key1 = keyGenerator.generateMapKey(params1);
        String key2 = keyGenerator.generateMapKey(params2);
        
        assertEquals(key1, key2);
    }
    
    @Test
    void testKeyUniqueness() {
        // 不同的参数应该生成不同的键
        String key1 = keyGenerator.generateSimpleKey("cache", "user", "123");
        String key2 = keyGenerator.generateSimpleKey("cache", "user", "456");
        
        assertNotEquals(key1, key2);
    }
    
    @Test
    void testVersionedKeyIsolation() {
        // 不同版本的键应该是隔离的
        String v1Key = keyGenerator.generateVersionedKey("cache", "api", "data", "1.0");
        String v2Key = keyGenerator.generateVersionedKey("cache", "api", "data", "2.0");
        
        assertNotEquals(v1Key, v2Key);
    }
    
    @Test
    void testTenantKeyIsolation() {
        // 不同租户的键应该是隔离的
        String tenant1Key = keyGenerator.generateTenantKey("tenant001", "cache", "user", "123");
        String tenant2Key = keyGenerator.generateTenantKey("tenant002", "cache", "user", "123");
        
        assertNotEquals(tenant1Key, tenant2Key);
    }
}
