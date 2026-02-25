package com.basebackend.backup.service;

/**
 * PostgreSQL备份服务接口
 *
 * @deprecated 使用 {@link BackupService} 替代，通过 {@link BackupService#getDatasourceType()} 区分数据源
 * @author BaseBackend
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public interface PostgreSQLBackupService extends BackupService {
}
