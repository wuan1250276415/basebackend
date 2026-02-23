package com.basebackend.scheduler.enums;

import lombok.Getter;

/**
 * 任务运行状态枚举
 * <p>
 * 定义调度任务的生命周期状态，从待调度到最终完成（成功/失败/终止）。
 * 任务状态的流转遵循严格的状态机规则，不允许非法状态转换。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Getter
public enum JobStatus {

    /**
     * 待调度
     * <p>
     * 任务已创建，等待调度器调度执行。
     * 初始状态，可转换为：SCHEDULING, TERMINATED
     * </p>
     */
    PENDING(0, "待调度", "任务已创建，等待调度执行"),

    /**
     * 调度中
     * <p>
     * 调度器正在分配Worker节点或任务正在分发中。
     * 中间状态，可转换为：RUNNING, FAILED, TERMINATED
     * </p>
     */
    SCHEDULING(1, "调度中", "正在分配Worker节点或分发任务"),

    /**
     * 运行中
     * <p>
     * 任务正在Worker节点上执行。
     * 运行状态，可转换为：SUCCEEDED, FAILED, TERMINATED
     * </p>
     */
    RUNNING(2, "运行中", "任务正在Worker节点上执行"),

    /**
     * 执行成功
     * <p>
     * 任务执行完成且无异常。
     * 终态，不可再转换到其他状态
     * </p>
     */
    SUCCEEDED(3, "执行成功", "任务执行完成且无异常"),

    /**
     * 执行失败
     * <p>
     * 任务执行过程中发生异常或超时。
     * 终态（除非配置了重试机制），失败后可重新调度
     * </p>
     */
    FAILED(4, "执行失败", "任务执行过程中发生异常或超时"),

    /**
     * 已终止
     * <p>
     * 任务被手动取消或系统强制终止。
     * 终态，不可再转换到其他状态
     * </p>
     */
    TERMINATED(5, "已终止", "任务被手动取消或系统强制终止"),

    /**
     * 已暂停
     * <p>
     * 任务被操作员暂停或触发了暂停策略。
     * 可恢复状态，可转换为：PENDING（恢复后重新调度）, TERMINATED（取消暂停任务）
     * </p>
     */
    PAUSED(6, "已暂停", "任务被暂停执行");

    /**
     * 状态代码
     */
    private final Integer code;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        状态代码
     * @param name        状态名称
     * @param description 状态描述
     */
    JobStatus(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据代码获取任务状态
     *
     * @param code 状态代码
     * @return 任务状态枚举，如果未找到则返回null
     */
    public static JobStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为终态
     * <p>
     * 终态包括：SUCCEEDED, FAILED, TERMINATED
     * </p>
     *
     * @return true表示终态，false表示非终态
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == TERMINATED;
    }

    /**
     * 判断是否为运行态
     * <p>
     * 运行态包括：SCHEDULING, RUNNING
     * </p>
     *
     * @return true表示运行态，false表示非运行态
     */
    public boolean isRunning() {
        return this == SCHEDULING || this == RUNNING;
    }

    /**
     * 判断状态转换是否合法
     *
     * @param targetStatus 目标状态
     * @return true表示可以转换，false表示不可转换
     */
    public boolean canTransitionTo(JobStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        // 终态不可再转换
        if (this.isTerminal()) {
            return false;
        }

        // PENDING可转换为SCHEDULING或TERMINATED
        if (this == PENDING) {
            return targetStatus == SCHEDULING || targetStatus == TERMINATED;
        }

        // SCHEDULING可转换为RUNNING、FAILED或TERMINATED
        if (this == SCHEDULING) {
            return targetStatus == RUNNING || targetStatus == FAILED || targetStatus == TERMINATED;
        }

        // RUNNING可转换为SUCCEEDED、FAILED或TERMINATED
        if (this == RUNNING) {
            return targetStatus == SUCCEEDED || targetStatus == FAILED || targetStatus == TERMINATED;
        }

        // PAUSED可转换为PENDING（恢复）或TERMINATED（取消）
        if (this == PAUSED) {
            return targetStatus == PENDING || targetStatus == TERMINATED;
        }

        return false;
    }
}
