package com.basebackend.common.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 异步大数据量导出服务
 * <p>
 * 支持大数据量场景（>10000条），后台线程生成文件，
 * 通过 taskId 查询状态和下载结果。任务超时自动清理。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class AsyncExportService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AsyncExportService.class);

    private final ExportManager exportManager;
    private final ExecutorService executor;
    private final Map<String, ExportTaskStatus> taskMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;
    private final long taskTtlMillis;

    public AsyncExportService(ExportManager exportManager, int threadPoolSize, long taskTtlHours) {
        this.exportManager = exportManager;
        int normalizedPoolSize = threadPoolSize > 0 ? threadPoolSize : 1;
        if (normalizedPoolSize != threadPoolSize) {
            log.warn("Invalid async export threadPoolSize={}, fallback to 1", threadPoolSize);
        }
        this.executor = Executors.newFixedThreadPool(normalizedPoolSize, createWorkerThreadFactory());
        this.taskTtlMillis = TimeUnit.HOURS.toMillis(taskTtlHours);
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "async-export-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredTasks, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 提交异步导出任务
     *
     * @param dataSupplier 数据提供函数
     * @param clazz        数据类型
     * @param format       导出格式
     * @return 任务 ID
     */
    public <T> String exportAsync(Supplier<List<T>> dataSupplier, Class<T> clazz, ExportFormat format) {
        String taskId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        ExportTaskStatus status = ExportTaskStatus.builder()
                .taskId(taskId)
                .status(ExportTaskStatus.Status.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskMap.put(taskId, status);

        executor.submit(() -> {
            try {
                updateStatus(status, ExportTaskStatus.Status.PROCESSING);
                List<T> data = dataSupplier.get();
                ExportResult result = exportManager.export(data, clazz, format);
                status.setResult(result);
                updateStatus(status, ExportTaskStatus.Status.COMPLETED);
                log.info("Async export task completed: taskId={}, format={}, rows={}", taskId, format, data.size());
            } catch (Exception e) {
                status.setMessage(e.getMessage());
                updateStatus(status, ExportTaskStatus.Status.FAILED);
                log.error("Async export task failed: taskId={}", taskId, e);
            }
        });

        return taskId;
    }

    /**
     * 查询导出任务状态
     */
    public ExportTaskStatus getExportStatus(String taskId) {
        return taskMap.get(taskId);
    }

    /**
     * 获取导出结果（仅 COMPLETED 状态有结果）
     */
    public ExportResult getExportResult(String taskId) {
        ExportTaskStatus status = taskMap.get(taskId);
        if (status != null && status.getStatus() == ExportTaskStatus.Status.COMPLETED) {
            return status.getResult();
        }
        return null;
    }

    void cleanupExpiredTasks() {
        long now = System.currentTimeMillis();
        taskMap.entrySet().removeIf(entry -> {
            ExportTaskStatus taskStatus = entry.getValue();
            ExportTaskStatus.Status status = taskStatus.getStatus();
            if (status != ExportTaskStatus.Status.COMPLETED && status != ExportTaskStatus.Status.FAILED) {
                return false;
            }
            long completedAt = resolveTerminalTimestamp(taskStatus, now);
            return now - completedAt >= taskTtlMillis;
        });
    }

    private long resolveTerminalTimestamp(ExportTaskStatus status, long now) {
        if (status.getCompletedAt() != null) {
            return status.getCompletedAt();
        }
        if (status.getUpdatedAt() != null) {
            return status.getUpdatedAt();
        }
        if (status.getCreatedAt() != null) {
            return status.getCreatedAt();
        }
        return now;
    }

    private void updateStatus(ExportTaskStatus status, ExportTaskStatus.Status newStatus) {
        long now = System.currentTimeMillis();
        status.setStatus(newStatus);
        status.setUpdatedAt(now);
        if (newStatus == ExportTaskStatus.Status.COMPLETED || newStatus == ExportTaskStatus.Status.FAILED) {
            status.setCompletedAt(now);
        }
    }

    private ThreadFactory createWorkerThreadFactory() {
        AtomicInteger threadIndex = new AtomicInteger(1);
        return runnable -> {
            Thread t = new Thread(runnable, "async-export-worker-" + threadIndex.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }

    @Override
    public void close() {
        executor.shutdownNow();
        cleanupScheduler.shutdownNow();
    }
}
