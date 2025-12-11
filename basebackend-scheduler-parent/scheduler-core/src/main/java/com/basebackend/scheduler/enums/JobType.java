package com.basebackend.scheduler.enums;

import lombok.Getter;

/**
 * 调度任务类型枚举
 * <p>
 * 定义调度器支持的任务类型，包括一次性任务、定时任务、工作流任务和延迟任务。
 * 不同类型的任务使用不同的调度策略和执行引擎。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Getter
public enum JobType {

    /**
     * 一次性任务
     * <p>
     * 任务仅执行一次，执行后自动完成。
     * 适用场景：临时数据处理、一次性批量操作、手动触发的任务等。
     * </p>
     */
    ONE_TIME(1, "一次性任务", "任务仅执行一次后自动完成"),

    /**
     * Cron定时任务
     * <p>
     * 基于Cron表达式周期性执行的任务。
     * 适用场景：定时报表生成、定时数据同步、定时清理任务等。
     * 支持标准Cron表达式（秒 分 时 日 月 周 年）。
     * </p>
     */
    CRON(2, "定时任务", "基于Cron表达式周期性执行"),

    /**
     * 工作流任务
     * <p>
     * 由Camunda BPM工作流引擎编排和驱动的任务。
     * 适用场景：审批流程、复杂业务流程、多步骤任务编排等。
     * 任务执行由BPMN 2.0流程定义控制，支持分支、循环、并行网关等复杂逻辑。
     * </p>
     */
    WORKFLOW(3, "工作流任务", "由Camunda工作流引擎编排驱动"),

    /**
     * 延迟任务
     * <p>
     * 基于延迟队列的任务，在指定时间后执行。
     * 适用场景：订单超时取消、消息延迟推送、定时提醒等。
     * 由PowerJob延迟队列实现，支持秒级延迟精度。
     * </p>
     */
    DELAY(4, "延迟任务", "在指定延迟时间后执行"),

    /**
     * 固定频率任务
     * <p>
     * 按固定时间间隔周期性执行的任务（无论上次执行是否完成）。
     * 适用场景：实时监控采集、心跳检测、定时轮询等。
     * 注意：可能出现任务堆积，需要合理设置间隔时间。
     * </p>
     */
    FIXED_RATE(5, "固定频率任务", "按固定间隔周期执行（不等待上次完成）"),

    /**
     * 固定延迟任务
     * <p>
     * 上次执行完成后，延迟固定时间再执行下次任务。
     * 适用场景：避免任务堆积的周期性任务、依赖执行结果的任务等。
     * 与FIXED_RATE的区别：会等待上次执行完成后再计算延迟时间。
     * </p>
     */
    FIXED_DELAY(6, "固定延迟任务", "上次执行完成后延迟固定时间再执行");

    /**
     * 任务类型代码
     */
    private final Integer code;

    /**
     * 任务类型名称
     */
    private final String name;

    /**
     * 任务类型描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        任务类型代码
     * @param name        任务类型名称
     * @param description 任务类型描述
     */
    JobType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据代码获取任务类型
     *
     * @param code 任务类型代码
     * @return 任务类型枚举，如果未找到则返回null
     */
    public static JobType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为周期性任务
     * <p>
     * 周期性任务包括：CRON, FIXED_RATE, FIXED_DELAY
     * </p>
     *
     * @return true表示周期性任务，false表示非周期性任务
     */
    public boolean isPeriodic() {
        return this == CRON || this == FIXED_RATE || this == FIXED_DELAY;
    }

    /**
     * 判断是否需要工作流引擎
     * <p>
     * 仅WORKFLOW类型需要工作流引擎支持
     * </p>
     *
     * @return true表示需要工作流引擎，false表示不需要
     */
    public boolean requiresWorkflowEngine() {
        return this == WORKFLOW;
    }
}
