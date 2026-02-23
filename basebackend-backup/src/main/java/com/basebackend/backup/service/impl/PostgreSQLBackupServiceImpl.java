package com.basebackend.backup.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.enums.BackupType;
import com.basebackend.backup.infrastructure.executor.impl.PostgresWalParser;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.service.PostgreSQLBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PostgreSQL备份服务实现
 *
 * @author BaseBackend
 */
@Slf4j
@RequiredArgsConstructor
public class PostgreSQLBackupServiceImpl implements PostgreSQLBackupService {

    private final BackupProperties backupProperties;
    private final RetryTemplate retryTemplate;
    private final LockManager lockManager;
    private final ChecksumService checksumService;
    private final PostgresWalParser walParser;

    private final ConcurrentMap<String, BackupRecord> backupCache = new ConcurrentHashMap<>();

    @Override
    public BackupRecord fullBackup() {
        log.info("开始执行PostgreSQL全量备份...");
        String lockKey = backupProperties.getDistributedLock().getKeyPrefix() + "postgres:full";
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
            log.error("PostgreSQL全量备份失败（含重试后）", e);
            return lastRecord[0] != null ? lastRecord[0] : failedRecord("full", e.getMessage());
        }
    }

    private BackupRecord doFullBackup() throws Exception {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        String backupId = IdUtil.fastSimpleUUID();
        LocalDateTime startTime = LocalDateTime.now();

        BackupRecord record = BackupRecord.builder()
                .backupId(backupId)
                .backupType(BackupType.FULL)
                .status(BackupStatus.RUNNING)
                .databaseName(pg.getDatabase())
                .startTime(startTime)
                .createTime(startTime)
                .build();

        String backupFile = null;
        try {
            String backupDir = pg.getBackupPath() + File.separator + "full";
            FileUtil.mkdir(backupDir);

            String timestamp = DateUtil.format(DateUtil.date(), "yyyyMMdd_HHmmss");
            backupFile = backupDir + File.separator + pg.getDatabase() + "_" + timestamp + ".sql";

            // 记录备份前的WAL位置
            String walPosition = walParser.getCurrentPosition(
                    pg.getHost(), pg.getPort(), pg.getUsername(), pg.getPassword(), pg.getDatabase());

            List<String> command = buildPgDumpCommand(backupFile);
            logCommand(command);

            ProcessBuilder pb = new ProcessBuilder(command);
            applyPgPassword(pb, pg.getPassword());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            consumeProcessOutput(process.getInputStream());

            boolean completed = process.waitFor(
                    backupProperties.getRetry().getMaxAttempts() * 60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("pg_dump执行超时");
            }

            int exitCode = process.exitValue();
            LocalDateTime endTime = LocalDateTime.now();
            long duration = ChronoUnit.SECONDS.between(startTime, endTime);

            if (exitCode == 0) {
                File file = new File(backupFile);
                record.setStatus(BackupStatus.SUCCESS);
                record.setFilePath(backupFile);
                record.setFileSize(file.length());
                record.setEndTime(endTime);
                record.setDuration(duration);
                record.setBinlogFile(walPosition); // 复用字段存储WAL LSN

                try {
                    var checksum = checksumService.computeChecksum(Paths.get(backupFile));
                    log.info("PostgreSQL全量备份校验完成 MD5={}, SHA256={}",
                            checksum.getMd5(), checksum.getSha256());
                } catch (Exception ce) {
                    log.warn("备份校验失败（不影响备份结果）", ce);
                }

                log.info("PostgreSQL全量备份成功: {} ({}), WAL LSN: {}",
                        backupFile, FileUtil.readableFileSize(file.length()), walPosition);
            } else {
                record.setStatus(BackupStatus.FAILED);
                record.setErrorMessage("pg_dump退出码: " + exitCode);
                record.setEndTime(endTime);
                record.setDuration(duration);
                log.error("PostgreSQL全量备份失败, pg_dump退出码: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("PostgreSQL全量备份异常", e);
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setEndTime(LocalDateTime.now());

            if (backupFile != null) {
                try {
                    FileUtil.del(backupFile);
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
        log.info("开始执行PostgreSQL增量备份(WAL归档)...");
        String lockKey = backupProperties.getDistributedLock().getKeyPrefix() + "postgres:incremental";
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
            log.error("PostgreSQL增量备份失败（含重试后）", e);
            return lastRecord[0] != null ? lastRecord[0] : failedRecord("incremental", e.getMessage());
        }
    }

    private BackupRecord doIncrementalBackup() throws Exception {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        String backupId = IdUtil.fastSimpleUUID();
        LocalDateTime startTime = LocalDateTime.now();

        BackupRecord record = BackupRecord.builder()
                .backupId(backupId)
                .backupType(BackupType.INCREMENTAL)
                .status(BackupStatus.RUNNING)
                .databaseName(pg.getDatabase())
                .startTime(startTime)
                .createTime(startTime)
                .build();

        String backupFile = null;
        try {
            String backupDir = pg.getBackupPath() + File.separator + "incremental";
            FileUtil.mkdir(backupDir);

            // 获取起始WAL位置
            String startLsn = findLastIncrementalEndLsn();
            if (startLsn == null) {
                startLsn = walParser.getCurrentPosition(
                        pg.getHost(), pg.getPort(), pg.getUsername(), pg.getPassword(), pg.getDatabase());
                log.info("首次增量备份，使用当前WAL LSN: {}", startLsn);
            }

            // 获取当前WAL位置
            String endLsn = walParser.getCurrentPosition(
                    pg.getHost(), pg.getPort(), pg.getUsername(), pg.getPassword(), pg.getDatabase());

            if (startLsn.equals(endLsn)) {
                log.info("WAL无变化，跳过增量备份");
                record.setStatus(BackupStatus.SUCCESS);
                record.setEndTime(LocalDateTime.now());
                record.setDuration(0L);
                backupCache.put(backupId, record);
                return record;
            }

            // 使用pg_dump在两个LSN之间导出变更（近似增量）
            String timestamp = DateUtil.format(DateUtil.date(), "yyyyMMdd_HHmmss");
            backupFile = backupDir + File.separator
                    + pg.getDatabase() + "_incr_" + timestamp + ".sql";

            // pg_dump with --data-only for incremental-like backup
            List<String> command = buildPgDumpIncrementalCommand(backupFile);
            logCommand(command);

            ProcessBuilder pb = new ProcessBuilder(command);
            applyPgPassword(pb, pg.getPassword());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            consumeProcessOutput(process.getInputStream());

            boolean completed = process.waitFor(
                    backupProperties.getRetry().getMaxAttempts() * 60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("pg_dump增量执行超时");
            }

            int exitCode = process.exitValue();
            LocalDateTime endTime = LocalDateTime.now();
            long duration = ChronoUnit.SECONDS.between(startTime, endTime);

            if (exitCode == 0) {
                File file = new File(backupFile);
                record.setStatus(BackupStatus.SUCCESS);
                record.setFilePath(backupFile);
                record.setFileSize(file.length());
                record.setEndTime(endTime);
                record.setDuration(duration);
                record.setBinlogStartPosition(startLsn);
                record.setBinlogEndPosition(endLsn);

                log.info("PostgreSQL增量备份成功: {}, WAL LSN范围: {} -> {}, 大小: {}",
                        backupFile, startLsn, endLsn, FileUtil.readableFileSize(file.length()));
            } else {
                record.setStatus(BackupStatus.FAILED);
                record.setErrorMessage("pg_dump退出码: " + exitCode);
                record.setEndTime(endTime);
                record.setDuration(duration);
                log.error("PostgreSQL增量备份失败, pg_dump退出码: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("PostgreSQL增量备份异常", e);
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setEndTime(LocalDateTime.now());

            if (backupFile != null) {
                try {
                    FileUtil.del(backupFile);
                } catch (Exception cleanupEx) {
                    log.warn("清理临时增量备份文件失败: {}", backupFile, cleanupEx);
                }
            }

            backupCache.put(backupId, record);
            throw e;
        }

        backupCache.put(backupId, record);
        return record;
    }

    private String findLastIncrementalEndLsn() {
        return backupCache.values().stream()
                .filter(r -> r.getBackupType() == BackupType.INCREMENTAL)
                .filter(r -> r.getStatus() == BackupStatus.SUCCESS)
                .filter(r -> r.getBinlogEndPosition() != null)
                .max((a, b) -> a.getEndTime().compareTo(b.getEndTime()))
                .map(BackupRecord::getBinlogEndPosition)
                .orElse(null);
    }

    @Override
    public boolean restore(String backupId) {
        log.info("开始恢复PostgreSQL备份: {}", backupId);
        String lockKey = backupProperties.getDistributedLock().getKeyPrefix() + "postgres:restore:" + backupId;

        try {
            return retryTemplate.execute(() ->
                lockManager.withLock(lockKey, () -> doRestore(backupId))
            );
        } catch (Exception e) {
            log.error("PostgreSQL恢复失败（含重试后）", e);
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
            BackupProperties.PostgresConfig pg = backupProperties.getPostgres();

            List<String> command = Arrays.asList(
                    pg.getPsqlPath(),
                    "-h", pg.getHost(),
                    "-p", String.valueOf(pg.getPort()),
                    "-U", pg.getUsername(),
                    "-d", pg.getDatabase(),
                    "-f", record.getFilePath()
            );

            logCommand(command);

            ProcessBuilder pb = new ProcessBuilder(command);
            applyPgPassword(pb, pg.getPassword());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            consumeProcessOutput(process.getInputStream());

            boolean completed = process.waitFor(30, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                log.error("PostgreSQL恢复执行超时");
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("PostgreSQL恢复成功: {}", backupId);
                return true;
            } else {
                log.error("PostgreSQL恢复失败, psql退出码: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("PostgreSQL恢复异常", e);
            return false;
        }
    }

    @Override
    public boolean restoreToPointInTime(String targetTime) {
        log.info("开始PostgreSQL时间点恢复: {}", targetTime);
        log.warn("PostgreSQL PITR需配合WAL归档和recovery.conf，暂未实现");
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
            if (record.getFilePath() != null) {
                FileUtil.del(record.getFilePath());
            }
            backupCache.remove(backupId);
            log.info("删除PostgreSQL备份成功: {}", backupId);
            return true;
        } catch (Exception e) {
            log.error("删除PostgreSQL备份失败", e);
            return false;
        }
    }

    @Override
    public int cleanExpiredBackups() {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        log.info("开始清理过期PostgreSQL备份，保留天数: {}", pg.getRetentionDays());

        LocalDateTime expireTime = LocalDateTime.now().minusDays(pg.getRetentionDays());
        int cleanedCount = 0;

        List<String> expiredBackups = backupCache.values().stream()
                .filter(record -> record.getCreateTime().isBefore(expireTime))
                .map(BackupRecord::getBackupId)
                .collect(Collectors.toList());

        for (String id : expiredBackups) {
            if (deleteBackup(id)) {
                cleanedCount++;
            }
        }

        log.info("清理过期PostgreSQL备份完成，共清理 {} 个", cleanedCount);
        return cleanedCount;
    }

    private List<String> buildPgDumpCommand(String backupFile) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();

        return Arrays.asList(
                pg.getPgDumpPath(),
                "-h", pg.getHost(),
                "-p", String.valueOf(pg.getPort()),
                "-U", pg.getUsername(),
                "-d", pg.getDatabase(),
                "--format=plain",
                "--no-owner",
                "--no-privileges",
                "--file=" + backupFile
        );
    }

    private List<String> buildPgDumpIncrementalCommand(String backupFile) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();

        return Arrays.asList(
                pg.getPgDumpPath(),
                "-h", pg.getHost(),
                "-p", String.valueOf(pg.getPort()),
                "-U", pg.getUsername(),
                "-d", pg.getDatabase(),
                "--format=plain",
                "--data-only",
                "--no-owner",
                "--no-privileges",
                "--file=" + backupFile
        );
    }

    private void applyPgPassword(ProcessBuilder pb, String password) {
        if (password != null && !password.isEmpty()) {
            pb.environment().put("PGPASSWORD", password);
        }
    }

    private void logCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        log.info("执行命令: {}", String.join(" ", command));
    }

    private void consumeProcessOutput(InputStream inputStream) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("PostgreSQL进程输出: {}", line);
                }
            } catch (IOException e) {
                log.debug("读取进程输出失败", e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private BackupRecord failedRecord(String type, String message) {
        LocalDateTime now = LocalDateTime.now();
        BackupRecord record = BackupRecord.builder()
                .backupId(IdUtil.fastSimpleUUID())
                .backupType(BackupType.valueOf(type.toUpperCase()))
                .status(BackupStatus.FAILED)
                .databaseName(backupProperties.getPostgres().getDatabase())
                .startTime(now)
                .endTime(now)
                .createTime(now)
                .errorMessage(message)
                .build();
        backupCache.put(record.getBackupId(), record);
        return record;
    }
}
