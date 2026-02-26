package com.basebackend.logging.loglevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 traceId / userId 的条件日志级别过滤器
 * <p>
 * 作为 Logback TurboFilter 运行，在日志事件被丢弃前拦截并检查
 * 当前 MDC 中的 traceId 或 userId 是否匹配已注册的调试条目。
 * 匹配时将日志级别提升至目标级别（默认 DEBUG），不匹配时不干预。
 * <p>
 * 所有条目带有 TTL，到期后自动失效。后台清理由调用方触发（惰性清理）。
 */
@Slf4j
public class ConditionalLogLevelFilter extends TurboFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String USER_ID_KEY = "userId";

    private final ConcurrentHashMap<String, DebugEntry> traceIdEntries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DebugEntry> userIdEntries = new ConcurrentHashMap<>();

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        if (traceIdEntries.isEmpty() && userIdEntries.isEmpty()) {
            return FilterReply.NEUTRAL;
        }

        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null) {
            DebugEntry entry = traceIdEntries.get(traceId);
            if (entry != null) {
                if (entry.isExpired()) {
                    traceIdEntries.remove(traceId);
                } else if (level.isGreaterOrEqual(entry.targetLevel)) {
                    return FilterReply.ACCEPT;
                }
            }
        }

        String userId = MDC.get(USER_ID_KEY);
        if (userId != null) {
            DebugEntry entry = userIdEntries.get(userId);
            if (entry != null) {
                if (entry.isExpired()) {
                    userIdEntries.remove(userId);
                } else if (level.isGreaterOrEqual(entry.targetLevel)) {
                    return FilterReply.ACCEPT;
                }
            }
        }

        return FilterReply.NEUTRAL;
    }

    /**
     * 注册 traceId 级别提升
     *
     * @param traceId    目标 traceId
     * @param level      提升到的级别
     * @param ttlSeconds TTL 秒数
     */
    public void addTraceId(String traceId, Level level, int ttlSeconds) {
        traceIdEntries.put(traceId, new DebugEntry(level, ttlSeconds));
        evictExpired();
    }

    /**
     * 注册 userId 级别提升
     *
     * @param userId     目标 userId
     * @param level      提升到的级别
     * @param ttlSeconds TTL 秒数
     */
    public void addUserId(String userId, Level level, int ttlSeconds) {
        userIdEntries.put(userId, new DebugEntry(level, ttlSeconds));
        evictExpired();
    }

    public void removeTraceId(String traceId) {
        traceIdEntries.remove(traceId);
    }

    public void removeUserId(String userId) {
        userIdEntries.remove(userId);
    }

    public Map<String, DebugEntry> getActiveTraceIds() {
        evictExpired();
        return Map.copyOf(traceIdEntries);
    }

    public Map<String, DebugEntry> getActiveUserIds() {
        evictExpired();
        return Map.copyOf(userIdEntries);
    }

    private void evictExpired() {
        evictFrom(traceIdEntries);
        evictFrom(userIdEntries);
    }

    private void evictFrom(ConcurrentHashMap<String, DebugEntry> map) {
        Iterator<Map.Entry<String, DebugEntry>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    /**
     * 调试条目：记录目标级别和过期时间
     */
    public static class DebugEntry {
        final Level targetLevel;
        final Instant expiresAt;

        DebugEntry(Level targetLevel, int ttlSeconds) {
            this.targetLevel = targetLevel;
            this.expiresAt = Instant.now().plusSeconds(ttlSeconds);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public Level getTargetLevel() {
            return targetLevel;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
