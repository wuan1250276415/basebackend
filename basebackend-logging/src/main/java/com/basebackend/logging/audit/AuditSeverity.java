package com.basebackend.logging.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计事件严重级别枚举
 *
 * 定义审计事件的严重程度，用于告警策略和存储优化。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Getter
@AllArgsConstructor
public enum AuditSeverity {

    /**
     * 低级别 - 常规操作记录
     * 例如：用户登录、系统启动等
     */
    LOW(1, "LOW", "低级别", "常规操作记录"),

    /**
     * 中级别 - 重要业务操作
     * 例如：数据修改、文件传输等
     */
    MEDIUM(2, "MEDIUM", "中级别", "重要业务操作"),

    /**
     * 高级别 - 关键数据操作
     * 例如：数据删除、导出、备份等
     */
    HIGH(3, "HIGH", "高级别", "关键数据操作"),

    /**
     * 严重 - 安全相关操作
     * 例如：访问被拒绝、权限提升等
     */
    CRITICAL(4, "CRITICAL", "严重", "安全相关操作");

    private final int level;
    private final String code;
    private final String name;
    private final String description;

    /**
     * 根据级别码获取严重级别
     */
    public static AuditSeverity fromLevel(int level) {
        for (AuditSeverity severity : values()) {
            if (severity.getLevel() == level) {
                return severity;
            }
        }
        return MEDIUM; // 默认值
    }

    /**
     * 根据编码获取严重级别
     */
    public static AuditSeverity fromCode(String code) {
        for (AuditSeverity severity : values()) {
            if (severity.getCode().equals(code)) {
                return severity;
            }
        }
        return MEDIUM; // 默认值
    }
}
