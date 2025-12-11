package com.basebackend.file.audit;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分享审计日志实体
 *
 * 记录所有文件分享相关的安全和管理操作
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Data
@TableName("sys_file_share_audit_log")
public class FileShareAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 追踪ID（用于链路追踪）
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 跨度ID（可选）
     */
    @TableField("span_id")
    private String spanId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 租户ID（可选）
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 分享码
     */
    @TableField("share_code")
    private String shareCode;

    /**
     * 操作类型（枚举值）
     */
    @TableField("action")
    private AuditAction action;

    /**
     * 操作结果
     */
    @TableField("outcome")
    private AuditOutcome outcome;

    /**
     * 错误码（失败时使用）
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误原因（失败时使用）
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 客户端IP地址
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * User-Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * Referer
     */
    @TableField("referrer")
    private String referrer;

    /**
     * 是否触发限流
     */
    @TableField("rate_limit_hit")
    private Boolean rateLimitHit;

    /**
     * 扩展信息（JSON格式）
     * 包含：limitRemaining、downloadCountBefore、downloadCountAfter、cooldownUntil等
     */
    @TableField("details")
    private String details;
}
