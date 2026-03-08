package com.basebackend.cache.hook;

import com.basebackend.cache.hotkey.HotKeyDetector;
import com.basebackend.cache.hotkey.HotKeyMitigator;
import com.basebackend.cache.invalidation.CacheInvalidationPublisher;
import com.basebackend.cache.refresh.NearExpiryRefreshManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvancedCacheOperationHookTest {

    @Mock
    private HotKeyDetector hotKeyDetector;

    @Mock
    private HotKeyMitigator hotKeyMitigator;

    @Mock
    private NearExpiryRefreshManager nearExpiryRefreshManager;

    @Mock
    private CacheInvalidationPublisher cacheInvalidationPublisher;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private AdvancedCacheOperationHook hook;

    @BeforeEach
    void setUp() {
        hook = new AdvancedCacheOperationHook(
                hotKeyDetector,
                hotKeyMitigator,
                nearExpiryRefreshManager,
                cacheInvalidationPublisher);
    }

    @Test
    void beforeCacheLookupShouldRecordAccessAndReadMitigationCache() {
        when(hotKeyMitigator.get("order:1")).thenReturn("cached-value");

        Object value = hook.beforeCacheLookup("order", "order:1", 60, joinPoint);

        assertEquals("cached-value", value);
        verify(hotKeyDetector).recordAccess("order:1");
        verify(hotKeyMitigator).get("order:1");
    }

    @Test
    void afterCacheHitShouldTriggerMitigationAndRefresh() {
        hook.afterCacheHit("order", "order:1", "value", 120, joinPoint);

        verify(hotKeyMitigator).onCacheHit("order:1", "value");
        verify(nearExpiryRefreshManager).checkAndRefresh("order:1", 120, "order", joinPoint);
    }

    @Test
    void afterCachePutShouldInvalidateMitigationAndPublishEvict() {
        hook.afterCachePut("order", "order:1", "value");

        verify(hotKeyMitigator).invalidate("order:1");
        verify(cacheInvalidationPublisher).publishEvict("order", "order:1");
    }

    @Test
    void afterCacheClearWithEmptyNameShouldPublishClearAll() {
        hook.afterCacheClear("");

        verify(hotKeyMitigator).invalidateAll();
        verify(cacheInvalidationPublisher).publishClearAll();
    }
}
