package com.basebackend.database.audit.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志查询条件
 */
@Data
public class AuditLogQuery {

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 操作人 ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
