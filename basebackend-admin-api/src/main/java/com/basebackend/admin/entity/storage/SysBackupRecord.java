package com.basebackend.admin.entity.storage;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 备份记录表
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_backup_record")
public class SysBackupRecord {

    /**
     * 备份ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 备份编号
     */
    private String backupCode;

    /**
     * 备份类型（full/incremental）
     */
    private String backupType;

    /**
     * 备份状态（running/success/failed）
     */
    private String status;

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
     * Binlog文件名
     */
    private String binlogFile;

    /**
     * Binlog位置
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

    /**
     * 创建人
     */
    private String createBy;
}
