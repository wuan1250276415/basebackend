package com.basebackend.cache.ratelimit;

import com.basebackend.cache.config.CacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter rateLimiter;

    private RateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.getRateLimiter().setKeyPrefix("ratelimit:");
        rateLimitService = new RateLimitServiceImpl(redissonClient, cacheProperties);
    }

    @Test
    void tryAcquireUsesDurationBasedRateConfiguration() {
        when(redissonClient.getRateLimiter("ratelimit:login")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(2L)).thenReturn(true);

        boolean acquired = rateLimitService.tryAcquire("login", 2, 5, 60, TimeUnit.SECONDS, RateType.OVERALL);

        assertTrue(acquired);
        verify(rateLimiter).trySetRate(RateType.OVERALL, 5, Duration.ofSeconds(60));
        verify(rateLimiter).tryAcquire(2L);
    }

    @Test
    void tryAcquireSupportsSubSecondIntervals() {
        when(redissonClient.getRateLimiter("ratelimit:burst")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(1L)).thenReturn(true);

        boolean acquired = rateLimitService.tryAcquire("burst", 1, 20, 250, TimeUnit.MILLISECONDS, RateType.PER_CLIENT);

        assertTrue(acquired);
        verify(rateLimiter).trySetRate(RateType.PER_CLIENT, 20, Duration.ofMillis(250));
        verify(rateLimiter).tryAcquire(1L);
    }

    @Test
    void availablePermitsUsesPrefixedKey() {
        when(redissonClient.getRateLimiter("ratelimit:jobs")).thenReturn(rateLimiter);
        when(rateLimiter.availablePermits()).thenReturn(7L);

        long permits = rateLimitService.availablePermits("jobs");

        assertEquals(7L, permits);
        verify(redissonClient).getRateLimiter("ratelimit:jobs");
    }

    @Test
    void deleteRateLimiterUsesPrefixedKey() {
        when(redissonClient.getRateLimiter("ratelimit:jobs")).thenReturn(rateLimiter);

        rateLimitService.deleteRateLimiter("jobs");

        verify(redissonClient).getRateLimiter("ratelimit:jobs");
        verify(rateLimiter).delete();
    }
}
