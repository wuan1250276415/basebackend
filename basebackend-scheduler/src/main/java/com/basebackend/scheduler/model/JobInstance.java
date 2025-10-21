package com.basebackend.scheduler.model;

import com.basebackend.scheduler.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务实例模型
 * 每次任务执行都会创建一个实例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private Long id;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * PowerJob实例ID
     */
    private Long powerJobInstanceId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 实例状态
     */
    private JobStatus status;

    /**
     * 执行参数
     */
    private String params;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行机器地址
     */
    private String workerAddress;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 预期触发时间
     */
    private LocalDateTime expectedTriggerTime;

    /**
     * 实际触发时间
     */
    private LocalDateTime actualTriggerTime;

    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 执行耗时(毫秒)
     */
    private Long duration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 计算执行耗时
     */
    public void calculateDuration() {
        if (startTime != null && finishTime != null) {
            this.duration = java.time.Duration.between(startTime, finishTime).toMillis();
        }
    }

    /**
     * 判断是否执行成功
     */
    public boolean isSuccess() {
        return JobStatus.SUCCESS.equals(status);
    }

    /**
     * 判断是否执行失败
     */
    public boolean isFailed() {
        return JobStatus.FAILED.equals(status);
    }
}
