package com.basebackend.backup.infrastructure.executor;

/**
 * 数据源备份执行器接口
 * 统一抽象不同数据源（MySQL、PostgreSQL、Redis等）的备份操作
 *
 * @param <T> 具体的请求类型（BackupRequest或IncrementalBackupRequest）
 */
public interface DataSourceBackupExecutor<T extends BackupRequest> {

    /**
     * 执行全量备份
     *
     * @param request 备份请求
     * @return 备份产物
     * @throws Exception 备份失败时抛出异常
     */
    BackupArtifact executeFull(T request) throws Exception;

    /**
     * 执行增量备份
     *
     * @param request 增量备份请求
     * @return 备份产物
     * @throws Exception 备份失败时抛出异常
     */
    BackupArtifact executeIncremental(IncrementalBackupRequest request) throws Exception;

    /**
     * 恢复备份
     *
     * @param artifact 备份产物
     * @param targetDatabase 目标数据库名称（可选）
     * @return 恢复是否成功
     * @throws Exception 恢复失败时抛出异常
     */
    boolean restore(BackupArtifact artifact, String targetDatabase) throws Exception;

    /**
     * 获取当前数据源的增量位置（binlog position或WAL position）
     *
     * @param request 请求对象
     * @return 当前增量位置
     * @throws Exception 获取失败时抛出异常
     */
    String getCurrentIncrementalPosition(T request) throws Exception;

    /**
     * 验证备份文件完整性
     *
     * @param artifact 备份产物
     * @return 验证是否通过
     * @throws Exception 验证失败时抛出异常
     */
    boolean verifyBackup(BackupArtifact artifact) throws Exception;

    /**
     * 获取支持的数据源类型
     *
     * @return 数据源类型标识符，如"mysql"、"postgres"、"redis"
     */
    String getSupportedDatasourceType();

    /**
     * 获取支持的功能特性
     *
     * @return 支持的特性列表，如["full_backup", "incremental_backup", "restore"]
     */
    String[] getSupportedFeatures();
}
