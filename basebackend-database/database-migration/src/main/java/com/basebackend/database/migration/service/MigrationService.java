package com.basebackend.database.migration.service;

import com.basebackend.database.migration.model.MigrationConfirmation;
import com.basebackend.database.migration.model.MigrationInfo;

import java.util.List;

/**
 * 数据库迁移服务接口
 * 
 * @author basebackend
 */
public interface MigrationService {

    /**
     * 执行数据库迁移
     * 
     * @return 迁移结果信息
     */
    String migrate();

    /**
     * 执行数据库迁移（带备份）
     * 
     * @param createBackup 是否创建备份
     * @return 迁移结果信息
     */
    String migrateWithBackup(boolean createBackup);

    /**
     * 执行数据库迁移（生产环境，需要确认）
     * 
     * @param confirmation 确认信息
     * @return 迁移结果信息
     */
    String migrateWithConfirmation(MigrationConfirmation confirmation);

    /**
     * 回滚到指定版本
     * 
     * @param targetVersion 目标版本
     * @return 回滚结果
     */
    String rollbackToVersion(String targetVersion);

    /**
     * 验证迁移脚本
     * 
     * @return 验证结果
     */
    String validate();

    /**
     * 获取迁移历史
     * 
     * @return 迁移历史列表
     */
    List<MigrationInfo> getMigrationHistory();

    /**
     * 获取待执行的迁移
     * 
     * @return 待执行的迁移列表
     */
    List<MigrationInfo> getPendingMigrations();

    /**
     * 获取当前数据库版本
     * 
     * @return 当前版本号
     */
    String getCurrentVersion();

    /**
     * 清理失败的迁移记录
     * 
     * @return 清理结果
     */
    String repair();

    /**
     * 获取迁移信息
     * 
     * @return 迁移信息摘要
     */
    String info();

    /**
     * 基线化数据库（将现有数据库标记为特定版本）
     * 
     * @param version 基线版本
     * @return 基线化结果
     */
    String baseline(String version);

    /**
     * 生成确认令牌（用于生产环境迁移）
     * 
     * @return 确认令牌
     */
    String generateConfirmationToken();

    /**
     * 验证确认令牌
     * 
     * @param token 确认令牌
     * @return 是否有效
     */
    boolean validateConfirmationToken(String token);
}
