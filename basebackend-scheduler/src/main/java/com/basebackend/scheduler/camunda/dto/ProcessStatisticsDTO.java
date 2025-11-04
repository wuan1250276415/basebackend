package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流程统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总流程实例数
     */
    private Long totalInstances;

    /**
     * 运行中的流程实例数
     */
    private Long runningInstances;

    /**
     * 已完成的流程实例数
     */
    private Long completedInstances;

    /**
     * 已挂起的流程实例数
     */
    private Long suspendedInstances;

    /**
     * 已终止的流程实例数
     */
    private Long terminatedInstances;

    /**
     * 待办任务总数
     */
    private Long totalTasks;

    /**
     * 流程定义总数
     */
    private Long totalDefinitions;

    /**
     * 活跃流程定义数
     */
    private Long activeDefinitions;

    /**
     * 今日启动的流程实例数
     */
    private Long todayStarted;

    /**
     * 今日完成的流程实例数
     */
    private Long todayCompleted;

    /**
     * 本周启动的流程实例数
     */
    private Long weekStarted;

    /**
     * 本周完成的流程实例数
     */
    private Long weekCompleted;

    /**
     * 按流程定义分组的统计
     */
    private List<DefinitionStatistics> definitionStatistics;

    /**
     * 流程定义统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefinitionStatistics implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 流程定义Key
         */
        private String processDefinitionKey;

        /**
         * 流程定义名称
         */
        private String processDefinitionName;

        /**
         * 版本号
         */
        private Integer version;

        /**
         * 运行中实例数
         */
        private Long runningInstances;

        /**
         * 已完成实例数
         */
        private Long completedInstances;

        /**
         * 待办任务数
         */
        private Long pendingTasks;

        /**
         * 平均完成时间（毫秒）
         */
        private Long avgDurationInMillis;
    }
}
