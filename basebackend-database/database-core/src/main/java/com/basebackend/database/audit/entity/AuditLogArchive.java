package com.basebackend.database.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审计日志归档实体
 * 用于存储已归档的审计日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_audit_log_archive")
public class AuditLogArchive extends BaseEntity {

    /**
     * 操作类型 (INSERT/UPDATE/DELETE)
     */
    private String operationType;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 主键值
     */
    private String primaryKey;

    /**
     * 变更前数据 (JSON)
     */
    private String beforeData;

    /**
     * 变更后数据 (JSON)
     */
    private String afterData;

    /**
     * 变更字段
     */
    private String changedFields;

    /**
     * 操作人 ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作 IP
     */
    private String operatorIp;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 归档时间
     */
    private LocalDateTime archiveTime;

    /**
     * 原始日志ID
     */
    private Long originalLogId;
}
