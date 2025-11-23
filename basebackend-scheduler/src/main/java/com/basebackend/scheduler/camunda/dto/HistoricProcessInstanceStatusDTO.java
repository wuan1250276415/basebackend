package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

import java.time.Instant;

/**
 * 历史流程实例状态数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HistoricProcessInstanceStatusDTO", description = "历史流程实例状态信息")
public class HistoricProcessInstanceStatusDTO {

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "12345")
    private String instanceId;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "COMPLETED")
    private String state;

    /**
     * 是否已完成
     */
    @Schema(description = "是否已完成", example = "true")
    private boolean completed;

    /**
     * 是否已终止
     */
    @Schema(description = "是否已终止", example = "false")
    private boolean terminated;

    /**
     * 是否仍在运行/未结束
     */
    @Schema(description = "是否仍在运行/未结束", example = "false")
    private boolean running;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2025-01-01T11:00:00Z")
    private Instant endTime;

    /**
     * 删除原因（终止原因）
     */
    @Schema(description = "删除原因（终止原因）", example = "Cancelled by user")
    private String deleteReason;

    /**
     * 已完成的活动实例数量
     */
    @Schema(description = "已完成的活动实例数量", example = "5")
    private Long completedActivities;

    /**
     * 总活动实例数量
     */
    @Schema(description = "总活动实例数量", example = "10")
    private Long totalActivities;

    /**
     * 流程实例开始时间
     */
    @Schema(description = "开始时间", example = "2025-01-01T10:00:00Z")
    private Instant startTime;

    /**
     * 持续时间（毫秒）
     */
    @Schema(description = "持续时间（毫秒）", example = "3600000")
    private Long durationInMillis;

    /**
     * 从 Camunda HistoricProcessInstance 转换为 DTO
     *
     * @param instance Camunda 历史流程实例
     * @return DTO 对象
     */
    public static HistoricProcessInstanceStatusDTO from(HistoricProcessInstance instance) {
        if (instance == null) {
            return null;
        }
        String state = instance.getState();
        boolean completed = HistoricProcessInstance.STATE_COMPLETED.equals(state);
        boolean terminated = HistoricProcessInstance.STATE_EXTERNALLY_TERMINATED.equals(state)
                || HistoricProcessInstance.STATE_INTERNALLY_TERMINATED.equals(state);
        boolean running = HistoricProcessInstance.STATE_ACTIVE.equals(state)
                || HistoricProcessInstance.STATE_SUSPENDED.equals(state);

        return HistoricProcessInstanceStatusDTO.builder()
                .instanceId(instance.getId())
                .state(state)
                .completed(completed)
                .terminated(terminated)
                .running(running)
                .endTime(toInstant(instance.getEndTime()))
                .deleteReason(instance.getDeleteReason())
                .build();
    }

    /**
     * 将 Date 转换为 Instant
     *
     * @param date 日期
     * @return Instant
     */
    private static Instant toInstant(java.util.Date date) {
        return date == null ? null : date.toInstant();
    }

    /**
     * 设置完成活动数
     */
    public void setCompletedActivities(Long completedActivities) {
        this.completedActivities = completedActivities;
    }

    /**
     * 设置总活动数
     */
    public void setTotalActivities(Long totalActivities) {
        this.totalActivities = totalActivities;
    }

    /**
     * 设置开始时间
     */
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    /**
     * 设置持续时间
     */
    public void setDurationInMillis(Long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置结束时间（兼容性方法）
     * @param ended 结束时间
     */
    public void setEnded(Instant ended) {
        this.endTime = ended;
    }

    /**
     * 获取结束时间（兼容性方法）
     * @return 结束时间
     */
    public Instant getEnded() {
        return this.endTime;
    }
}
