package com.basebackend.logging.loglevel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 动态日志级别管理器
 *
 * 支持运行时修改日志级别，可选 TTL 自动恢复机制。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class LogLevelManager {

    private final LoggingSystem loggingSystem;
    private final int maxTtlSeconds;

    /**
     * 记录每个 logger 被修改前的原始级别，用于 reset 时恢复
     */
    private final ConcurrentHashMap<String, LogLevel> originalLevels = new ConcurrentHashMap<>();

    /**
     * TTL 自动恢复任务
     */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> revertTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler;

    public LogLevelManager(LoggingSystem loggingSystem, int maxTtlSeconds) {
        this.loggingSystem = loggingSystem;
        this.maxTtlSeconds = maxTtlSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "log-level-revert");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 获取所有已配置的 logger 信息
     */
    public List<Map<String, String>> getAllLoggers() {
        return loggingSystem.getLoggerConfigurations().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定 logger 的当前级别信息
     */
    public Map<String, String> getLogger(String loggerName) {
        LoggerConfiguration config = loggingSystem.getLoggerConfiguration(loggerName);
        if (config == null) {
            return Collections.emptyMap();
        }
        return toMap(config);
    }

    /**
     * 设置日志级别
     *
     * @param loggerName logger 名称
     * @param level      目标级别
     * @param ttlSeconds TTL 秒数，0 或负数表示永久
     */
    public void setLogLevel(String loggerName, LogLevel level, int ttlSeconds) {
        // 保存原始级别（仅首次修改时）
        originalLevels.computeIfAbsent(loggerName, name -> {
            LoggerConfiguration config = loggingSystem.getLoggerConfiguration(name);
            return config != null && config.getConfiguredLevel() != null
                    ? config.getConfiguredLevel()
                    : null;
        });

        // 取消已有的 revert 任务
        cancelRevertTask(loggerName);

        // 应用新级别
        loggingSystem.setLogLevel(loggerName, level);
        log.info("日志级别已修改: {} -> {}{}", loggerName, level,
                ttlSeconds > 0 ? " (TTL: " + ttlSeconds + "s)" : "");

        // 设置 TTL 自动恢复
        if (ttlSeconds > 0) {
            int effectiveTtl = Math.min(ttlSeconds, maxTtlSeconds);
            ScheduledFuture<?> future = scheduler.schedule(
                    () -> resetLogLevel(loggerName),
                    effectiveTtl,
                    TimeUnit.SECONDS
            );
            revertTasks.put(loggerName, future);
        }
    }

    /**
     * 重置日志级别为原始值
     */
    public void resetLogLevel(String loggerName) {
        cancelRevertTask(loggerName);

        LogLevel original = originalLevels.remove(loggerName);
        loggingSystem.setLogLevel(loggerName, original); // null 表示继承父级
        log.info("日志级别已恢复: {} -> {}", loggerName, original != null ? original : "inherited");
    }

    /**
     * 批量设置日志级别（用于 Nacos 配置推送）
     */
    public void applyBulkLevels(Map<String, LogLevel> levels) {
        if (levels == null || levels.isEmpty()) {
            return;
        }
        log.info("批量设置日志级别: {} 个 logger", levels.size());
        levels.forEach((name, level) -> setLogLevel(name, level, 0));
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void cancelRevertTask(String loggerName) {
        ScheduledFuture<?> existing = revertTasks.remove(loggerName);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }
    }

    private Map<String, String> toMap(LoggerConfiguration config) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", config.getName());
        map.put("configuredLevel", config.getConfiguredLevel() != null
                ? config.getConfiguredLevel().name() : null);
        map.put("effectiveLevel", config.getEffectiveLevel() != null
                ? config.getEffectiveLevel().name() : null);
        return map;
    }
}
