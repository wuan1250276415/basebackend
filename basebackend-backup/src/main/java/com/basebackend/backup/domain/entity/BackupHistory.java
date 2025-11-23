package com.basebackend.backup.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备份历史记录实体类
 * 用于记录每次备份的执行结果和元数据
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("backup_history")
public class BackupHistory implements Serializable {

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
     * 任务名称（冗余存储，便于查询）
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 备份状态：SUCCESS/FAILED/RUNNING
     */
    @TableField("status")
    private String status;

    /**
     * 备份类型：full/incremental
     */
    @TableField("backup_type")
    private String backupType;

    /**
     * 增量链基线全量备份ID（仅增量备份使用）
     */
    @TableField("base_full_id")
    private Long baseFullId;

    /**
     * MySQL binlog起始位置
     */
    @TableField("binlog_start")
    private String binlogStart;

    /**
     * MySQL binlog结束位置
     */
    @TableField("binlog_end")
    private String binlogEnd;

    /**
     * PostgreSQL WAL起始位置
     */
    @TableField("wal_start")
    private String walStart;

    /**
     * PostgreSQL WAL结束位置
     */
    @TableField("wal_end")
    private String walEnd;

    /**
     * 备份文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 存储位置列表（JSON格式，支持多副本）
     * 例如：[{"type":"local","path":"/data/backup/xxx.sql"},{"type":"s3","bucket":"xxx","key":"xxx"}]
     */
    @TableField("storage_locations")
    private String storageLocations;

    /**
     * MD5校验和
     */
    @TableField("checksum_md5")
    private String checksumMd5;

    /**
     * SHA256校验和
     */
    @TableField("checksum_sha256")
    private String checksumSha256;

    /**
     * 备份开始时间
     */
    @TableField(value = "started_at", fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    /**
     * 备份结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 备份耗时（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 错误信息（失败时记录）
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 备份开始时间戳（毫秒）
     */
    @TableField("started_at_ms")
    private Long startedAtMs;

    /**
     * 备份结束时间戳（毫秒）
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
     * 判断备份是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * 判断备份是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * 判断备份是否正在运行
     */
    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    /**
     * 判断是否为增量备份
     */
    public boolean isIncremental() {
        return "incremental".equals(backupType);
    }

    /**
     * 判断是否为全量备份
     */
    public boolean isFull() {
        return "full".equals(backupType);
    }
}
