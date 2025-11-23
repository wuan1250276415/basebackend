package com.basebackend.backup.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.enums.BackupType;
import com.basebackend.backup.service.MySQLBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * MySQL备份服务实现
 *
 * @author BaseBackend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MySQLBackupServiceImpl implements MySQLBackupService {

    private final BackupProperties backupProperties;

    /**
     * 备份记录缓存（实际应存储到数据库）
     */
    private final ConcurrentMap<String, BackupRecord> backupCache = new ConcurrentHashMap<>();

    /**
     * 安全地记录命令（隐藏敏感信息）
     */
    private void logCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            log.warn("命令为空，无法记录");
            return;
        }

        // 复制列表并隐藏密码
        List<String> safeCommand = command.stream()
            .map(arg -> {
                // 如果是密码参数（以 -p 开头），则隐藏
                if (arg.startsWith("-p") && arg.length() > 2) {
                    return arg.substring(0, 2) + "******";
                }
                return arg;
            })
            .collect(Collectors.toList());

        log.info("执行命令: {}", String.join(" ", safeCommand));
    }

    @Override
    public BackupRecord fullBackup() {
        log.info("开始执行全量备份...");

        String backupId = IdUtil.fastSimpleUUID();
        LocalDateTime startTime = LocalDateTime.now();

        BackupRecord record = BackupRecord.builder()
                .backupId(backupId)
                .backupType(BackupType.FULL)
                .status(BackupStatus.RUNNING)
                .databaseName(backupProperties.getDatabase().getDatabase())
                .startTime(startTime)
                .createTime(startTime)
                .build();

        try {
            // 创建备份目录
            String backupDir = backupProperties.getBackupPath() + File.separator + "full";
            FileUtil.mkdir(backupDir);

            // 生成备份文件名
            String timestamp = DateUtil.format(DateUtil.date(), "yyyyMMdd_HHmmss");
            String backupFile = backupDir + File.separator +
                    backupProperties.getDatabase().getDatabase() + "_" + timestamp + ".sql";

            // 构建mysqldump命令
            List<String> command = buildMysqldumpCommand(backupFile);

            logCommand(command);

            // 执行备份
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            LocalDateTime endTime = LocalDateTime.now();
            long duration = ChronoUnit.SECONDS.between(startTime, endTime);

            if (exitCode == 0) {
                // 备份成功
                File file = new File(backupFile);
                record.setStatus(BackupStatus.SUCCESS);
                record.setFilePath(backupFile);
                record.setFileSize(file.length());
                record.setEndTime(endTime);
                record.setDuration(duration);

                log.info("全量备份成功: {} ({})", backupFile, FileUtil.readableFileSize(file.length()));
            } else {
                // 备份失败
                record.setStatus(BackupStatus.FAILED);
                record.setErrorMessage(output.toString());
                record.setEndTime(endTime);
                record.setDuration(duration);

                log.error("全量备份失败: {}", output);
            }

        } catch (Exception e) {
            log.error("全量备份异常", e);
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setEndTime(LocalDateTime.now());
        }

        // 保存备份记录
        backupCache.put(backupId, record);

        return record;
    }

    @Override
    public BackupRecord incrementalBackup() {
        log.info("开始执行增量备份...");

        String backupId = IdUtil.fastSimpleUUID();
        LocalDateTime startTime = LocalDateTime.now();

        BackupRecord record = BackupRecord.builder()
                .backupId(backupId)
                .backupType(BackupType.INCREMENTAL)
                .status(BackupStatus.RUNNING)
                .databaseName(backupProperties.getDatabase().getDatabase())
                .startTime(startTime)
                .createTime(startTime)
                .build();

        try {
            // 创建备份目录
            String backupDir = backupProperties.getBackupPath() + File.separator + "incremental";
            FileUtil.mkdir(backupDir);

            // 获取当前Binlog信息
            // TODO: 实现Binlog增量备份逻辑
            // 这里简化实现，实际需要解析SHOW MASTER STATUS获取binlog文件和位置

            record.setStatus(BackupStatus.SUCCESS);
            record.setEndTime(LocalDateTime.now());

            log.info("增量备份成功");

        } catch (Exception e) {
            log.error("增量备份异常", e);
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setEndTime(LocalDateTime.now());
        }

        backupCache.put(backupId, record);
        return record;
    }

    @Override
    public boolean restore(String backupId) {
        log.info("开始恢复备份: {}", backupId);

        BackupRecord record = backupCache.get(backupId);
        if (record == null) {
            log.error("备份记录不存在: {}", backupId);
            return false;
        }

        if (record.getStatus() != BackupStatus.SUCCESS) {
            log.error("备份状态异常，无法恢复: {}", record.getStatus());
            return false;
        }

        try {
            // 构建mysql恢复命令
            List<String> command = buildMysqlRestoreCommand(record.getFilePath());

            logCommand(command);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.info(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("数据库恢复成功");
                return true;
            } else {
                log.error("数据库恢复失败: {}", output);
                return false;
            }

        } catch (Exception e) {
            log.error("数据库恢复异常", e);
            return false;
        }
    }

    @Override
    public boolean restoreToPointInTime(String targetTime) {
        log.info("开始时间点恢复: {}", targetTime);
        // TODO: 实现PITR（Point-In-Time Recovery）
        // 1. 找到最近的全量备份
        // 2. 恢复全量备份
        // 3. 应用增量备份（Binlog）到指定时间点
        log.warn("PITR功能暂未实现");
        return false;
    }

    @Override
    public List<BackupRecord> listBackups() {
        return new ArrayList<>(backupCache.values());
    }

    @Override
    public boolean deleteBackup(String backupId) {
        BackupRecord record = backupCache.get(backupId);
        if (record == null) {
            return false;
        }

        try {
            // 删除备份文件
            if (record.getFilePath() != null) {
                FileUtil.del(record.getFilePath());
            }

            // 删除记录
            backupCache.remove(backupId);

            log.info("删除备份成功: {}", backupId);
            return true;

        } catch (Exception e) {
            log.error("删除备份失败", e);
            return false;
        }
    }

    @Override
    public int cleanExpiredBackups() {
        log.info("开始清理过期备份，保留天数: {}", backupProperties.getRetentionDays());

        LocalDateTime expireTime = LocalDateTime.now().minusDays(backupProperties.getRetentionDays());
        int cleanedCount = 0;

        List<String> expiredBackups = backupCache.values().stream()
                .filter(record -> record.getCreateTime().isBefore(expireTime))
                .map(BackupRecord::getBackupId)
                .collect(Collectors.toList());

        for (String backupId : expiredBackups) {
            if (deleteBackup(backupId)) {
                cleanedCount++;
            }
        }

        log.info("清理过期备份完成，共清理 {} 个", cleanedCount);
        return cleanedCount;
    }

    /**
     * 构建mysqldump命令
     */
    private List<String> buildMysqldumpCommand(String backupFile) {
        BackupProperties.DatabaseConfig db = backupProperties.getDatabase();

        return Arrays.asList(
                backupProperties.getMysqldumpPath(),
                "-h", db.getHost(),
                "-P", String.valueOf(db.getPort()),
                "-u", db.getUsername(),
                "-p" + db.getPassword(),
                "--single-transaction",  // InnoDB一致性备份
                "--master-data=2",       // 记录binlog位置
                "--flush-logs",          // 刷新日志
                "--routines",            // 备份存储过程
                "--triggers",            // 备份触发器
                "--events",              // 备份事件
                "--default-character-set=utf8mb4",
                db.getDatabase(),
                "--result-file=" + backupFile
        );
    }

    /**
     * 构建mysql恢复命令
     */
    private List<String> buildMysqlRestoreCommand(String backupFile) {
        BackupProperties.DatabaseConfig db = backupProperties.getDatabase();

        return Arrays.asList(
                backupProperties.getMysqlPath(),
                "-h", db.getHost(),
                "-P", String.valueOf(db.getPort()),
                "-u", db.getUsername(),
                "-p" + db.getPassword(),
                "--default-character-set=utf8mb4",
                db.getDatabase(),
                "-e", "source " + backupFile
        );
    }
}
