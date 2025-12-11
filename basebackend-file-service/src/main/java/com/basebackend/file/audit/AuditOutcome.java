package com.basebackend.file.audit;

/**
 * 审计结果枚举
 *
 * 定义操作的执行结果
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
public enum AuditOutcome {

    /**
     * 操作成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 操作失败
     */
    FAIL("FAIL", "失败");

    private final String code;
    private final String description;

    AuditOutcome(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }
}
