package com.basebackend.logging.audit;

import lombok.Getter;

/**
 * 审计事件类型枚举
 *
 * 定义系统中所有需要审计的操作类型，按类别和严重级别进行分类。
 * 支持等保2.0三级要求的完整审计覆盖。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Getter
public enum AuditEventType {

    /**
     * 认证相关事件
     */
    LOGIN("auth", AuditSeverity.MEDIUM, "用户登录"),
    LOGOUT("auth", AuditSeverity.LOW, "用户登出"),
    LOGIN_FAILED("auth", AuditSeverity.MEDIUM, "登录失败"),
    PASSWORD_CHANGE("auth", AuditSeverity.HIGH, "密码修改"),
    TOKEN_REFRESH("auth", AuditSeverity.LOW, "令牌刷新"),

    /**
     * 数据操作事件
     */
    CREATE("data", AuditSeverity.MEDIUM, "数据创建"),
    UPDATE("data", AuditSeverity.MEDIUM, "数据更新"),
    DELETE("data", AuditSeverity.HIGH, "数据删除"),
    BATCH_CREATE("data", AuditSeverity.MEDIUM, "批量创建"),
    BATCH_UPDATE("data", AuditSeverity.MEDIUM, "批量更新"),
    BATCH_DELETE("data", AuditSeverity.HIGH, "批量删除"),

    /**
     * 文件操作事件
     */
    UPLOAD("file", AuditSeverity.MEDIUM, "文件上传"),
    DOWNLOAD("file", AuditSeverity.MEDIUM, "文件下载"),
    EXPORT("file", AuditSeverity.HIGH, "数据导出"),
    IMPORT("file", AuditSeverity.HIGH, "数据导入"),

    /**
     * 安全相关事件
     */
    ACCESS_DENIED("security", AuditSeverity.CRITICAL, "访问被拒绝"),
    PRIVILEGE_ESCALATION("security", AuditSeverity.CRITICAL, "权限提升"),
    SUSPICIOUS_ACTIVITY("security", AuditSeverity.CRITICAL, "可疑活动"),
    SECURITY_VIOLATION("security", AuditSeverity.CRITICAL, "安全违规"),

    /**
     * 系统操作事件
     */
    SYSTEM_START("system", AuditSeverity.LOW, "系统启动"),
    SYSTEM_STOP("system", AuditSeverity.LOW, "系统停止"),
    CONFIG_CHANGE("system", AuditSeverity.MEDIUM, "配置变更"),
    BACKUP("system", AuditSeverity.HIGH, "数据备份"),
    RESTORE("system", AuditSeverity.HIGH, "数据恢复"),

    /**
     * API调用事件
     */
    API_ACCESS("api", AuditSeverity.MEDIUM, "API访问"),
    API_ERROR("api", AuditSeverity.MEDIUM, "API错误"),
    API_RATE_LIMIT("api", AuditSeverity.LOW, "API限流"),

    /**
     * 业务操作事件
     */
    ORDER_CREATE("business", AuditSeverity.MEDIUM, "订单创建"),
    ORDER_CANCEL("business", AuditSeverity.MEDIUM, "订单取消"),
    PAYMENT_PROCESS("business", AuditSeverity.HIGH, "支付处理"),
    REFUND("business", AuditSeverity.HIGH, "退款操作");

    /**
     * 事件类别
     */
    private final String category;

    /**
     * 严重级别
     */
    private final AuditSeverity severity;

    /**
     * 事件描述
     */
    private final String description;

    /**
     * 构造函数
     */
    AuditEventType(String category, AuditSeverity severity, String description) {
        this.category = category;
        this.severity = severity;
        this.description = description;
    }

    /**
     * 获取事件严重级别数值
     */
    public int getSeverityLevel() {
        return severity.getLevel();
    }

    /**
     * 是否为高危操作
     */
    public boolean isHighRisk() {
        return severity == AuditSeverity.HIGH || severity == AuditSeverity.CRITICAL;
    }

    /**
     * 是否需要特殊保护
     */
    public boolean requiresProtection() {
        return isHighRisk() || category.equals("security");
    }
}
