package com.basebackend.backup.service;

import com.basebackend.backup.entity.BackupRecord;

import java.util.List;

/**
 * 统一备份服务接口
 * <p>
 * 适用于所有数据源（MySQL、PostgreSQL等）的备份操作。
 * </p>
 *
 * @author BaseBackend
 */
public interface BackupService {

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

    /**
     * 获取数据源类型标识
     *
     * @return 数据源类型，如 "mysql"、"postgresql"
     */
    String getDatasourceType();
}
