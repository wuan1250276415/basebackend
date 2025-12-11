package com.basebackend.featuretoggle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseBackendFeatureToggle应用测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
class BaseBackendFeatureToggleApplicationTest {

    @Test
    void testBasicAssertions() {
        assertEquals(2, 1 + 1);
        assertNotNull(new Object());
        assertTrue(true);
    }

    @Test
    void testHashAlgorithmExists() {
        // 验证HashAlgorithm类存在并可调用
        long hash1 = com.basebackend.featuretoggle.abtest.HashAlgorithm.murmur3_32("test");
        long hash2 = com.basebackend.featuretoggle.abtest.HashAlgorithm.murmur3_32("test");
        assertEquals(hash1, hash2, "同一输入应产生相同哈希值");

        long hash3 = com.basebackend.featuretoggle.abtest.HashAlgorithm.murmur3_32("different");
        assertNotEquals(hash1, hash3, "不同输入应产生不同哈希值");
    }

    @Test
    void testFeatureContextCreation() {
        // 验证FeatureContext可以正常创建
        var context = com.basebackend.featuretoggle.model.FeatureContext.forUser("user123");
        assertNotNull(context);
        assertEquals("user123", context.getUserId());
    }
}
