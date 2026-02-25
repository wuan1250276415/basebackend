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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessTrackingDTO", description = "流程跟踪信息")
public class ProcessTrackingDTO {

    @Schema(description = "流程实例 ID") private String processInstanceId;
    @Schema(description = "流程定义 ID") private String processDefinitionId;
    @Schema(description = "流程定义 Key") private String processDefinitionKey;
    @Schema(description = "流程实例名称") private String processInstanceName;
    @Schema(description = "业务键") private String businessKey;
    @Schema(description = "流程是否结束") private Boolean ended;
    @Schema(description = "流程是否挂起") private Boolean suspended;
    @Schema(description = "流程开始时间") private Date startTime;
    @Schema(description = "流程结束时间") private Date endTime;
    @Schema(description = "BPMN XML 内容") private String bpmnXml;
    @Schema(description = "当前活动节点 ID 列表") private List<String> activeActivityIds;
    @Schema(description = "已完成活动节点 ID 列表") private List<String> completedActivityIds;
    @Schema(description = "待执行活动节点 ID 列表") private List<String> pendingActivityIds;
    @Schema(description = "失败活动节点 ID 列表") private List<String> failedActivityIds;
    @Schema(description = "活动历史详情列表") private List<ActivityHistoryDTO> activityHistories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ActivityHistoryDTO", description = "活动历史详情")
    public static class ActivityHistoryDTO {
        @Schema(description = "活动历史 ID") private String id;
        @Schema(description = "活动 ID") private String activityId;
        @Schema(description = "活动名称") private String activityName;
        @Schema(description = "活动类型") private String activityType;
        @Schema(description = "执行人 ID") private String assignee;
        @Schema(description = "关联任务 ID") private String taskId;
        @Schema(description = "活动开始时间") private Date startTime;
        @Schema(description = "活动结束时间") private Date endTime;
        @Schema(description = "持续时间（毫秒）") private Long durationInMillis;
        @Schema(description = "是否已结束") private Boolean ended;
        @Schema(description = "是否取消") private Boolean canceled;
    }
}
