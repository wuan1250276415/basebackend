package com.basebackend.database.audit.service;

/**
 * 审计日志归档服务接口
 */
public interface AuditLogArchiveService {

    /**
     * 归档过期的审计日志
     * 将超过保留期限的日志移动到归档表
     *
     * @param retentionDays 保留天数
     * @return 归档的日志数量
     */
    int archiveExpiredLogs(int retentionDays);

    /**
     * 清理过期的审计日志（不归档，直接删除）
     * 
     * @param retentionDays 保留天数
     * @return 清理的日志数量
     */
    int cleanExpiredLogs(int retentionDays);

    /**
     * 清理归档表中的过期数据
     * 
     * @param archiveRetentionDays 归档数据保留天数
     * @return 清理的归档日志数量
     */
    int cleanExpiredArchives(int archiveRetentionDays);
}
