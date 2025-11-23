package com.basebackend.backup.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 备份系统指标端点
 * 提供 Prometheus 格式的指标数据
 */
@Slf4j
@Component
@WebEndpoint(id = "backup-metrics")
public class MetricsController {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private BackupMetricsRegistrar backupMetricsRegistrar;

    /**
     * 获取所有备份指标
     */
    @ReadOperation
    public Map<String, Object> getBackupMetrics() {
        log.debug("获取备份系统指标");

        Map<String, Object> metrics = new HashMap<>();

        // 获取基础指标
        BackupMetricsRegistrar.MetricsSnapshot snapshot = backupMetricsRegistrar.getSnapshot();

        metrics.put("backup_total", snapshot.getBackupTotal());
        metrics.put("backup_success", snapshot.getBackupSuccess());
        metrics.put("backup_failure", snapshot.getBackupFailure());
        metrics.put("backup_success_rate", backupMetricsRegistrar.getBackupSuccessRate());

        metrics.put("restore_total", snapshot.getRestoreTotal());
        metrics.put("restore_success", snapshot.getRestoreSuccess());
        metrics.put("restore_failure", snapshot.getRestoreFailure());
        metrics.put("restore_success_rate", backupMetricsRegistrar.getRestoreSuccessRate());

        metrics.put("active_backup_tasks", snapshot.getActiveBackupTasks());
        metrics.put("active_restore_tasks", snapshot.getActiveRestoreTasks());

        metrics.put("total_retries", snapshot.getTotalRetries());
        metrics.put("successful_retries", snapshot.getSuccessfulRetries());
        metrics.put("retry_success_rate", backupMetricsRegistrar.getRetrySuccessRate());

        // 存储使用量
        Map<String, Object> storage = new HashMap<>();
        snapshot.getStorageSizeMap().forEach((type, size) -> {
            storage.put(type + "_bytes", size.get());
        });
        snapshot.getStorageCountMap().forEach((type, count) -> {
            storage.put(type + "_objects", count.get());
        });
        metrics.put("storage", storage);

        // 时间戳
        metrics.put("timestamp", System.currentTimeMillis());

        log.debug("备份指标统计: 备份成功率={}%, 恢复成功率={}%",
            String.format("%.2f", backupMetricsRegistrar.getBackupSuccessRate() * 100),
            String.format("%.2f", backupMetricsRegistrar.getRestoreSuccessRate() * 100));

        return metrics;
    }

    /**
     * 获取Prometheus格式的指标
     */
    @ReadOperation
    public String getPrometheusMetrics() {
        log.debug("获取Prometheus格式指标");

        StringBuilder sb = new StringBuilder();

        // 生成Prometheus格式指标
        BackupMetricsRegistrar.MetricsSnapshot snapshot = backupMetricsRegistrar.getSnapshot();

        // 备份指标
        sb.append("# HELP backup_backup_total Total number of backup operations\n");
        sb.append("# TYPE backup_backup_total counter\n");
        sb.append("backup_backup_total ").append(snapshot.getBackupTotal()).append("\n");

        sb.append("# HELP backup_backup_success_total Total number of successful backup operations\n");
        sb.append("# TYPE backup_backup_success_total counter\n");
        sb.append("backup_backup_success_total ").append(snapshot.getBackupSuccess()).append("\n");

        sb.append("# HELP backup_backup_failure_total Total number of failed backup operations\n");
        sb.append("# TYPE backup_backup_failure_total counter\n");
        sb.append("backup_backup_failure_total ").append(snapshot.getBackupFailure()).append("\n");

        // 恢复指标
        sb.append("# HELP backup_restore_total Total number of restore operations\n");
        sb.append("# TYPE backup_restore_total counter\n");
        sb.append("backup_restore_total ").append(snapshot.getRestoreTotal()).append("\n");

        sb.append("# HELP backup_restore_success_total Total number of successful restore operations\n");
        sb.append("# TYPE backup_restore_success_total counter\n");
        sb.append("backup_restore_success_total ").append(snapshot.getRestoreSuccess()).append("\n");

        sb.append("# HELP backup_restore_failure_total Total number of failed restore operations\n");
        sb.append("# TYPE backup_restore_failure_total counter\n");
        sb.append("backup_restore_failure_total ").append(snapshot.getRestoreFailure()).append("\n");

        // 活跃任务
        sb.append("# HELP backup_active_backup_tasks Number of currently running backup tasks\n");
        sb.append("# TYPE backup_active_backup_tasks gauge\n");
        sb.append("backup_active_backup_tasks ").append(snapshot.getActiveBackupTasks()).append("\n");

        sb.append("# HELP backup_active_restore_tasks Number of currently running restore tasks\n");
        sb.append("# TYPE backup_active_restore_tasks gauge\n");
        sb.append("backup_active_restore_tasks ").append(snapshot.getActiveRestoreTasks()).append("\n");

        // 重试指标
        sb.append("# HELP backup_retries_total Total number of retry attempts\n");
        sb.append("# TYPE backup_retries_total counter\n");
        sb.append("backup_retries_total ").append(snapshot.getTotalRetries()).append("\n");

        sb.append("# HELP backup_retries_success_total Total number of successful retry attempts\n");
        sb.append("# TYPE backup_retries_success_total counter\n");
        sb.append("backup_retries_success_total ").append(snapshot.getSuccessfulRetries()).append("\n");

        // 存储使用量
        snapshot.getStorageSizeMap().forEach((type, size) -> {
            sb.append("# HELP backup_storage_usage_bytes Storage usage in bytes\n");
            sb.append("# TYPE backup_storage_usage_bytes gauge\n");
            sb.append("backup_storage_usage_bytes{type=\"").append(type).append("\"} ")
                .append(size.get()).append("\n");
        });

        snapshot.getStorageCountMap().forEach((type, count) -> {
            sb.append("# HELP backup_storage_objects_total Total number of storage objects\n");
            sb.append("# TYPE backup_storage_objects_total gauge\n");
            sb.append("backup_storage_objects_total{type=\"").append(type).append("\"} ")
                .append(count.get()).append("\n");
        });

        return sb.toString();
    }

    /**
     * 健康检查
     */
    @ReadOperation
    public Map<String, Object> getHealthStatus() {
        BackupMetricsRegistrar.MetricsSnapshot snapshot = backupMetricsRegistrar.getSnapshot();

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, Object> details = new HashMap<>();
        details.put("backup_success_rate", backupMetricsRegistrar.getBackupSuccessRate());
        details.put("restore_success_rate", backupMetricsRegistrar.getRestoreSuccessRate());
        details.put("active_tasks", snapshot.getActiveBackupTasks() + snapshot.getActiveRestoreTasks());
        details.put("total_retries", snapshot.getTotalRetries());

        health.put("details", details);

        return health;
    }
}
