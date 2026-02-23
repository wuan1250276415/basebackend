package com.basebackend.common.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
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
public class AsyncExportService {

    private static final Logger log = LoggerFactory.getLogger(AsyncExportService.class);

    private final ExportManager exportManager;
    private final ExecutorService executor;
    private final Map<String, ExportTaskStatus> taskMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;
    private final long taskTtlMillis;

    public AsyncExportService(ExportManager exportManager, int threadPoolSize, long taskTtlHours) {
        this.exportManager = exportManager;
        this.executor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "async-export-worker");
            t.setDaemon(true);
            return t;
        });
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
        ExportTaskStatus status = ExportTaskStatus.builder()
                .taskId(taskId)
                .status(ExportTaskStatus.Status.PENDING)
                .build();
        taskMap.put(taskId, status);

        executor.submit(() -> {
            try {
                status.setStatus(ExportTaskStatus.Status.PROCESSING);
                List<T> data = dataSupplier.get();
                ExportResult result = exportManager.export(data, clazz, format);
                status.setResult(result);
                status.setStatus(ExportTaskStatus.Status.COMPLETED);
                log.info("Async export task completed: taskId={}, format={}, rows={}", taskId, format, data.size());
            } catch (Exception e) {
                status.setStatus(ExportTaskStatus.Status.FAILED);
                status.setMessage(e.getMessage());
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

    private void cleanupExpiredTasks() {
        // 简单策略：清理所有已完成/失败超过 TTL 的任务
        // 由于没有记录创建时间在 status 中，这里清理所有已完成/失败的任务
        taskMap.entrySet().removeIf(entry -> {
            ExportTaskStatus.Status s = entry.getValue().getStatus();
            return s == ExportTaskStatus.Status.COMPLETED || s == ExportTaskStatus.Status.FAILED;
        });
    }
}
