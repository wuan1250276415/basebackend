package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 流程跟踪数据传输对象
 *
 * <p>
 * 用于展示流程实例的执行轨迹和当前状态，支持流程图可视化。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessTrackingDTO", description = "流程跟踪信息")
public class ProcessTrackingDTO {

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "12345")
    private String processInstanceId;

    /**
     * 流程定义 ID
     */
    @Schema(description = "流程定义 ID", example = "order_approval:1:12345")
    private String processDefinitionId;

    /**
     * 流程定义 Key
     */
    @Schema(description = "流程定义 Key", example = "order_approval")
    private String processDefinitionKey;

    /**
     * 流程实例名称
     */
    @Schema(description = "流程实例名称", example = "订单审批-20250115")
    private String processInstanceName;

    /**
     * 业务键
     */
    @Schema(description = "业务键", example = "ORDER-001")
    private String businessKey;

    /**
     * 流程是否结束
     */
    @Schema(description = "流程是否结束", example = "false")
    private Boolean ended;

    /**
     * 流程是否挂起
     */
    @Schema(description = "流程是否挂起", example = "false")
    private Boolean suspended;

    /**
     * 流程开始时间
     */
    @Schema(description = "流程开始时间")
    private Date startTime;

    /**
     * 流程结束时间
     */
    @Schema(description = "流程结束时间")
    private Date endTime;

    /**
     * BPMN XML 内容
     */
    @Schema(description = "BPMN XML 内容")
    private String bpmnXml;

    /**
     * 当前活动节点 ID 列表（进行中）
     */
    @Schema(description = "当前活动节点 ID 列表（蓝色高亮）")
    private List<String> activeActivityIds;

    /**
     * 已完成活动节点 ID 列表
     */
    @Schema(description = "已完成活动节点 ID 列表（绿色高亮）")
    private List<String> completedActivityIds;

    /**
     * 待执行活动节点 ID 列表（可选，需要流程分析）
     */
    @Schema(description = "待执行活动节点 ID 列表（灰色）")
    private List<String> pendingActivityIds;

    /**
     * 失败/异常活动节点 ID 列表
     */
    @Schema(description = "失败/异常活动节点 ID 列表（红色高亮）")
    private List<String> failedActivityIds;

    /**
     * 活动历史详情列表
     */
    @Schema(description = "活动历史详情列表")
    private List<ActivityHistoryDTO> activityHistories;

    /**
     * 活动历史详情内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ActivityHistoryDTO", description = "活动历史详情")
    public static class ActivityHistoryDTO {

        /**
         * 活动历史 ID
         */
        @Schema(description = "活动历史 ID")
        private String id;

        /**
         * 活动 ID（BPMN 节点 ID）
         */
        @Schema(description = "活动 ID", example = "Task_1")
        private String activityId;

        /**
         * 活动名称
         */
        @Schema(description = "活动名称", example = "部门经理审批")
        private String activityName;

        /**
         * 活动类型
         */
        @Schema(description = "活动类型", example = "userTask")
        private String activityType;

        /**
         * 执行人 ID
         */
        @Schema(description = "执行人 ID")
        private String assignee;

        /**
         * 任务 ID（如果是用户任务）
         */
        @Schema(description = "关联任务 ID")
        private String taskId;

        /**
         * 开始时间
         */
        @Schema(description = "活动开始时间")
        private Date startTime;

        /**
         * 结束时间
         */
        @Schema(description = "活动结束时间")
        private Date endTime;

        /**
         * 持续时间（毫秒）
         */
        @Schema(description = "持续时间（毫秒）")
        private Long durationInMillis;

        /**
         * 是否已结束
         */
        @Schema(description = "是否已结束")
        private Boolean ended;

        /**
         * 是否取消
         */
        @Schema(description = "是否取消")
        private Boolean canceled;
    }
}
