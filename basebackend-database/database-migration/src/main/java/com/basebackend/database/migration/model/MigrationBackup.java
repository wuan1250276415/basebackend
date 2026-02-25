package com.basebackend.database.migration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 迁移备份信息模型
 * 
 * @author basebackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationBackup {

    /**
     * 备份ID
     */
    private String backupId;

    /**
     * 迁移版本
     */
    private String migrationVersion;

    /**
     * 备份文件路径
     */
    private String backupPath;

    /**
     * 备份时间
     */
    private LocalDateTime backupTime;

    /**
     * 备份大小（字节）
     */
    private Long backupSize;

    /**
     * 备份状态 (SUCCESS, FAILED)
     */
    private String status;

    /**
     * 备份描述
     */
    private String description;

    /**
     * 是否已恢复
     */
    private Boolean restored;

    /**
     * 恢复时间
     */
    private LocalDateTime restoreTime;
}
