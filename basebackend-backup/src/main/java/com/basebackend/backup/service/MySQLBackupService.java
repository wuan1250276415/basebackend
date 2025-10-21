package com.basebackend.backup.service;

import com.basebackend.backup.entity.BackupRecord;

import java.util.List;

/**
 * MySQL备份服务接口
 *
 * @author BaseBackend
 */
public interface MySQLBackupService {

    /**
     * 执行全量备份
     *
     * @return 备份记录
     */
    BackupRecord fullBackup();

    /**
     * 执行增量备份
     *
     * @return 备份记录
     */
    BackupRecord incrementalBackup();

    /**
     * 恢复数据库到指定备份点
     *
     * @param backupId 备份ID
     * @return 是否成功
     */
    boolean restore(String backupId);

    /**
     * 恢复数据库到指定时间点（PITR）
     *
     * @param targetTime 目标时间
     * @return 是否成功
     */
    boolean restoreToPointInTime(String targetTime);

    /**
     * 获取备份列表
     *
     * @return 备份记录列表
     */
    List<BackupRecord> listBackups();

    /**
     * 删除备份
     *
     * @param backupId 备份ID
     * @return 是否成功
     */
    boolean deleteBackup(String backupId);

    /**
     * 清理过期备份
     *
     * @return 清理的备份数量
     */
    int cleanExpiredBackups();
}
