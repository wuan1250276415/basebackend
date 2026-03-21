package com.basebackend.backup.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.enums.BackupType;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
public class MySQLBackupServiceImpl implements BackupService {

    private final BackupProperties backupProperties;
    private final RetryTemplate retryTemplate;
    private final LockManager lockManager;
    private final ChecksumService checksumService;

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
        String lockKey = backupProperties.getDistributedLock().getKeyPrefix() + "mysql:full";
        final BackupRecord[] lastRecord = new BackupRecord[1];

        try {
            return retryTemplate.execute(() ->
                lockManager.withLock(lockKey, () -> {
                    BackupRecord record = doFullBackup();
                    lastRecord[0] = record;
                    return record;
                })
            );
        } catch (Exception e) {
            log.error("全量备份失败（含重试后）", e);
            return lastRecord[0] != null ? lastRecord[0] : failedRecord("full", e.getMessage());
        }
    }

    private BackupRecord doFullBackup() throws Exception {
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

        String backupFile = null;
        try {
            // 创建备份目录
            String backupDir = backupProperties.getBackupPath() + File.separator + "full";
            FileUtil.mkdir(backupDir);

            // 生成备份文件名
            String timestamp = DateUtil.format(DateUtil.date(), "yyyyMMdd_HHmmss");
            backupFile = backupDir + File.separator +
                    backupProperties.getDatabase().getDatabase() + "_" + timestamp + ".sql";

            // 构建mysqldump命令
            List<String> command = buildMysqldumpCommand(backupFile);

            logCommand(command);
            CommandExecutionResult executionResult = executeCommand(command, "MySQL全量备份");

            if (shouldRetryWithoutBinlogMetadata(executionResult.output())) {
                log.warn("当前账号缺少MySQL复制相关权限，降级为普通全量备份: {}",
                        summarizeProcessOutput(executionResult.output()));
                FileUtil.del(backupFile);

                command = buildMysqldumpCommand(backupFile, false);
                logCommand(command);
                executionResult = executeCommand(command, "MySQL全量备份(降级)");
            }

            int exitCode = executionResult.exitCode();
            String output = executionResult.output();

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
                try {
                    var checksum = checksumService.computeChecksum(Paths.get(backupFile));
                    log.info("全量备份校验完成 MD5={}, SHA256={}", checksum.getMd5(), checksum.getSha256());
                } catch (Exception ce) {
                    log.warn("备份校验失败（不影响备份结果）", ce);
                }

                log.info("全量备份成功: {} ({})", backupFile, FileUtil.readableFileSize(file.length()));
            } else {
                // 备份失败
                record.setStatus(BackupStatus.FAILED);
                record.setErrorMessage(output.isBlank() ? "mysqldump执行失败, 退出码: " + exitCode : output);
                record.setEndTime(endTime);
                record.setDuration(duration);

                log.error("全量备份失败: {}", output);
            }

        } catch (Exception e) {
            log.error("全量备份异常", e);
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setEndTime(LocalDateTime.now());

            // 异常时清理临时文件
            if (backupFile != null) {
                try {
                    FileUtil.del(backupFile);
                    log.info("已清理临时备份文件: {}", backupFile);
                } catch (Exception cleanupEx) {
                    log.warn("清理临时备份文件失败: {}", backupFile, cleanupEx);
                }
            }

            backupCache.put(backupId, record);
            throw e;
        }

        backupCache.put(backupId, record);
        return record;
    }

    @Override
    public BackupRecord incrementalBackup() {
        log.info("开始执行增量备份...");
        String lockKey = backupProperties.getDistributedLock().getKeyPrefix() + "mysql:incremental";
        final BackupRecord[] lastRecord = new BackupRecord[1];

        try {
            return retryTemplate.execute(() ->
                lockManager.withLock(lockKey, () -> {
                    BackupRecord record = doIncrementalBackup();
                    lastRecord[0] = record;
                    return record;
                })
            );
        } catch (Exception e) {
            log.error("增量备份失败（含重试后）", e);
            return lastRecord[0] != null ? lastRecord[0] : failedRecord("incremental", e.getMessage());
        }
    }

    private BackupRecord doIncrementalBackup() {
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

        String message = "MySQL增量备份尚未实现真实Binlog采集与回放链路，已禁止返回伪成功";
        record.setStatus(BackupStatus.FAILED);
        record.setErrorMessage(message);
        record.setEndTime(LocalDateTime.now());
        record.setDuration(ChronoUnit.SECONDS.between(startTime, record.getEndTime()));
        log.warn(message);

        backupCache.put(backupId, record);
        return record;
    }

    @Override
    public boolean restore(String backupId) {
        log.info("开始恢复备份: {}", backupId);
        String lockKey = backupProperties.getDistributedLock().getKeyPrefix() + "mysql:restore:" + backupId;

        try {
            return retryTemplate.execute(() ->
                lockManager.withLock(lockKey, () -> doRestore(backupId))
            );
        } catch (Exception e) {
            log.error("数据库恢复失败（含重试后）", e);
            return false;
        }
    }

    private boolean doRestore(String backupId) {
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
            applyMysqlPassword(pb, backupProperties.getDatabase().getPassword());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuffer processOutput = new StringBuffer();
            Thread outputThread = consumeProcessOutput(process.getInputStream(), processOutput, "MySQL恢复");

            // 等待进程完成（设置超时）
            boolean completed = process.waitFor(getProcessTimeoutSeconds(), TimeUnit.SECONDS);

            if (!completed) {
                log.error("数据库恢复执行超时，强制终止进程");
                process.destroyForcibly();
                return false;
            }

            waitForOutputThread(outputThread, "MySQL恢复");
            int exitCode = process.exitValue();
            String output = processOutput.toString().trim();

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
        return buildMysqldumpCommand(backupFile, true);
    }

    private List<String> buildMysqldumpCommand(String backupFile, boolean includeBinlogMetadata) {
        BackupProperties.DatabaseConfig db = backupProperties.getDatabase();

        List<String> command = new ArrayList<>(Arrays.asList(
                backupProperties.getMysqldumpPath(),
                "-h", db.getHost(),
                "-P", String.valueOf(db.getPort()),
                "-u", db.getUsername(),
                "--single-transaction"
        ));

        if (includeBinlogMetadata) {
            command.add("--master-data=2");
            command.add("--flush-logs");
        }

        command.addAll(Arrays.asList(
                "--routines",
                "--triggers",
                "--events",
                "--default-character-set=utf8mb4",
                db.getDatabase(),
                "--result-file=" + backupFile
        ));
        return command;
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
                "--default-character-set=utf8mb4",
                db.getDatabase(),
                "-e", "source " + backupFile
        );
    }

    private BackupRecord failedRecord(String type, String message) {
        LocalDateTime now = LocalDateTime.now();
        BackupRecord record = BackupRecord.builder()
            .backupId(IdUtil.fastSimpleUUID())
            .backupType(BackupType.valueOf(type.toUpperCase()))
            .status(BackupStatus.FAILED)
            .databaseName(backupProperties.getDatabase().getDatabase())
            .startTime(now)
            .endTime(now)
            .createTime(now)
            .errorMessage(message)
            .build();
        backupCache.put(record.getBackupId(), record);
        return record;
    }

    private void applyMysqlPassword(ProcessBuilder processBuilder, String password) {
        if (password != null && !password.isEmpty()) {
            processBuilder.environment().put("MYSQL_PWD", password);
        }
    }

    private long getProcessTimeoutSeconds() {
        BackupProperties.Retry retry = backupProperties.getRetry();
        if (retry == null || retry.getMaxAttempts() <= 0) {
            return 60L;
        }
        return retry.getMaxAttempts() * 60L;
    }

    private boolean shouldRetryWithoutBinlogMetadata(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }

        return output.contains("Access denied")
                && (output.contains("FLUSH TABLES")
                || output.contains("FLUSH_TABLES")
                || output.contains("RELOAD")
                || output.contains("SHOW MASTER STATUS"));
    }

    private String summarizeProcessOutput(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        String firstLine = output.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse("");
        return firstLine.length() > 200 ? firstLine.substring(0, 200) : firstLine;
    }

    private CommandExecutionResult executeCommand(List<String> command, String operation) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyMysqlPassword(processBuilder, backupProperties.getDatabase().getPassword());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuffer processOutput = new StringBuffer();
        Thread outputThread = consumeProcessOutput(process.getInputStream(), processOutput, operation);

        boolean completed = process.waitFor(getProcessTimeoutSeconds(), TimeUnit.SECONDS);
        if (!completed) {
            log.error("{}执行超时，强制终止进程", operation);
            process.destroyForcibly();
            throw new RuntimeException(operation + "执行超时");
        }

        waitForOutputThread(outputThread, operation);
        return new CommandExecutionResult(process.exitValue(), processOutput.toString().trim());
    }

    /**
     * 消费进程输出（避免进程阻塞）
     */
    private Thread consumeProcessOutput(InputStream inputStream, StringBuffer outputBuffer, String operation) {
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuffer.append(line).append(System.lineSeparator());
                    log.debug("MySQL进程输出: {}", line);
                }
            } catch (IOException e) {
                log.debug("{}读取进程输出失败", operation, e);
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
        return outputThread;
    }

    private void waitForOutputThread(Thread outputThread, String operation) {
        if (outputThread == null) {
            return;
        }
        try {
            outputThread.join(TimeUnit.SECONDS.toMillis(5));
            if (outputThread.isAlive()) {
                log.warn("{}输出采集线程未在预期时间内结束", operation);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待{}输出采集线程结束时被中断", operation, e);
        }
    }

    private record CommandExecutionResult(int exitCode, String output) {
    }

    @Override
    public String getDatasourceType() {
        return "mysql";
    }
}
