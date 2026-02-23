package com.basebackend.backup.service;

import com.basebackend.backup.entity.BackupRecord;

import java.util.List;

/**
 * PostgreSQL备份服务接口
 *
 * @author BaseBackend
 */
public interface PostgreSQLBackupService {

    BackupRecord fullBackup();

    BackupRecord incrementalBackup();

    boolean restore(String backupId);

    boolean restoreToPointInTime(String targetTime);

    List<BackupRecord> listBackups();

    boolean deleteBackup(String backupId);

    int cleanExpiredBackups();
}
