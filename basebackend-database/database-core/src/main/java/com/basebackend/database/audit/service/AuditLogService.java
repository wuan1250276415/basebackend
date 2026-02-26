package com.basebackend.database.audit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.database.audit.entity.AuditLog;
import com.basebackend.database.audit.query.AuditLogQuery;

/**
 * 审计日志服务接口
 */
public interface AuditLogService {

    /**
     * 异步记录审计日志
     *
     * @param auditLog 审计日志
     */
    void logAsync(AuditLog auditLog);

    /**
     * 同步记录审计日志
     *
     * @param auditLog 审计日志
     */
    void log(AuditLog auditLog);

    /**
     * 查询审计日志
     *
     * @param query 查询条件
     * @return 审计日志分页结果
     */
    Page<AuditLog> query(AuditLogQuery query);

    /**
     * 归档过期日志
     *
     * @param retentionDays 保留天数
     * @return 归档的日志数量
     */
    int archiveExpiredLogs(int retentionDays);
}
