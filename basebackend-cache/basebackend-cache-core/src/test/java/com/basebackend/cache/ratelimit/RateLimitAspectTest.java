package com.basebackend.cache.ratelimit;

import com.basebackend.cache.annotation.RateLimit;
import com.basebackend.cache.config.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RateLimitService rateLimitService;

    private SimpleMeterRegistry meterRegistry;
    private CacheProperties cacheProperties;
    private RateLimitAspect rateLimitAspect;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        cacheProperties = new CacheProperties();
        cacheProperties.getRateLimiter().setEnabled(true);
        cacheProperties.getRateLimiter().setFailOpen(true);
        rateLimitAspect = new RateLimitAspect(rateLimitService, cacheProperties, meterRegistry);
    }

    @Test
    void shouldRecordAllowedMetricUsingKeyScopeTag() throws Throwable {
        ProceedingJoinPoint joinPoint = buildJoinPoint("user-1");
        Method method = TestService.class.getMethod("execute", String.class);
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        when(rateLimitService.tryAcquire("api:user:user-1", 5, 60, rateLimit.timeUnit(), rateLimit.mode()))
                .thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        assertEquals("ok", result);
        Counter counter = meterRegistry.get("cache.ratelimit.acquired")
                .tag("key_scope", "api")
                .tag("result", "allowed")
                .counter();
        assertEquals(1.0, counter.count());
        assertNull(meterRegistry.find("cache.ratelimit.acquired").tag("key", "api:user:user-1").counter());
    }

    @Test
    void shouldRecordFailOpenMetricUsingKeyScopeTag() throws Throwable {
        ProceedingJoinPoint joinPoint = buildJoinPoint("user-2");
        Method method = TestService.class.getMethod("execute", String.class);
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        when(rateLimitService.tryAcquire("api:user:user-2", 5, 60, rateLimit.timeUnit(), rateLimit.mode()))
                .thenThrow(new RuntimeException("redis down"));
        when(joinPoint.proceed()).thenReturn("fallback");

        Object result = rateLimitAspect.handleRateLimit(joinPoint, rateLimit);

        assertEquals("fallback", result);
        Counter counter = meterRegistry.get("cache.ratelimit.failopen")
                .tag("key_scope", "api")
                .counter();
        assertEquals(1.0, counter.count());
    }

    private ProceedingJoinPoint buildJoinPoint(String userId) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestService.class.getMethod("execute", String.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{userId});

        return joinPoint;
    }

    static class TestService {
        @RateLimit(key = "'api:user:' + #userId", rate = 5, interval = 60)
        public String execute(String userId) {
            return userId;
        }
    }
}
