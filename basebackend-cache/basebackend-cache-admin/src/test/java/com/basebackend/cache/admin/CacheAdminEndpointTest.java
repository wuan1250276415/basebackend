package com.basebackend.cache.admin;

import com.basebackend.cache.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CacheAdminEndpoint 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CacheAdminEndpointTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CacheAdminEndpoint endpoint;

    @Test
    @DisplayName("clearCache 非法名称返回 0")
    void shouldReturnZeroWhenClearCacheNameInvalid() {
        when(cacheService.validateCacheName("bad name")).thenReturn(false);

        long result = endpoint.clearCache("bad name");

        assertThat(result).isZero();
        verify(cacheService).validateCacheName("bad name");
        verify(cacheService, never()).clearCache(anyString());
    }
}
