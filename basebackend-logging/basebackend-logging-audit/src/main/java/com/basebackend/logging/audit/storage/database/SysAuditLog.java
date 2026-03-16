package com.basebackend.logging.audit.storage.database;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 审计日志数据库实体
 *
 * 映射到 sys_audit_log 表，不继承 BaseEntity，使用 String UUID 主键。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.INPUT)
    private String id;

    private Instant timestamp;

    private String userId;

    private String sessionId;

    /**
     * 事件类型，以 VARCHAR 存储枚举名称
     */
    private String eventType;

    private String resource;

    private String result;

    private String clientIp;

    private String userAgent;

    private String deviceInfo;

    private String location;

    private String entityId;

    private String operation;

    /**
     * 详情字段，JSON TEXT 存储
     */
    @TableField(typeHandler = JsonMapTypeHandler.class)
    private Map<String, Object> details;

    private Long durationMs;

    private String errorCode;

    private String errorMessage;

    private String traceId;

    private String spanId;

    private String prevHash;

    private String entryHash;

    private String signature;

    private String certificateId;

    /**
     * 租户 ID（多租户场景必填）
     *
     * <p>对应 {@code AuditLogEntry#tenantId}，用于在单库多租户场景下隔离审计数据。
     * 数据库表须添加 {@code tenant_id VARCHAR(64)} 列并建立索引。
     */
    private String tenantId;
}
