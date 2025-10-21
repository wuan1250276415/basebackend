package com.basebackend.scheduler.enums;

/**
 * 任务类型
 */
public enum JobType {
    /**
     * 定时任务 - 按照CRON表达式定期执行
     */
    SCHEDULED("定时任务"),

    /**
     * 延迟任务 - 延迟指定时间后执行一次
     */
    DELAY("延迟任务"),

    /**
     * 工作流任务 - DAG工作流编排
     */
    WORKFLOW("工作流任务"),

    /**
     * 即时任务 - 立即执行一次
     */
    IMMEDIATE("即时任务");

    private final String description;

    JobType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
