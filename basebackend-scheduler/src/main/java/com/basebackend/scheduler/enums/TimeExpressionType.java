package com.basebackend.scheduler.enums;

/**
 * 时间表达式类型
 */
public enum TimeExpressionType {
    /**
     * CRON表达式 - 例如: 0 0 0 * * ?
     */
    CRON("CRON表达式"),

    /**
     * 固定频率 - 例如: 每5秒执行一次
     */
    FIXED_RATE("固定频率"),

    /**
     * 固定延迟 - 例如: 上次执行完成后5秒再执行
     */
    FIXED_DELAY("固定延迟"),

    /**
     * API触发 - 通过API手动触发
     */
    API("API触发"),

    /**
     * 工作流 - 由工作流引擎触发
     */
    WORKFLOW("工作流触发");

    private final String description;

    TimeExpressionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
