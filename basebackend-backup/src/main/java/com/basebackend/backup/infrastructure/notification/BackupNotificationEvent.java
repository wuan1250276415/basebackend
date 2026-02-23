package com.basebackend.backup.infrastructure.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备份通知事件
 */
@Data
@Builder
public class BackupNotificationEvent {

    public enum EventType {
        BACKUP_SUCCESS,
        BACKUP_FAILED,
        RESTORE_SUCCESS,
        RESTORE_FAILED,
        CLEANUP_COMPLETED
    }

    private EventType eventType;
    private String taskName;
    private String backupType;
    private String databaseName;
    private Long fileSize;
    private Long durationSeconds;
    private String errorMessage;
    private LocalDateTime eventTime;

    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("[BaseBackend Backup] ");

        switch (eventType) {
            case BACKUP_SUCCESS:
                sb.append("备份成功");
                break;
            case BACKUP_FAILED:
                sb.append("备份失败");
                break;
            case RESTORE_SUCCESS:
                sb.append("恢复成功");
                break;
            case RESTORE_FAILED:
                sb.append("恢复失败");
                break;
            case CLEANUP_COMPLETED:
                sb.append("清理完成");
                break;
        }

        sb.append("\n任务: ").append(taskName);
        if (backupType != null) {
            sb.append("\n类型: ").append(backupType);
        }
        if (databaseName != null) {
            sb.append("\n数据库: ").append(databaseName);
        }
        if (fileSize != null && fileSize > 0) {
            sb.append("\n文件大小: ").append(formatBytes(fileSize));
        }
        if (durationSeconds != null) {
            sb.append("\n耗时: ").append(durationSeconds).append("秒");
        }
        if (errorMessage != null) {
            sb.append("\n错误: ").append(errorMessage);
        }
        sb.append("\n时间: ").append(eventTime);
        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
}
