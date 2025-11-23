package com.basebackend.logging.audit.storage;

import com.basebackend.logging.audit.model.AuditLogEntry;

import java.util.List;

/**
 * 审计存储接口
 *
 * 定义审计日志的存储操作，
 * 支持多种存储后端实现。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public interface AuditStorage {

    /**
     * 保存单个审计日志条目
     *
     * @param entry 审计日志条目
     * @throws StorageException 存储异常
     */
    void save(AuditLogEntry entry) throws StorageException;

    /**
     * 批量保存审计日志条目
     *
     * @param entries 审计日志条目列表
     * @throws StorageException 存储异常
     */
    void batchSave(List<AuditLogEntry> entries) throws StorageException;

    /**
     * 读取审计日志条目
     *
     * @param id 审计日志 ID
     * @return 审计日志条目
     * @throws StorageException 存储异常
     */
    AuditLogEntry findById(String id) throws StorageException;

    /**
     * 读取指定时间范围内的审计日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 审计日志列表
     * @throws StorageException 存储异常
     */
    List<AuditLogEntry> findByTimeRange(long startTime, long endTime, int limit) throws StorageException;

    /**
     * 读取指定用户的审计日志
     *
     * @param userId 用户 ID
     * @param limit 限制数量
     * @return 审计日志列表
     * @throws StorageException 存储异常
     */
    List<AuditLogEntry> findByUserId(String userId, int limit) throws StorageException;

    /**
     * 读取指定事件类型的审计日志
     *
     * @param eventType 事件类型
     * @param limit 限制数量
     * @return 审计日志列表
     * @throws StorageException 存储异常
     */
    List<AuditLogEntry> findByEventType(String eventType, int limit) throws StorageException;

    /**
     * 验证存储的完整性
     *
     * @return 验证结果
     * @throws StorageException 存储异常
     */
    boolean verify() throws StorageException;

    /**
     * 清理过期数据
     *
     * @param retentionDays 保留天数
     * @return 清理的条目数量
     * @throws StorageException 存储异常
     */
    int cleanup(int retentionDays) throws StorageException;

    /**
     * 获取存储统计信息
     *
     * @return 统计信息
     * @throws StorageException 存储异常
     */
    StorageStats getStats() throws StorageException;

    /**
     * 关闭存储资源
     */
    void close();

    /**
     * 存储异常类
     */
    class StorageException extends Exception {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 存储统计信息
     */
    class StorageStats {
        private final long totalEntries;
        private final long totalSizeBytes;
        private final int fileCount;
        private final long oldestEntryTime;
        private final long newestEntryTime;
        private final int retentionDays;

        public StorageStats(long totalEntries, long totalSizeBytes, int fileCount,
                           long oldestEntryTime, long newestEntryTime, int retentionDays) {
            this.totalEntries = totalEntries;
            this.totalSizeBytes = totalSizeBytes;
            this.fileCount = fileCount;
            this.oldestEntryTime = oldestEntryTime;
            this.newestEntryTime = newestEntryTime;
            this.retentionDays = retentionDays;
        }

        public long getTotalEntries() {
            return totalEntries;
        }

        public long getTotalSizeBytes() {
            return totalSizeBytes;
        }

        public int getFileCount() {
            return fileCount;
        }

        public long getOldestEntryTime() {
            return oldestEntryTime;
        }

        public long getNewestEntryTime() {
            return newestEntryTime;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public double getAverageEntrySize() {
            return totalEntries > 0 ? (double) totalSizeBytes / totalEntries : 0.0;
        }

        public String formatSize() {
            return formatBytes(totalSizeBytes);
        }

        private String formatBytes(long bytes) {
            String[] units = {"B", "KB", "MB", "GB", "TB"};
            int unitIndex = 0;
            double size = bytes;

            while (size >= 1024 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }

            return String.format("%.2f %s", size, units[unitIndex]);
        }

        @Override
        public String toString() {
            return String.format(
                "存储统计: 条目=%d, 大小=%s, 文件数=%d, 最早=%s, 最新=%s, 保留期=%d天",
                totalEntries,
                formatSize(),
                fileCount,
                new java.util.Date(oldestEntryTime),
                new java.util.Date(newestEntryTime),
                retentionDays
            );
        }
    }
}
