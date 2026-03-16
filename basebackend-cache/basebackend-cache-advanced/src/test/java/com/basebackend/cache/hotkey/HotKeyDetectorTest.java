package com.basebackend.cache.hotkey;

import com.basebackend.cache.config.CacheProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HotKeyDetectorTest {

    private ScheduledExecutorService scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void shouldForceTrimWhenWindowFarBeyondCapacity() throws Exception {
        CacheProperties properties = new CacheProperties();
        properties.getHotKey().setTopK(2); // maxSize = 20
        scheduler = Executors.newSingleThreadScheduledExecutor();
        HotKeyDetector detector = new HotKeyDetector(properties, scheduler, null);

        for (int i = 0; i < 80; i++) {
            detector.recordAccess("cold-" + i);
        }

        Map<?, ?> currentWindow = getCurrentWindow(detector);
        assertTrue(currentWindow.size() <= 40);
    }

    @Test
    void shouldPreferKeepingFrequentKeysWhenTrim() throws Exception {
        CacheProperties properties = new CacheProperties();
        properties.getHotKey().setTopK(2); // maxSize = 20
        scheduler = Executors.newSingleThreadScheduledExecutor();
        HotKeyDetector detector = new HotKeyDetector(properties, scheduler, null);

        for (int i = 0; i < 200; i++) {
            detector.recordAccess("hot-key");
        }
        for (int i = 0; i < 80; i++) {
            detector.recordAccess("cold-" + i);
        }

        Map<?, ?> currentWindow = getCurrentWindow(detector);
        assertTrue(currentWindow.containsKey("hot-key"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getCurrentWindow(HotKeyDetector detector) throws Exception {
        Field field = HotKeyDetector.class.getDeclaredField("currentWindow");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(detector);
    }
}
