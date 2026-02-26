package com.basebackend.database.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 审计日志实体
 * 记录数据变更历史和操作日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_audit_log")
public class AuditLog extends BaseEntity {

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
    private Date operateTime;

    /**
     * 租户 ID
     */
    private String tenantId;
}
