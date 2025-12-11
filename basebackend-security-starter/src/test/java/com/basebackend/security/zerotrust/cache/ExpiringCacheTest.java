package com.basebackend.security.zerotrust.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpiringCache 单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("ExpiringCache 单元测试")
class ExpiringCacheTest {

    @Test
    @DisplayName("基本put和get操作")
    void basicPutAndGet() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);

        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    @DisplayName("获取不存在的key返回null")
    void getNonExistentKeyReturnsNull() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);

        assertNull(cache.get("nonExistentKey"));
    }

    @Test
    @DisplayName("remove删除条目")
    void removeDeletesEntry() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);
        cache.put("key1", "value1");
        cache.remove("key1");

        assertNull(cache.get("key1"));
    }

    @Test
    @DisplayName("size返回正确数量")
    void sizeReturnsCorrectCount() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("clear清空缓存")
    void clearEmptiesCache() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);
        cache.put("key1", "value1");
        cache.clear();

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("更新已存在的key覆盖值")
    void putExistingKeyOverwritesValue() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);
        cache.put("key1", "value1");
        cache.put("key1", "newValue");

        assertEquals("newValue", cache.get("key1"));
    }

    @Test
    @DisplayName("cleanup不影响未过期条目")
    void cleanupDoesNotAffectNonExpiredEntries() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);
        cache.put("key1", "value1");
        cache.cleanup();

        assertEquals(1, cache.size());
        assertNotNull(cache.get("key1"));
    }

    @Test
    @DisplayName("多次put同一个key")
    void multiplePutsSameKey() {
        RiskDataCacheManager.ExpiringCache<String, String> cache = new RiskDataCacheManager.ExpiringCache<>(100, 24);

        cache.put("key1", "v1");
        cache.put("key1", "v2");
        cache.put("key1", "v3");

        assertEquals("v3", cache.get("key1"));
        assertEquals(1, cache.size());
    }
}
