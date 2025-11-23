package com.basebackend.backup.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 恢复记录实体类
 * 用于记录每次恢复操作的执行结果
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("restore_record")
public class RestoreRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的备份任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 关联的备份历史ID（用于指定要恢复的备份）
     */
    @TableField("history_id")
    private Long historyId;

    /**
     * PITR目标时间点（格式：yyyy-MM-dd HH:mm:ss）
     * 如果为空，则恢复到备份时的状态
     */
    @TableField("target_point")
    private String targetPoint;

    /**
     * 恢复状态：SUCCESS/FAILED/RUNNING
     */
    @TableField("status")
    private String status;

    /**
     * 恢复开始时间
     */
    @TableField(value = "started_at", fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    /**
     * 恢复结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 恢复耗时（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 错误信息（失败时记录）
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 恢复操作者
     */
    @TableField("operator")
    private String operator;

    /**
     * 恢复备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 恢复开始时间戳（毫秒）
     */
    @TableField("started_at_ms")
    private Long startedAtMs;

    /**
     * 恢复结束时间戳（毫秒）
     */
    @TableField("finished_at_ms")
    private Long finishedAtMs;

    /**
     * 计算耗时（秒）
     */
    public void calculateDuration() {
        if (startedAt != null && finishedAt != null) {
            this.durationSeconds = (int) (finishedAt.toEpochSecond(java.time.ZoneOffset.UTC)
                - startedAt.toEpochSecond(java.time.ZoneOffset.UTC));
        }
    }

    /**
     * 判断恢复是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * 判断恢复是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * 判断恢复是否正在运行
     */
    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    /**
     * 判断是否为PITR恢复
     */
    public boolean isPITR() {
        return targetPoint != null && !targetPoint.trim().isEmpty();
    }
}
