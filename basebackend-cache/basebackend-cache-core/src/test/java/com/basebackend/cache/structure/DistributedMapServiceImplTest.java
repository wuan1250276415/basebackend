package com.basebackend.cache.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedMapServiceImplTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RMap<Object, Object> map;

    @Mock
    private RMapCache<Object, Object> mapCache;

    private DistributedMapServiceImpl distributedMapService;

    @BeforeEach
    void setUp() {
        distributedMapService = new DistributedMapServiceImpl(redissonClient);
    }

    @Test
    void putWithoutTtlShouldUseRMapPut() {
        when(redissonClient.getMap("orders")).thenReturn(map);

        distributedMapService.put("orders", "id-1", "value-1");

        verify(map).put("id-1", "value-1");
    }

    @Test
    void putWithTtlShouldUseEntryTtlOnMapCache() {
        when(redissonClient.getMapCache("orders")).thenReturn(mapCache);

        distributedMapService.put("orders", "id-1", "value-1", 30, TimeUnit.SECONDS);

        verify(mapCache).fastPut("id-1", "value-1", 30, TimeUnit.SECONDS);
    }
}
