package com.basebackend.scheduler.enums;

import lombok.Getter;

/**
 * 任务执行类型枚举
 * <p>
 * 定义调度任务的执行模式，包括单机执行、广播执行、分片执行和工作流触发执行。
 * 不同的执行类型适用于不同的业务场景。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Getter
public enum ExecuteType {

    /**
     * 单机执行
     * <p>
     * 任务在单个Worker节点上执行，适用于非资源密集型任务。
     * 调度器会选择一个健康的Worker节点执行任务。
     * </p>
     */
    STANDALONE(1, "单机执行", "任务在单个Worker节点上执行"),

    /**
     * 广播执行
     * <p>
     * 任务在所有可用的Worker节点上同时执行，适用于需要在所有节点执行的配置更新、缓存刷新等场景。
     * 所有Worker节点都会收到执行指令并独立执行。
     * </p>
     */
    BROADCAST(2, "广播执行", "任务在所有Worker节点上同时执行"),

    /**
     * 分片执行
     * <p>
     * 任务数据被分片后分配到多个Worker节点并行执行，适用于大数据量处理场景。
     * 每个Worker节点处理一部分数据，最终汇总结果。
     * </p>
     */
    SHARDING(3, "分片执行", "任务数据分片后在多个Worker节点并行执行"),

    /**
     * 工作流触发执行
     * <p>
     * 任务由Camunda BPM工作流引擎触发执行，适用于需要复杂流程编排的业务场景。
     * 任务执行由BPMN流程定义驱动，支持审批、分支、循环等复杂逻辑。
     * </p>
     */
    WORKFLOW(4, "工作流执行", "任务由Camunda工作流引擎触发执行");

    /**
     * 执行类型代码
     */
    private final Integer code;

    /**
     * 执行类型名称
     */
    private final String name;

    /**
     * 执行类型描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        执行类型代码
     * @param name        执行类型名称
     * @param description 执行类型描述
     */
    ExecuteType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据代码获取执行类型
     *
     * @param code 执行类型代码
     * @return 执行类型枚举，如果未找到则返回null
     */
    public static ExecuteType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ExecuteType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
