package com.basebackend.backup.entity;

import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.enums.BackupType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 备份记录实体
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecord {

    /**
     * 备份ID
     */
    private String backupId;

    /**
     * 备份类型
     */
    private BackupType backupType;

    /**
     * 备份状态
     */
    private BackupStatus status;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 备份文件路径
     */
    private String filePath;

    /**
     * 备份文件大小（字节）
     */
    private Long fileSize;

    /**
     * Binlog文件名（仅增量备份）
     */
    private String binlogFile;

    /**
     * Binlog位置（仅增量备份）
     */
    private Long binlogPosition;

    /**
     * 备份开始时间
     */
    private LocalDateTime startTime;

    /**
     * 备份结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时（秒）
     */
    private Long duration;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
