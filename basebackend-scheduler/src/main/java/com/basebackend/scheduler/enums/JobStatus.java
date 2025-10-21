package com.basebackend.scheduler.enums;

/**
 * 任务状态
 */
public enum JobStatus {
    /**
     * 等待执行
     */
    WAITING("等待执行"),

    /**
     * 执行中
     */
    RUNNING("执行中"),

    /**
     * 执行成功
     */
    SUCCESS("执行成功"),

    /**
     * 执行失败
     */
    FAILED("执行失败"),

    /**
     * 已取消
     */
    CANCELLED("已取消"),

    /**
     * 超时
     */
    TIMEOUT("超时"),

    /**
     * 已停止
     */
    STOPPED("已停止");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
