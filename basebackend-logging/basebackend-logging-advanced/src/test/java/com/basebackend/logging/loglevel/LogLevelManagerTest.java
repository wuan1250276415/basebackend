package com.basebackend.logging.loglevel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * LogLevelManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LogLevelManagerTest {

    @Mock
    private LoggingSystem loggingSystem;

    private LogLevelManager manager;

    @BeforeEach
    void setUp() {
        manager = new LogLevelManager(loggingSystem, 3600);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    void setLogLevel_shouldDelegateToLoggingSystem() {
        when(loggingSystem.getLoggerConfiguration("com.test"))
                .thenReturn(new LoggerConfiguration("com.test", LogLevel.INFO, LogLevel.INFO));

        manager.setLogLevel("com.test", LogLevel.DEBUG, 0);

        verify(loggingSystem).setLogLevel("com.test", LogLevel.DEBUG);
    }

    @Test
    void resetLogLevel_shouldRestoreOriginalLevel() {
        when(loggingSystem.getLoggerConfiguration("com.test"))
                .thenReturn(new LoggerConfiguration("com.test", LogLevel.WARN, LogLevel.WARN));

        manager.setLogLevel("com.test", LogLevel.DEBUG, 0);
        manager.resetLogLevel("com.test");

        verify(loggingSystem).setLogLevel("com.test", LogLevel.DEBUG);
        verify(loggingSystem).setLogLevel("com.test", LogLevel.WARN);
    }

    @Test
    void resetLogLevel_noPreviousSet_shouldSetNull() {
        manager.resetLogLevel("com.unknown");

        verify(loggingSystem).setLogLevel("com.unknown", null);
    }

    @Test
    void setLogLevel_withTtl_shouldAutoRevert() throws InterruptedException {
        when(loggingSystem.getLoggerConfiguration("com.ttl"))
                .thenReturn(new LoggerConfiguration("com.ttl", LogLevel.INFO, LogLevel.INFO));

        manager.setLogLevel("com.ttl", LogLevel.TRACE, 1); // 1 second TTL

        verify(loggingSystem).setLogLevel("com.ttl", LogLevel.TRACE);

        // Wait for TTL to expire
        TimeUnit.MILLISECONDS.sleep(1500);

        // Should have been reverted to original
        verify(loggingSystem).setLogLevel("com.ttl", LogLevel.INFO);
    }

    @Test
    void setLogLevel_ttlExceedsMax_shouldCapAtMax() {
        LogLevelManager shortMaxManager = new LogLevelManager(loggingSystem, 2);
        try {
            when(loggingSystem.getLoggerConfiguration("com.cap"))
                    .thenReturn(new LoggerConfiguration("com.cap", LogLevel.INFO, LogLevel.INFO));

            // Setting TTL of 9999 should be capped at maxTtlSeconds (2)
            shortMaxManager.setLogLevel("com.cap", LogLevel.DEBUG, 9999);

            verify(loggingSystem).setLogLevel("com.cap", LogLevel.DEBUG);
        } finally {
            shortMaxManager.shutdown();
        }
    }

    @Test
    void applyBulkLevels_shouldSetAllLoggers() {
        Map<String, LogLevel> levels = new LinkedHashMap<>();
        levels.put("com.a", LogLevel.DEBUG);
        levels.put("com.b", LogLevel.WARN);
        levels.put("com.c", LogLevel.ERROR);

        manager.applyBulkLevels(levels);

        verify(loggingSystem).setLogLevel("com.a", LogLevel.DEBUG);
        verify(loggingSystem).setLogLevel("com.b", LogLevel.WARN);
        verify(loggingSystem).setLogLevel("com.c", LogLevel.ERROR);
    }

    @Test
    void applyBulkLevels_nullOrEmpty_shouldDoNothing() {
        manager.applyBulkLevels(null);
        manager.applyBulkLevels(Map.of());

        verifyNoInteractions(loggingSystem);
    }

    @Test
    void getAllLoggers_shouldReturnMappedConfigurations() {
        when(loggingSystem.getLoggerConfigurations()).thenReturn(Arrays.asList(
                new LoggerConfiguration("ROOT", LogLevel.INFO, LogLevel.INFO),
                new LoggerConfiguration("com.test", LogLevel.DEBUG, LogLevel.DEBUG)
        ));

        List<Map<String, String>> result = manager.getAllLoggers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name")).isEqualTo("ROOT");
        assertThat(result.get(0).get("configuredLevel")).isEqualTo("INFO");
        assertThat(result.get(1).get("name")).isEqualTo("com.test");
    }

    @Test
    void getLogger_existing_shouldReturnInfo() {
        when(loggingSystem.getLoggerConfiguration("com.test"))
                .thenReturn(new LoggerConfiguration("com.test", LogLevel.DEBUG, LogLevel.DEBUG));

        Map<String, String> result = manager.getLogger("com.test");

        assertThat(result).isNotEmpty();
        assertThat(result.get("name")).isEqualTo("com.test");
        assertThat(result.get("configuredLevel")).isEqualTo("DEBUG");
        assertThat(result.get("effectiveLevel")).isEqualTo("DEBUG");
    }

    @Test
    void getLogger_nonExisting_shouldReturnEmpty() {
        when(loggingSystem.getLoggerConfiguration("com.nonexist")).thenReturn(null);

        Map<String, String> result = manager.getLogger("com.nonexist");

        assertThat(result).isEmpty();
    }

    @Test
    void setLogLevel_multipleTimes_shouldPreserveOriginalLevel() {
        when(loggingSystem.getLoggerConfiguration("com.multi"))
                .thenReturn(new LoggerConfiguration("com.multi", LogLevel.INFO, LogLevel.INFO));

        manager.setLogLevel("com.multi", LogLevel.DEBUG, 0);
        manager.setLogLevel("com.multi", LogLevel.TRACE, 0);
        manager.resetLogLevel("com.multi");

        // Should revert to the ORIGINAL level (INFO), not the intermediate one (DEBUG)
        verify(loggingSystem).setLogLevel("com.multi", LogLevel.INFO);
    }
}
