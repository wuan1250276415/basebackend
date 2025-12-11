package com.basebackend.scheduler.processor.database;

import com.basebackend.scheduler.core.RetryPolicy;
import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 数据库备份任务处理器，支持全量与增量备份，使用幂等键避免重复执行。
 *
 * <p>特性：
 * <ul>
 *     <li>读取上下文参数 type=full|incremental 选择备份类型，默认全量。</li>
 *     <li>内置幂等缓存，幂等键命中则直接返回历史结果。</li>
 *     <li>指数退避重试策略，避免频繁重试冲击数据库。</li>
 *     <li>输出备份文件路径、大小、耗时等结果信息，便于指标采集。</li>
 * </ul>
 */
@Slf4j
@Component
public class DatabaseBackupProcessor implements TaskProcessor {

    private final DataSource dataSource;

    /**
     * 本地幂等缓存，避免同一幂等键重复触发真实备份。
     */
    private final ConcurrentMap<String, TaskResult> idempotentCache = new ConcurrentHashMap<>();

    public DatabaseBackupProcessor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "database-backup";
    }

    @Override
    public TaskResult process(TaskContext context) {
        Instant start = Instant.now();
        Optional<String> idempotentKey = idempotentKey(context);
        if (idempotentKey.isPresent()) {
            TaskResult cached = idempotentCache.get(idempotentKey.get());
            if (cached != null) {
                log.info("[DatabaseBackup] Idempotent hit for key={}, skip execution", idempotentKey.get());
                return cached;
            }
        }

        String typeParam = String.valueOf(
                context.getParameters().getOrDefault("type", "full"));
        boolean incremental = "incremental".equalsIgnoreCase(typeParam)
                || "incr".equalsIgnoreCase(typeParam);

        log.info("[DatabaseBackup] Start {} backup, taskId={}, idemKey={}",
                incremental ? "incremental" : "full", context.getTaskId(), idempotentKey.orElse("N/A"));

        Map<String, Object> output = new LinkedHashMap<>();
        try {
            String backupFile = performBackup(incremental);
            Duration duration = Duration.between(start, Instant.now());

            output.put("backupId", "backup-" + System.currentTimeMillis());
            output.put("type", incremental ? "INCREMENTAL" : "FULL");
            output.put("filePath", backupFile);
            output.put("fileSize", 1024L * 1024L); // 模拟1MB
            output.put("durationSeconds", duration.toSeconds());

            TaskResult result = TaskResult.builder(TaskResult.Status.SUCCESS)
                    .startTime(start)
                    .duration(duration)
                    .output(output)
                    .idempotentKey(idempotentKey.orElse(context.getIdempotentKey()))
                    .idempotentHit(idempotentKey.isPresent())
                    .build();

            if (idempotentKey.isPresent()) {
                idempotentCache.putIfAbsent(idempotentKey.get(), result);
            }

            log.info("[DatabaseBackup] Finished {} backup, status=SUCCESS, path={}, size={}",
                    output.get("type"), output.get("filePath"), output.get("fileSize"));
            return result;
        } catch (Exception ex) {
            log.error("[DatabaseBackup] Backup failed", ex);
            return TaskResult.builder(TaskResult.Status.FAILED)
                    .startTime(start)
                    .duration(Duration.between(start, Instant.now()))
                    .errorMessage(ex.getMessage())
                    .idempotentKey(idempotentKey.orElse(context.getIdempotentKey()))
                    .idempotentHit(idempotentKey.isPresent())
                    .output(output)
                    .build();
        }
    }

    private String performBackup(boolean incremental) throws Exception {
        // 模拟数据库备份
        try (Connection conn = dataSource.getConnection()) {
            // 执行备份逻辑（这里只是模拟）
            Thread.sleep(500); // 模拟耗时操作
            return "/backup/db-" + (incremental ? "incr-" : "full-") + System.currentTimeMillis() + ".sql";
        }
    }

    @Override
    public RetryPolicy retryPolicy() {
        return RetryPolicy.exponentialBackoff(
                3,
                Duration.ofSeconds(2),
                Duration.ofSeconds(30),
                ex -> true
        );
    }
}
