package com.basebackend.logging.loglevel;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 条件日志级别 Actuator 端点
 * <p>
 * 端点路径: /actuator/conditional-log-level
 * <p>
 * 支持按 traceId 或 userId 注册临时日志级别提升。
 */
@Slf4j
@Endpoint(id = "conditional-log-level")
public class ConditionalLogLevelEndpoint {

    private final ConditionalLogLevelFilter filter;
    private final int defaultTtlSeconds;
    private final int maxTtlSeconds;

    public ConditionalLogLevelEndpoint(ConditionalLogLevelFilter filter,
                                       int defaultTtlSeconds,
                                       int maxTtlSeconds) {
        this.filter = filter;
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.maxTtlSeconds = maxTtlSeconds;
    }

    /**
     * GET /actuator/conditional-log-level — 查看所有活跃条件
     */
    @ReadOperation
    public Map<String, Object> listAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traceIds", toDisplayList(filter.getActiveTraceIds()));
        result.put("userIds", toDisplayList(filter.getActiveUserIds()));
        return result;
    }

    /**
     * POST /actuator/conditional-log-level
     * body: { "type": "traceId", "value": "abc123", "level": "DEBUG", "ttlSeconds": 300 }
     */
    @WriteOperation
    public Map<String, Object> addCondition(String type,
                                             String value,
                                             @Nullable String level,
                                             @Nullable Integer ttlSeconds) {
        Level targetLevel = parseLevel(level, Level.DEBUG);
        int ttl = Math.min(ttlSeconds != null ? ttlSeconds : defaultTtlSeconds, maxTtlSeconds);
        if (ttl <= 0) {
            ttl = defaultTtlSeconds > 0 ? defaultTtlSeconds : 300;
        }

        switch (type.toLowerCase()) {
            case "traceid" -> filter.addTraceId(value, targetLevel, ttl);
            case "userid" -> filter.addUserId(value, targetLevel, ttl);
            default -> {
                return Map.of("status", "ERROR", "error", "type must be 'traceId' or 'userId'");
            }
        }

        log.info("条件日志级别已注册: type={}, value={}, level={}, ttl={}s", type, value, targetLevel, ttl);
        return Map.of(
                "type", type,
                "value", value,
                "level", targetLevel.toString(),
                "ttlSeconds", ttl,
                "status", "OK"
        );
    }

    /**
     * DELETE /actuator/conditional-log-level
     * params: type=traceId&value=abc123
     */
    @DeleteOperation
    public Map<String, String> removeCondition(String type, String value) {
        switch (type.toLowerCase()) {
            case "traceid" -> filter.removeTraceId(value);
            case "userid" -> filter.removeUserId(value);
            default -> {
                return Map.of("status", "ERROR", "error", "type must be 'traceId' or 'userId'");
            }
        }

        log.info("条件日志级别已移除: type={}, value={}", type, value);
        return Map.of("type", type, "value", value, "status", "REMOVED");
    }

    private Level parseLevel(String level, Level defaultLevel) {
        if (level == null || level.isBlank()) {
            return defaultLevel;
        }
        try {
            return Level.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultLevel;
        }
    }

    private List<Map<String, String>> toDisplayList(Map<String, ConditionalLogLevelFilter.DebugEntry> entries) {
        return entries.entrySet().stream()
                .map(e -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("value", e.getKey());
                    m.put("level", e.getValue().getTargetLevel().toString());
                    m.put("expiresAt", e.getValue().getExpiresAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
