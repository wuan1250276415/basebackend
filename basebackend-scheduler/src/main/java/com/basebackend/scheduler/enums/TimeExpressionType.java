package com.basebackend.scheduler.enums;

import lombok.Getter;

/**
 * Time Expression Type Enum
 * <p>
 * Supported time expression types for scheduling.
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Getter
public enum TimeExpressionType {

    /**
     * Cron expression
     */
    CRON(1, "Cron表达式", "标准Cron语法周期性触发"),

    /**
     * Fixed rate with interval in milliseconds
     */
    FIXED_RATE(2, "固定频率", "按固定间隔（毫秒）周期执行"),

    /**
     * Fixed delay with interval in milliseconds
     */
    FIXED_DELAY(3, "固定延迟", "上次完成后延迟固定时间（毫秒）执行"),

    /**
     * Scheduled at specific timestamp
     */
    SCHEDULED_TIME(4, "指定时间", "在指定时间戳（毫秒）执行一次"),

    /**
     * Delayed execution with milliseconds from creation time
     */
    DELAY(5, "延迟执行", "从创建时间起延迟指定时间（毫秒）后执行");

    /**
     * 表达式类型代码
     */
    private final Integer code;

    /**
     * 表达式类型名称
     */
    private final String name;

    /**
     * 表达式类型描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        表达式类型代码
     * @param name        表达式类型名称
     * @param description 表达式类型描述
     */
    TimeExpressionType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * Get time expression type from code
     *
     * @param code expression type code
     * @return time expression type enum, null if not found
     */
    public static TimeExpressionType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TimeExpressionType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Check if this is periodic expression
     *
     * @return true if periodic
     */
    public boolean isPeriodic() {
        return this == CRON || this == FIXED_RATE || this == FIXED_DELAY;
    }

    /**
     * Check if this is one-time expression
     *
     * @return true if one-time
     */
    public boolean isOneTime() {
        return this == SCHEDULED_TIME || this == DELAY;
    }

    /**
     * Validate expression format (basic validation)
     *
     * @param expression time expression string
     * @return true if valid format
     */
    public boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            switch (this) {
                case CRON:
                    String[] fields = expression.trim().split("\\s+");
                    return fields.length >= 6 && fields.length <= 7;

                case FIXED_RATE:
                case FIXED_DELAY:
                case DELAY:
                    long interval = Long.parseLong(expression.trim());
                    return interval > 0;

                case SCHEDULED_TIME:
                    long timestamp = Long.parseLong(expression.trim());
                    return timestamp > System.currentTimeMillis();

                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
