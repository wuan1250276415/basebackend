package com.basebackend.cache.admin;

import com.basebackend.cache.service.CacheService;
import com.basebackend.cache.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CacheAdminController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CacheAdminControllerTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisService redisService;

    private CacheAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new CacheAdminController(cacheService, redisService);
    }

    @Nested
    @DisplayName("非法入参拦截")
    class InvalidInputGuardTest {

        @Test
        @DisplayName("getCacheDetail 非法缓存名返回空详情")
        void shouldRejectInvalidCacheNameForDetail() {
            when(cacheService.validateCacheName("bad name")).thenReturn(false);

            var result = controller.getCacheDetail("bad name");

            assertThat(result.name()).isEqualTo("bad name");
            assertThat(result.size()).isEqualTo(0);
            assertThat(result.sampleKeys()).isEmpty();
            verify(cacheService).validateCacheName("bad name");
            verify(cacheService, never()).getStatistics(anyString());
            verify(cacheService, never()).getCacheSize(anyString());
            verify(cacheService, never()).keys(anyString());
        }

        @Test
        @DisplayName("getCacheKeys 非法 pattern 返回空集合")
        void shouldRejectUnsafePatternForKeys() {
            when(cacheService.validateCacheName("userCache")).thenReturn(true);

            Set<String> result = controller.getCacheKeys("userCache", "bad pattern");

            assertThat(result).isEmpty();
            verify(cacheService).validateCacheName("userCache");
            verify(cacheService, never()).keys(anyString());
        }
    }

    @Test
    @DisplayName("clear-all confirmed=false 时拦截")
    void shouldBlockClearAllWhenNotConfirmed() {
        Map<String, Object> result = controller.clearAllCaches(false);

        assertThat(result).containsEntry("error", "Must set confirmed=true to clear all caches");
        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("getCacheKeys 返回结果限制为 100 个")
    void shouldLimitReturnedKeys() {
        when(cacheService.validateCacheName("userCache")).thenReturn(true);
        LinkedHashSet<String> keys = IntStream.range(0, 120)
                .collect(LinkedHashSet::new, (set, index) -> set.add("userCache:key-" + index), Set::addAll);
        when(cacheService.keys("userCache:*")).thenReturn(keys);

        Set<String> result = controller.getCacheKeys("userCache", "*");

        assertThat(result).hasSize(100);
        assertThat(keys).containsAll(result);
        verify(cacheService).keys("userCache:*");
    }

    @Test
    @DisplayName("health 返回 redisAvailable 与 circuitBreakerState")
    void shouldReturnHealthFields() {
        when(redisService.isRedisAvailable()).thenReturn(true);
        when(redisService.getCircuitBreakerState()).thenReturn("CLOSED");

        Map<String, Object> result = controller.health();

        assertThat(result).containsEntry("redisAvailable", true);
        assertThat(result).containsEntry("circuitBreakerState", "CLOSED");
        assertThat(result).containsOnlyKeys("redisAvailable", "circuitBreakerState");
    }
}
