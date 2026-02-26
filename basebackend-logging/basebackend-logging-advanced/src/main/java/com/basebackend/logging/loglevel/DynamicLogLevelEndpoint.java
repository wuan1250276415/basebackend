package com.basebackend.logging.loglevel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.logging.LogLevel;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 动态日志级别 Actuator 端点
 *
 * 端点路径: /actuator/log-level
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@Endpoint(id = "log-level")
public class DynamicLogLevelEndpoint {

    private final LogLevelManager logLevelManager;
    private final int defaultTtlSeconds;

    public DynamicLogLevelEndpoint(LogLevelManager logLevelManager, int defaultTtlSeconds) {
        this.logLevelManager = logLevelManager;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    /**
     * GET /actuator/log-level — 列出所有 logger 级别
     */
    @ReadOperation
    public List<Map<String, String>> listAll() {
        return logLevelManager.getAllLoggers();
    }

    /**
     * GET /actuator/log-level/{loggerName} — 查看指定 logger
     */
    @ReadOperation
    public Map<String, String> getLoggerLevel(@Selector String loggerName) {
        return logLevelManager.getLogger(loggerName);
    }

    /**
     * POST /actuator/log-level/{loggerName}
     * body: { "configuredLevel": "DEBUG", "ttlSeconds": 300 }
     */
    @WriteOperation
    public Map<String, Object> setLoggerLevel(@Selector String loggerName,
                                               String configuredLevel,
                                               @Nullable Integer ttlSeconds) {
        try {
            LogLevel level = LogLevel.valueOf(configuredLevel.toUpperCase());
            int ttl = ttlSeconds != null ? ttlSeconds : defaultTtlSeconds;
            logLevelManager.setLogLevel(loggerName, level, ttl);
            return Map.of(
                    "loggerName", loggerName,
                    "configuredLevel", level.name(),
                    "ttlSeconds", ttl,
                    "status", "OK"
            );
        } catch (IllegalArgumentException e) {
            return Map.of(
                    "loggerName", loggerName,
                    "error", "无效的日志级别: " + configuredLevel,
                    "validLevels", List.of(LogLevel.values()),
                    "status", "ERROR"
            );
        }
    }

    /**
     * DELETE /actuator/log-level/{loggerName} — 重置为原始级别
     */
    @DeleteOperation
    public Map<String, String> resetLoggerLevel(@Selector String loggerName) {
        logLevelManager.resetLogLevel(loggerName);
        return Map.of(
                "loggerName", loggerName,
                "status", "RESET"
        );
    }
}
