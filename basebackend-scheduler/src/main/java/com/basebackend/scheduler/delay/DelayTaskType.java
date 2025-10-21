package com.basebackend.scheduler.delay;

/**
 * 延迟任务类型
 */
public enum DelayTaskType {
    /**
     * 订单超时取消
     */
    ORDER_TIMEOUT("订单超时取消"),

    /**
     * 消息延迟发送
     */
    MESSAGE_DELAY("消息延迟发送"),

    /**
     * 数据定时清理
     */
    DATA_CLEANUP("数据定时清理"),

    /**
     * 业务状态流转
     */
    STATE_TRANSITION("业务状态流转");

    private final String description;

    DelayTaskType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
