package com.basebackend.database.migration.service;

import com.basebackend.database.migration.model.MigrationBackup;

import java.util.List;

/**
 * 迁移备份服务接口
 * 
 * @author basebackend
 */
public interface MigrationBackupService {

    /**
     * 创建迁移前备份
     * 
     * @param migrationVersion 迁移版本
     * @param tables 需要备份的表列表（为空则备份所有表）
     * @return 备份信息
     */
    MigrationBackup createBackup(String migrationVersion, List<String> tables);

    /**
     * 恢复备份
     * 
     * @param backupId 备份ID
     * @return 恢复结果
     */
    String restoreBackup(String backupId);

    /**
     * 获取备份列表
     * 
     * @return 备份列表
     */
    List<MigrationBackup> listBackups();

    /**
     * 删除备份
     * 
     * @param backupId 备份ID
     * @return 删除结果
     */
    String deleteBackup(String backupId);

    /**
     * 获取备份详情
     * 
     * @param backupId 备份ID
     * @return 备份信息
     */
    MigrationBackup getBackup(String backupId);
}
