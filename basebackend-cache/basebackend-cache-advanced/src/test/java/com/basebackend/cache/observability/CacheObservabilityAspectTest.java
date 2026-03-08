package com.basebackend.cache.observability;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheObservabilityAspectTest {

    @Test
    void shouldExtractLowCardinalityKeyNamespace() throws Exception {
        CacheObservabilityAspect aspect = new CacheObservabilityAspect(ObservationRegistry.NOOP);
        Method extractMethod = CacheObservabilityAspect.class.getDeclaredMethod("extractKeyNamespace", String.class);
        extractMethod.setAccessible(true);

        assertEquals("order", extractMethod.invoke(aspect, "order:123"));
        assertEquals("rate", extractMethod.invoke(aspect, "rate|user|123"));
        assertEquals("unknown", extractMethod.invoke(aspect, " "));
    }
}
