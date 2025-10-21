package com.basebackend.admin.service.storage;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.admin.entity.storage.SysBackupRecord;

import java.util.List;

/**
 * 备份管理服务
 *
 * @author BaseBackend
 */
public interface SysBackupService extends IService<SysBackupRecord> {

    /**
     * 手动触发全量备份
     *
     * @return 备份记录ID
     */
    Long triggerFullBackup();

    /**
     * 手动触发增量备份
     *
     * @return 备份记录ID
     */
    Long triggerIncrementalBackup();

    /**
     * 恢复数据库
     *
     * @param backupId 备份记录ID
     * @return 是否成功
     */
    boolean restoreDatabase(Long backupId);

    /**
     * 获取备份列表
     *
     * @param backupType 备份类型
     * @param status 状态
     * @return 备份列表
     */
    List<SysBackupRecord> listBackups(String backupType, String status);

    /**
     * 删除备份
     *
     * @param backupId 备份ID
     * @return 是否成功
     */
    boolean deleteBackup(Long backupId);

    /**
     * 清理过期备份
     *
     * @return 清理数量
     */
    int cleanExpiredBackups();
}
