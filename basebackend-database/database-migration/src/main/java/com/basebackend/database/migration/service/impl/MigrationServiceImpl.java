package com.basebackend.database.migration.service.impl;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.MigrationException;
import com.basebackend.database.migration.model.MigrationBackup;
import com.basebackend.database.migration.model.MigrationConfirmation;
import com.basebackend.database.migration.model.MigrationInfo;
import com.basebackend.database.migration.service.MigrationBackupService;
import com.basebackend.database.migration.service.MigrationService;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.BaselineResult;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.RepairResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据库迁移服务实现
 * 
 * @author basebackend
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class MigrationServiceImpl implements MigrationService {

    private final Flyway flyway;
    private final MigrationBackupService backupService;
    private final DatabaseEnhancedProperties properties;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public MigrationServiceImpl(Flyway flyway, 
                                MigrationBackupService backupService,
                                DatabaseEnhancedProperties properties) {
        this.flyway = flyway;
        this.backupService = backupService;
        this.properties = properties;
    }

    // 存储确认令牌（生产环境使用）
    private final Map<String, LocalDateTime> confirmationTokens = new ConcurrentHashMap<>();
    
    // 令牌有效期（分钟）
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    @Override
    public String migrate() {
        try {
            log.info("开始执行数据库迁移...");
            MigrateResult result = flyway.migrate();
            
            String message = String.format(
                "数据库迁移完成。目标版本: %s, 执行的迁移数: %d",
                result.targetSchemaVersion,
                result.migrationsExecuted
            );
            
            log.info(message);
            return message;
        } catch (Exception e) {
            log.error("数据库迁移失败", e);
            // 迁移失败时自动调用repair清理失败记录
            try {
                log.info("迁移失败，尝试自动清理失败记录...");
                repair();
            } catch (Exception repairEx) {
                log.warn("自动清理失败记录时出错", repairEx);
            }
            throw new MigrationException("数据库迁移失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String migrateWithBackup(boolean createBackup) {
        MigrationBackup backup = null;
        
        try {
            // 如果需要创建备份
            if (createBackup) {
                log.info("迁移前创建数据备份...");
                String nextVersion = getNextMigrationVersion();
                backup = backupService.createBackup(nextVersion, null);
                log.info("备份创建成功，备份ID: {}", backup.getBackupId());
            }

            // 执行迁移
            String result = migrate();
            
            if (createBackup) {
                return result + " (备份ID: " + backup.getBackupId() + ")";
            }
            return result;

        } catch (Exception e) {
            log.error("带备份的迁移失败", e);
            
            // 如果创建了备份，提示可以恢复
            if (backup != null && backup.getBackupId() != null) {
                String message = String.format(
                    "迁移失败: %s。已创建备份 (ID: %s)，可以使用该备份恢复数据。",
                    e.getMessage(),
                    backup.getBackupId()
                );
                throw new MigrationException(message, e);
            }
            
            throw new MigrationException("迁移失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String migrateWithConfirmation(MigrationConfirmation confirmation) {
        // 检查是否为生产环境
        if (!isProductionEnvironment()) {
            log.warn("非生产环境，跳过确认步骤");
            return migrateWithBackup(confirmation.getCreateBackup() != null && confirmation.getCreateBackup());
        }

        // 验证确认令牌
        if (confirmation.getConfirmationToken() == null || 
            !validateConfirmationToken(confirmation.getConfirmationToken())) {
            throw new MigrationException("无效的确认令牌或令牌已过期");
        }

        log.info("生产环境迁移确认通过，确认人: {}, 原因: {}", 
            confirmation.getConfirmedBy(), 
            confirmation.getReason());

        // 移除已使用的令牌
        confirmationTokens.remove(confirmation.getConfirmationToken());

        // 生产环境默认创建备份
        boolean createBackup = confirmation.getCreateBackup() == null || confirmation.getCreateBackup();
        return migrateWithBackup(createBackup);
    }

    @Override
    public String rollbackToVersion(String targetVersion) {
        try {
            log.info("开始回滚到版本: {}", targetVersion);

            // 获取当前版本
            String currentVersion = getCurrentVersion();
            
            if (currentVersion.equals(targetVersion)) {
                return "当前版本已经是目标版本: " + targetVersion;
            }

            // Flyway不直接支持回滚，需要通过repair和重新迁移实现
            // 1. 清理当前版本之后的迁移记录
            log.info("清理迁移记录...");
            repair();

            // 2. 使用baseline设置目标版本
            log.info("设置基线版本: {}", targetVersion);
            baseline(targetVersion);

            String message = String.format(
                "回滚完成。从版本 %s 回滚到 %s。注意：这只是标记了版本，实际数据需要从备份恢复。",
                currentVersion,
                targetVersion
            );

            log.info(message);
            return message;

        } catch (Exception e) {
            log.error("回滚失败", e);
            throw new MigrationException("回滚失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateConfirmationToken() {
        String token = UUID.randomUUID().toString();
        confirmationTokens.put(token, LocalDateTime.now());
        
        // 清理过期令牌
        cleanupExpiredTokens();
        
        log.info("生成确认令牌: {}, 有效期: {} 分钟", token, TOKEN_VALIDITY_MINUTES);
        return token;
    }

    @Override
    public boolean validateConfirmationToken(String token) {
        LocalDateTime createdTime = confirmationTokens.get(token);
        
        if (createdTime == null) {
            return false;
        }

        // 检查令牌是否过期
        LocalDateTime expiryTime = createdTime.plusMinutes(TOKEN_VALIDITY_MINUTES);
        return LocalDateTime.now().isBefore(expiryTime);
    }

    @Override
    public String validate() {
        try {
            log.info("开始验证迁移脚本...");
            flyway.validate();
            
            String message = "迁移脚本验证成功";
            log.info(message);
            return message;
        } catch (Exception e) {
            log.error("验证迁移脚本失败", e);
            throw new RuntimeException("验证迁移脚本失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MigrationInfo> getMigrationHistory() {
        MigrationInfoService infoService = flyway.info();
        return Arrays.stream(infoService.all())
            .map(this::convertToMigrationInfo)
            .collect(Collectors.toList());
    }

    @Override
    public List<MigrationInfo> getPendingMigrations() {
        MigrationInfoService infoService = flyway.info();
        return Arrays.stream(infoService.pending())
            .map(this::convertToMigrationInfo)
            .collect(Collectors.toList());
    }

    @Override
    public String getCurrentVersion() {
        MigrationInfoService infoService = flyway.info();
        org.flywaydb.core.api.MigrationInfo current = infoService.current();
        
        if (current == null) {
            return "No migrations applied yet";
        }
        
        return current.getVersion().toString();
    }

    @Override
    public String repair() {
        try {
            log.info("开始修复迁移记录...");
            RepairResult result = flyway.repair();
            
            String message = String.format(
                "迁移记录修复完成。删除的失败迁移数: %d, 对齐的校验和数: %d",
                result.migrationsRemoved.size(),
                result.migrationsAligned.size()
            );
            
            log.info(message);
            return message;
        } catch (Exception e) {
            log.error("修复迁移记录失败", e);
            throw new RuntimeException("修复迁移记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String info() {
        MigrationInfoService infoService = flyway.info();
        
        int total = infoService.all().length;
        int applied = infoService.applied().length;
        int pending = infoService.pending().length;
        
        String currentVersion = getCurrentVersion();
        
        return String.format(
            "数据库迁移信息 - 当前版本: %s, 总迁移数: %d, 已应用: %d, 待执行: %d",
            currentVersion, total, applied, pending
        );
    }

    @Override
    public String baseline(String version) {
        try {
            log.info("开始基线化数据库，版本: {}", version);
            
            // 如果提供了版本号，需要重新配置 Flyway
            Flyway flywayWithVersion = version != null && !version.isEmpty()
                ? Flyway.configure()
                    .configuration(flyway.getConfiguration())
                    .baselineVersion(version)
                    .load()
                : flyway;
            
            BaselineResult result = flywayWithVersion.baseline();
            
            String message = String.format(
                "数据库基线化完成。基线版本: %s",
                result.baselineVersion
            );
            
            log.info(message);
            return message;
        } catch (Exception e) {
            log.error("数据库基线化失败", e);
            throw new RuntimeException("数据库基线化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换 Flyway MigrationInfo 到自定义 MigrationInfo
     */
    private MigrationInfo convertToMigrationInfo(org.flywaydb.core.api.MigrationInfo info) {
        LocalDateTime installedOn = info.getInstalledOn() != null
            ? LocalDateTime.ofInstant(info.getInstalledOn().toInstant(), ZoneId.systemDefault())
            : null;
        
        return MigrationInfo.builder()
            .version(info.getVersion() != null ? info.getVersion().toString() : null)
            .description(info.getDescription())
            .type(info.getType() != null ? info.getType().name() : null)
            .script(info.getScript())
            .checksum(info.getChecksum())
            .installedRank(info.getInstalledRank())
            .installedOn(installedOn)
            .installedBy(info.getInstalledBy())
            .executionTime(info.getExecutionTime())
            .state(info.getState() != null ? info.getState().getDisplayName() : null)
            .success(info.getState() == MigrationState.SUCCESS)
            .build();
    }

    /**
     * 获取下一个待执行的迁移版本
     */
    private String getNextMigrationVersion() {
        List<MigrationInfo> pending = getPendingMigrations();
        if (pending.isEmpty()) {
            return getCurrentVersion();
        }
        return pending.get(0).getVersion();
    }

    /**
     * 判断是否为生产环境
     */
    private boolean isProductionEnvironment() {
        return activeProfile != null && 
               (activeProfile.contains("prod") || activeProfile.contains("production"));
    }

    /**
     * 清理过期的确认令牌
     */
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        confirmationTokens.entrySet().removeIf(entry -> {
            LocalDateTime expiryTime = entry.getValue().plusMinutes(TOKEN_VALIDITY_MINUTES);
            return now.isAfter(expiryTime);
        });
    }
}
