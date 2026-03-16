package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.infrastructure.executor.BackupArtifact;
import com.basebackend.backup.infrastructure.executor.BackupRequest;
import com.basebackend.backup.infrastructure.executor.DataSourceBackupExecutor;
import com.basebackend.backup.infrastructure.executor.IncrementalBackupRequest;
import com.basebackend.backup.infrastructure.monitoring.BackupMetricsRegistrar;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PostgreSQL备份执行器
 */
@Slf4j
@Component
public class PostgresBackupExecutor extends AbstractBackupExecutor
        implements DataSourceBackupExecutor<BackupRequest> {

    static final String REPLAYABLE_INCREMENTAL_MARKER =
            "-- basebackend-postgres-incremental-format: snapshot-v1";
    static final String WAL_DUMP_INCREMENTAL_MARKER =
            "-- basebackend-postgres-incremental-format: wal-dump-v1";
    static final String LEGACY_REPLAYABLE_SNAPSHOT_HEADER =
            "-- PostgreSQL replayable incremental snapshot";
    static final String WAL_DUMP_NO_CHANGES_HEADER =
            "-- PostgreSQL WAL incremental backup: no changes";
    static final String REPLAY_MODE_LOGICAL_SNAPSHOT = "logical_snapshot";
    static final String REPLAY_MODE_WAL_EXTERNAL = "wal_external";
    static final String REPLAY_MODE_WAL_PHYSICAL_BUILTIN = "wal_physical_builtin";
    static final String WAL_HEADER_START_PREFIX = "-- wal-start:";
    static final String WAL_HEADER_END_PREFIX = "-- wal-end:";
    static final String TEMPLATE_PLACEHOLDER_ARTIFACT = "${artifact}";
    static final String TEMPLATE_PLACEHOLDER_ARCHIVE_DIR = "${archiveDir}";
    static final String TEMPLATE_PLACEHOLDER_WAL_START = "${walStart}";
    static final String TEMPLATE_PLACEHOLDER_WAL_END = "${walEnd}";
    static final String PHYSICAL_RECOVERY_SIGNAL_FILE = "recovery.signal";
    static final String PHYSICAL_BASELINE_METADATA_FILE = "baseline.meta";
    static final String PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX = "rollback-failed-";
    private static final int REPLAYABLE_HEADER_SCAN_LINES = 16;
    private static final Pattern WAL_LSN_PATTERN = Pattern.compile("^[0-9A-F]+/[0-9A-F]+$");

    private final BackupProperties backupProperties;
    private final PostgresWalParser walParser;

    public PostgresBackupExecutor(
            LockManager lockManager,
            RetryTemplate retryTemplate,
            StorageProvider storageProvider,
            ChecksumService checksumService,
            BackupHistoryMapper backupHistoryMapper,
            @Nullable BackupMetricsRegistrar backupMetricsRegistrar,
            BackupProperties backupProperties,
            PostgresWalParser walParser) {
        super(lockManager, retryTemplate, storageProvider, checksumService, backupHistoryMapper, backupMetricsRegistrar);
        this.backupProperties = backupProperties;
        this.walParser = walParser;
    }

    @Override
    public BackupArtifact executeFull(BackupRequest request) throws Exception {
        BackupRequest.DatabaseConfig dbConfig = resolveDatabaseConfig(request);
        String outputFile = getOutputFilePath(request, "full");
        File backupFile = new File(outputFile);
        String walStartPosition = null;
        String walEndPosition = null;

        try {
            try {
                walStartPosition = walParser.getCurrentPosition(
                        dbConfig.getHost(),
                        dbConfig.getPort(),
                        dbConfig.getUsername(),
                        dbConfig.getPassword(),
                        dbConfig.getDatabase());
            } catch (Exception e) {
                log.warn("获取PostgreSQL全量备份起始WAL位点失败，将继续执行全量备份: taskId={}",
                        request.getTaskId(), e);
            }

            List<String> command = buildPgDumpCommandArgs(dbConfig, outputFile);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            applyPostgresPassword(processBuilder, dbConfig.getPassword());
            processBuilder.redirectErrorStream(true);

            logCommand(command);

            Process process = processBuilder.start();
            consumeProcessOutput(process.getInputStream());

            boolean completed = process.waitFor(
                    backupProperties.getRetry().getMaxAttempts() * 60L, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("pg_dump执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("pg_dump执行失败, 退出码: " + exitCode);
            }

            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件未生成: " + outputFile);
            }

            try {
                walEndPosition = walParser.getCurrentPosition(
                        dbConfig.getHost(),
                        dbConfig.getPort(),
                        dbConfig.getUsername(),
                        dbConfig.getPassword(),
                        dbConfig.getDatabase());
            } catch (Exception e) {
                log.warn("获取PostgreSQL全量备份结束WAL位点失败: taskId={}", request.getTaskId(), e);
            }

            LocalDateTime startTime = request.getStartTime() == null ? LocalDateTime.now() : request.getStartTime();
            LocalDateTime endTime = LocalDateTime.now();

            return BackupArtifact.builder()
                    .file(backupFile)
                    .backupType("full")
                    .fileSize(backupFile.length())
                    .startTime(startTime)
                    .endTime(endTime)
                    .walStartPosition(walStartPosition)
                    .walEndPosition(walEndPosition)
                    .durationSeconds(ChronoUnit.SECONDS.between(startTime, endTime))
                    .build();
        } catch (Exception e) {
            if (backupFile.exists()) {
                try {
                    Files.delete(backupFile.toPath());
                } catch (IOException cleanupEx) {
                    log.warn("清理临时备份文件失败: {}", outputFile, cleanupEx);
                }
            }
            throw e;
        }
    }

    @Override
    public BackupArtifact executeIncremental(IncrementalBackupRequest request) throws Exception {
        if (!isIncrementalReplayEnabled()) {
            throw new UnsupportedOperationException(
                    "PostgreSQL增量回放能力未启用，已禁止生成不可恢复的增量备份");
        }

        BackupRequest.DatabaseConfig dbConfig = resolveDatabaseConfig(request);
        String replayMode = resolveReplayMode();
        String startPosition = resolveStartWalPosition(request);
        String endPosition = normalizeWalLsn(walParser.getCurrentPosition(
                dbConfig.getHost(),
                dbConfig.getPort(),
                dbConfig.getUsername(),
                dbConfig.getPassword(),
                dbConfig.getDatabase()), "PostgreSQL增量备份结束WAL位点");
        validateWalRange(startPosition, endPosition);

        if (startPosition.equals(endPosition)) {
            String outputFile = getOutputFilePath(request, "incremental");
            File backupFile = new File(outputFile);
            try {
                if (isWalDumpReplayMode(replayMode)) {
                    writeWalDumpNoChangeFile(backupFile, startPosition);
                } else {
                    Files.writeString(backupFile.toPath(),
                            REPLAYABLE_INCREMENTAL_MARKER + "\n"
                                    + "-- PostgreSQL incremental backup: no changes\n"
                                    + "-- lsn: " + startPosition + "\n");
                }
                return buildIncrementalArtifact(request, backupFile, startPosition, endPosition);
            } catch (Exception e) {
                cleanupTempFile(backupFile, outputFile);
                throw e;
            }
        }

        String outputFile = getOutputFilePath(request, "incremental");
        File backupFile = new File(outputFile);
        try {
            if (isWalDumpReplayMode(replayMode)) {
                executeWalDumpIncremental(backupFile, startPosition, endPosition);
            } else {
                // 为保证“可回放优先”，回放能力开启后生成可直接被 psql 应用的增量快照脚本。
                executeReplayableIncrementalSnapshot(dbConfig, backupFile, startPosition, endPosition);
            }
            return buildIncrementalArtifact(request, backupFile, startPosition, endPosition);
        } catch (Exception e) {
            cleanupTempFile(backupFile, outputFile);
            throw e;
        }
    }

    @Override
    public boolean restore(BackupArtifact artifact, String targetDatabase) throws Exception {
        if (artifact == null || artifact.getFile() == null) {
            throw new IllegalArgumentException("恢复产物不能为空");
        }
        if ("incremental".equalsIgnoreCase(artifact.getBackupType())) {
            if (!isIncrementalReplayEnabled()) {
                throw new UnsupportedOperationException("PostgreSQL增量回放能力未启用，当前仅支持全量备份恢复");
            }
            String format = detectIncrementalFormat(artifact.getFile());
            if (REPLAYABLE_INCREMENTAL_MARKER.equals(format)
                    || LEGACY_REPLAYABLE_SNAPSHOT_HEADER.equals(format)) {
                return restoreByPsql(artifact.getFile(), targetDatabase);
            }
            if (WAL_DUMP_INCREMENTAL_MARKER.equals(format)) {
                String replayMode = resolveReplayMode();
                if (REPLAY_MODE_WAL_PHYSICAL_BUILTIN.equals(replayMode)) {
                    return restoreByBuiltinPhysicalReplay(artifact, targetDatabase);
                }
                if (REPLAY_MODE_WAL_EXTERNAL.equals(replayMode)) {
                    return restoreByWalReplayCommand(artifact, targetDatabase);
                }
                throw new UnsupportedOperationException(
                        "当前PostgreSQL增量回放模式不支持WAL导出型恢复，请切换到 wal_external 或 wal_physical_builtin");
            }
            throw new UnsupportedOperationException(
                    "检测到未知增量备份格式，无法回放: " + artifact.getFile().getAbsolutePath());
        }
        return restoreByPsql(artifact.getFile(), targetDatabase);
    }

    private boolean restoreByPsql(File inputFile, String targetDatabase) throws Exception {
        List<String> command = buildPsqlCommandArgs(targetDatabase, inputFile);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyPostgresPassword(processBuilder, backupProperties.getPostgres().getPassword());
        processBuilder.redirectErrorStream(true);

        logCommand(command);

        Process process = processBuilder.start();
        consumeProcessOutput(process.getInputStream());

        boolean completed = process.waitFor(
                backupProperties.getRetry().getMaxAttempts() * 60L, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            log.error("PostgreSQL恢复执行超时");
            return false;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("PostgreSQL恢复失败, 退出码: {}", exitCode);
            return false;
        }

        return true;
    }

    @Override
    public String getCurrentIncrementalPosition(BackupRequest request) throws Exception {
        BackupRequest.DatabaseConfig dbConfig = resolveDatabaseConfig(request);
        return walParser.getCurrentPosition(
                dbConfig.getHost(),
                dbConfig.getPort(),
                dbConfig.getUsername(),
                dbConfig.getPassword(),
                dbConfig.getDatabase());
    }

    @Override
    public boolean verifyBackup(BackupArtifact artifact) {
        return artifact != null
                && artifact.getFile() != null
                && artifact.getFile().exists()
                && artifact.getFile().length() > 0;
    }

    @Override
    public String getSupportedDatasourceType() {
        return "postgresql";
    }

    @Override
    public String[] getSupportedFeatures() {
        if (isIncrementalReplayEnabled()) {
            String replayMode = resolveReplayMode();
            if (REPLAY_MODE_WAL_EXTERNAL.equals(replayMode)) {
                return new String[]{"full_backup", "incremental_backup", "restore", "checksum_verification",
                        "wal_external_replay"};
            }
            if (REPLAY_MODE_WAL_PHYSICAL_BUILTIN.equals(replayMode)) {
                return new String[]{"full_backup", "incremental_backup", "restore", "checksum_verification",
                        "wal_physical_builtin_replay"};
            }
            return new String[]{"full_backup", "incremental_backup", "restore", "checksum_verification"};
        }
        return new String[]{"full_backup", "restore", "checksum_verification"};
    }

    private boolean isIncrementalReplayEnabled() {
        return backupProperties.getPostgres().isIncrementalReplayEnabled();
    }

    String resolveReplayMode() {
        String rawMode = backupProperties.getPostgres().getIncrementalReplayMode();
        if (rawMode == null || rawMode.isBlank()) {
            return REPLAY_MODE_LOGICAL_SNAPSHOT;
        }
        String normalized = rawMode.trim().toLowerCase(Locale.ROOT);
        if (REPLAY_MODE_WAL_EXTERNAL.equals(normalized)
                || REPLAY_MODE_WAL_PHYSICAL_BUILTIN.equals(normalized)) {
            return normalized;
        }
        return REPLAY_MODE_LOGICAL_SNAPSHOT;
    }

    private boolean isWalDumpReplayMode(String replayMode) {
        return REPLAY_MODE_WAL_EXTERNAL.equals(replayMode)
                || REPLAY_MODE_WAL_PHYSICAL_BUILTIN.equals(replayMode);
    }

    private void executeReplayableIncrementalSnapshot(BackupRequest.DatabaseConfig dbConfig,
                                                      File backupFile,
                                                      String startPosition,
                                                      String endPosition) throws Exception {
        List<String> tableNames = queryUserTables(dbConfig);
        writeIncrementalPreamble(backupFile, startPosition, endPosition, tableNames);

        List<String> command = buildPgDumpDataOnlyCommandArgs(dbConfig);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyPostgresPassword(processBuilder, dbConfig.getPassword());
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(backupFile));
        processBuilder.redirectErrorStream(true);

        logCommand(command);

        Process process = processBuilder.start();
        boolean completed = process.waitFor(
                backupProperties.getRetry().getMaxAttempts() * 60L, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("PostgreSQL增量快照导出超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorMsg = backupFile.exists() ? Files.readString(backupFile.toPath()) : "unknown";
            throw new RuntimeException("PostgreSQL增量快照导出失败, 退出码: " + exitCode + ", 错误: " + errorMsg);
        }

        if (!backupFile.exists() || backupFile.length() == 0) {
            throw new RuntimeException("PostgreSQL增量备份文件未生成: " + backupFile.getAbsolutePath());
        }
    }

    private void executeWalDumpIncremental(File backupFile,
                                           String startPosition,
                                           String endPosition) throws Exception {
        StringBuilder header = new StringBuilder();
        header.append(WAL_DUMP_INCREMENTAL_MARKER).append('\n');
        header.append(WAL_HEADER_START_PREFIX).append(' ').append(startPosition).append('\n');
        header.append(WAL_HEADER_END_PREFIX).append(' ').append(endPosition).append('\n');
        header.append('\n');
        Files.writeString(backupFile.toPath(), header.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        List<String> command = buildPgWalDumpCommandArgs(startPosition, endPosition);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(backupFile));
        processBuilder.redirectErrorStream(true);

        logCommand(command);

        Process process = processBuilder.start();
        boolean completed = process.waitFor(
                backupProperties.getRetry().getMaxAttempts() * 60L, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("pg_waldump执行超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorMsg = backupFile.exists() ? Files.readString(backupFile.toPath()) : "unknown";
            throw new RuntimeException("pg_waldump执行失败, 退出码: " + exitCode + ", 错误: " + errorMsg);
        }

        if (!backupFile.exists() || backupFile.length() == 0) {
            throw new RuntimeException("WAL增量备份文件未生成: " + backupFile.getAbsolutePath());
        }
    }

    private void writeWalDumpNoChangeFile(File backupFile, String position) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(WAL_DUMP_INCREMENTAL_MARKER).append('\n');
        sb.append(WAL_DUMP_NO_CHANGES_HEADER).append('\n');
        sb.append("-- wal-position: ").append(position).append('\n');
        Files.writeString(backupFile.toPath(), sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    String detectIncrementalFormat(File incrementalFile) throws Exception {
        if (incrementalFile == null || !incrementalFile.exists()) {
            throw new IllegalArgumentException("增量备份文件不存在，无法执行回放校验");
        }

        try (BufferedReader reader = Files.newBufferedReader(incrementalFile.toPath())) {
            String line;
            int scanned = 0;
            while ((line = reader.readLine()) != null && scanned < REPLAYABLE_HEADER_SCAN_LINES) {
                String normalizedLine = line.trim();
                if (REPLAYABLE_INCREMENTAL_MARKER.equals(normalizedLine)
                        || LEGACY_REPLAYABLE_SNAPSHOT_HEADER.equals(normalizedLine)) {
                    return REPLAYABLE_INCREMENTAL_MARKER;
                }
                if (WAL_DUMP_INCREMENTAL_MARKER.equals(normalizedLine)) {
                    return WAL_DUMP_INCREMENTAL_MARKER;
                }
                scanned++;
            }
        }

        return "unknown";
    }

    private boolean restoreByWalReplayCommand(BackupArtifact artifact, String targetDatabase) throws Exception {
        File file = artifact.getFile();
        if (file == null || !file.exists() || file.length() == 0) {
            throw new IllegalArgumentException("WAL增量回放文件不存在或为空");
        }
        if (containsLine(file, WAL_DUMP_NO_CHANGES_HEADER)) {
            log.info("PostgreSQL WAL增量恢复跳过：检测到无变更增量文件");
            return true;
        }

        String template = backupProperties.getPostgres().getWalReplayCommand();
        validateWalReplayTemplate(template);
        resolveAndValidateWalRangeForReplay(artifact, file);

        String commandText = renderWalReplayCommand(template, artifact, targetDatabase);
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", commandText);
        applyPostgresPassword(processBuilder, backupProperties.getPostgres().getPassword());
        processBuilder.redirectErrorStream(true);

        log.info("执行PostgreSQL WAL回放命令模板");
        Process process = processBuilder.start();
        consumeProcessOutput(process.getInputStream());

        boolean completed = process.waitFor(
                backupProperties.getRetry().getMaxAttempts() * 60L, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            log.error("PostgreSQL WAL回放执行超时");
            return false;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("PostgreSQL WAL回放执行失败, 退出码: {}", exitCode);
            return false;
        }

        return true;
    }

    private boolean restoreByBuiltinPhysicalReplay(BackupArtifact artifact, String targetDatabase) throws Exception {
        File file = artifact.getFile();
        if (file == null || !file.exists() || file.length() == 0) {
            throw new IllegalArgumentException("WAL增量回放文件不存在或为空");
        }
        if (containsLine(file, WAL_DUMP_NO_CHANGES_HEADER)) {
            log.info("PostgreSQL内建物理回放跳过：检测到无变更增量文件");
            return true;
        }
        if (targetDatabase != null && !targetDatabase.isBlank()) {
            log.info("PostgreSQL内建物理回放为实例级恢复，忽略targetDatabase参数: {}", targetDatabase);
        }

        resolveAndValidateWalRangeForReplay(artifact, file);
        validatePhysicalReplayConfig();

        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        Path dataDir = resolveRequiredDirectory(pg.getPhysicalReplayDataDir(), "physicalReplayDataDir");
        Path archiveDir = resolveRequiredDirectory(pg.getPhysicalReplayArchiveDir(), "physicalReplayArchiveDir");
        Path baselineRootDir = resolveOrCreateDirectory(pg.getPhysicalReplayBaselineDir(), "physicalReplayBaselineDir");
        Path baselineDir = createPhysicalBaseline(artifact, baselineRootDir);
        Path recoverySignal = dataDir.resolve(PHYSICAL_RECOVERY_SIGNAL_FILE);

        Files.writeString(recoverySignal, "",
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        boolean replaySuccess = false;
        try {
            String restoreCommand = renderPhysicalRestoreCommand(
                    pg.getPhysicalReplayRestoreCommandTemplate(),
                    artifact,
                    archiveDir);
            String startOptions = buildPhysicalStartOptions(restoreCommand, artifact.getWalEndPosition());

            int stopTimeout = Math.max(pg.getPhysicalReplayStopTimeoutSeconds(), 1);
            int startTimeout = Math.max(pg.getPhysicalReplayStartTimeoutSeconds(), 1);

            int stopExit = executeProcess(buildPgCtlStopCommandArgs(dataDir), stopTimeout, "pg_ctl stop");
            if (stopExit != 0) {
                log.warn("pg_ctl stop 返回非0，将继续尝试启动恢复流程: exitCode={}", stopExit);
            }

            int startExit = executeProcess(buildPgCtlStartCommandArgs(dataDir, startTimeout, startOptions),
                    startTimeout, "pg_ctl start");
            if (startExit != 0) {
                rollbackStartupFromBaseline(artifact, dataDir, baselineDir);
                return false;
            }

            int promoteExit = executeProcess(buildPgCtlPromoteCommandArgs(dataDir, startTimeout),
                    startTimeout, "pg_ctl promote");
            if (promoteExit != 0) {
                rollbackStartupFromBaseline(artifact, dataDir, baselineDir);
                return false;
            }

            replaySuccess = true;
            prunePhysicalBaselines(baselineRootDir);
            return true;
        } finally {
            cleanupRecoverySignal(recoverySignal);
            if (!replaySuccess) {
                log.warn("PostgreSQL内建物理回放未成功，保留基线目录用于排障: {}", baselineDir);
            }
        }
    }

    private void validateWalReplayTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new UnsupportedOperationException(
                    "wal_external回放模式未配置 replay command，请设置 backup.postgres.wal-replay-command");
        }
        if (!template.contains(TEMPLATE_PLACEHOLDER_ARTIFACT)) {
            throw new UnsupportedOperationException(
                    "wal_external回放命令模板必须包含${artifact}占位符");
        }
    }

    private void validatePhysicalReplayConfig() {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        if (pg.getPgCtlPath() == null || pg.getPgCtlPath().isBlank()) {
            throw new UnsupportedOperationException("内建物理回放未配置 pgCtlPath");
        }
        String template = pg.getPhysicalReplayRestoreCommandTemplate();
        if (template == null || template.isBlank()) {
            throw new UnsupportedOperationException("内建物理回放未配置 physicalReplayRestoreCommandTemplate");
        }
        if (!template.contains("%f") || !template.contains("%p")) {
            throw new UnsupportedOperationException("内建物理回放 restore_command 模板必须包含 %f 和 %p");
        }
        if (pg.getPhysicalReplayStopTimeoutSeconds() <= 0
                || pg.getPhysicalReplayStartTimeoutSeconds() <= 0) {
            throw new UnsupportedOperationException("内建物理回放 stop/start 超时必须大于0");
        }
        if (pg.getPhysicalReplayBasebackupTimeoutSeconds() <= 0) {
            throw new UnsupportedOperationException("内建物理回放 basebackup 超时必须大于0");
        }
        if (!pg.isPhysicalReplayBaselineCleanupOnBasebackupFailure()) {
            log.warn("内建物理回放已关闭 pg_basebackup 失败残留自动清理，需人工治理基线目录");
        }
        if (pg.getPhysicalReplayKeepLatestBaselines() <= 0) {
            throw new UnsupportedOperationException("内建物理回放基线保留数量必须大于0");
        }
        if (pg.getPhysicalReplayRollbackStartTimeoutSeconds() <= 0) {
            throw new UnsupportedOperationException("内建物理回放回滚启动超时必须大于0");
        }
        if (!pg.isPhysicalReplayRollbackHealthProbeEnabled()
                && pg.isPhysicalReplayRollbackBusinessProbeEnabled()) {
            throw new UnsupportedOperationException("启用回滚业务一致性探针前必须先启用回滚健康探针");
        }
        if (pg.isPhysicalReplayRollbackHealthProbeEnabled()) {
            if (pg.getPhysicalReplayRollbackHealthProbeMaxAttempts() <= 0) {
                throw new UnsupportedOperationException("内建物理回放回滚健康探针最大尝试次数必须大于0");
            }
            if (pg.getPhysicalReplayRollbackHealthProbeIntervalSeconds() <= 0) {
                throw new UnsupportedOperationException("内建物理回放回滚健康探针重试间隔必须大于0");
            }
            if (pg.getPhysicalReplayRollbackHealthProbeTimeoutSeconds() <= 0) {
                throw new UnsupportedOperationException("内建物理回放回滚健康探针超时必须大于0");
            }
            if (pg.isPhysicalReplayRollbackBusinessProbeEnabled()
                    && (pg.getPhysicalReplayRollbackBusinessProbeSql() == null
                    || pg.getPhysicalReplayRollbackBusinessProbeSql().isBlank())) {
                throw new UnsupportedOperationException("启用回滚业务一致性探针时，businessProbeSql 不能为空");
            }
        }
        if (pg.getPgBasebackupPath() == null || pg.getPgBasebackupPath().isBlank()) {
            throw new UnsupportedOperationException("内建物理回放未配置 pgBasebackupPath");
        }
        normalizeRecoveryTargetAction(pg.getPhysicalReplayRecoveryTargetAction());
    }

    private Path resolveRequiredDirectory(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("内建物理回放配置缺失: " + fieldName);
        }
        Path path = Paths.get(value).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("内建物理回放目录不存在或不可访问: "
                    + fieldName + "=" + path);
        }
        return path;
    }

    private Path resolveOrCreateDirectory(String value, String fieldName) throws Exception {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("内建物理回放配置缺失: " + fieldName);
        }
        Path path = Paths.get(value).toAbsolutePath().normalize();
        Files.createDirectories(path);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("内建物理回放目录不可用: " + fieldName + "=" + path);
        }
        return path;
    }

    private Path createPhysicalBaseline(BackupArtifact artifact, Path baselineRootDir) throws Exception {
        String walStart = artifact.getWalStartPosition() == null ? "UNKNOWN" : artifact.getWalStartPosition();
        String walEnd = artifact.getWalEndPosition() == null ? "UNKNOWN" : artifact.getWalEndPosition();
        String baselineName = LocalDateTime.now().toString()
                .replace(":", "-")
                .replace(".", "-")
                + "_" + walStart.replace('/', '_')
                + "_" + walEnd.replace('/', '_');
        Path baselineDir = baselineRootDir.resolve(baselineName);
        Files.createDirectories(baselineDir);

        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        int timeout = Math.max(pg.getPhysicalReplayBasebackupTimeoutSeconds(), 1);
        int exitCode = executeProcess(buildPgBasebackupCommandArgs(baselineDir),
                timeout,
                "pg_basebackup baseline");
        if (exitCode != 0) {
            handleFailedPhysicalBaseline(baselineDir, exitCode);
        }

        String meta = "wal-start=" + walStart + "\n"
                + "wal-end=" + walEnd + "\n"
                + "created-at=" + LocalDateTime.now() + "\n";
        Files.writeString(baselineDir.resolve(PHYSICAL_BASELINE_METADATA_FILE), meta,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return baselineDir;
    }

    private void handleFailedPhysicalBaseline(Path baselineDir, int exitCode) throws Exception {
        recordPostgresPhysicalBaselineFailureMetric();
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        if (!pg.isPhysicalReplayBaselineCleanupOnBasebackupFailure()) {
            log.error("pg_basebackup 生成物理基线失败，且已关闭残留清理: exitCode={}, baselineDir={}",
                    exitCode, baselineDir);
            throw new IllegalStateException("pg_basebackup 生成物理基线失败，退出码: " + exitCode
                    + "，且已关闭残留自动清理");
        }

        try {
            deleteDirectoryRecursively(baselineDir);
            recordPostgresPhysicalBaselineCleanupMetric(true);
            log.warn("pg_basebackup 生成物理基线失败，已自动清理残留目录: exitCode={}, baselineDir={}",
                    exitCode, baselineDir);
        } catch (Exception cleanupEx) {
            recordPostgresPhysicalBaselineCleanupMetric(false);
            throw new IllegalStateException("pg_basebackup 生成物理基线失败且残留清理失败: exitCode="
                    + exitCode + ", baselineDir=" + baselineDir, cleanupEx);
        }
        throw new IllegalStateException("pg_basebackup 生成物理基线失败，退出码: "
                + exitCode + "，已自动清理残留目录");
    }

    private void recordPostgresPhysicalBaselineFailureMetric() {
        if (backupMetricsRegistrar != null) {
            backupMetricsRegistrar.recordPostgresPhysicalBaselineFailure();
        }
    }

    private void recordPostgresPhysicalBaselineCleanupMetric(boolean success) {
        if (backupMetricsRegistrar != null) {
            backupMetricsRegistrar.recordPostgresPhysicalBaselineCleanup(success);
        }
    }

    List<String> buildPgBasebackupCommandArgs(Path baselineDir) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        List<String> command = new ArrayList<>();
        command.add(pg.getPgBasebackupPath());
        command.add("-h");
        command.add(pg.getHost());
        command.add("-p");
        command.add(String.valueOf(pg.getPort()));
        command.add("-U");
        command.add(pg.getUsername());
        command.add("-D");
        command.add(baselineDir.toString());
        command.add("-Fp");
        command.add("-X");
        command.add("none");
        command.add("-c");
        command.add(pg.isPhysicalReplayBasebackupFastCheckpoint() ? "fast" : "spread");
        command.add("-P");
        return command;
    }

    String renderPhysicalRestoreCommand(String template, BackupArtifact artifact, Path archiveDir) {
        String command = template;
        command = command.replace(TEMPLATE_PLACEHOLDER_ARCHIVE_DIR, shellEscape(archiveDir.toString()));
        command = command.replace(TEMPLATE_PLACEHOLDER_ARTIFACT, shellEscape(artifact.getFile().getAbsolutePath()));
        command = command.replace(TEMPLATE_PLACEHOLDER_WAL_START,
                shellEscape(artifact.getWalStartPosition() == null ? "" : artifact.getWalStartPosition()));
        command = command.replace(TEMPLATE_PLACEHOLDER_WAL_END,
                shellEscape(artifact.getWalEndPosition() == null ? "" : artifact.getWalEndPosition()));
        return command;
    }

    String buildPhysicalStartOptions(String restoreCommand, String walEndPosition) {
        String normalizedWalEnd = normalizeWalLsn(walEndPosition, "WAL结束位点");
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        String action = normalizeRecoveryTargetAction(pg.getPhysicalReplayRecoveryTargetAction());

        StringBuilder options = new StringBuilder();
        options.append("-c restore_command='")
                .append(escapePostgresLiteral(restoreCommand))
                .append("'");
        options.append(" -c recovery_target_lsn='")
                .append(escapePostgresLiteral(normalizedWalEnd))
                .append("'");
        options.append(" -c recovery_target_action='")
                .append(action)
                .append("'");
        if (pg.getPhysicalReplayPort() > 0) {
            options.append(" -c port=").append(pg.getPhysicalReplayPort());
        }
        return options.toString();
    }

    List<String> buildPgCtlStopCommandArgs(Path dataDir) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgCtlPath());
        command.add("-D");
        command.add(dataDir.toString());
        command.add("stop");
        command.add("-m");
        command.add("fast");
        return command;
    }

    List<String> buildPgCtlStartCommandArgs(Path dataDir, int timeoutSeconds, String options) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgCtlPath());
        command.add("-D");
        command.add(dataDir.toString());
        command.add("start");
        command.add("-w");
        command.add("-t");
        command.add(String.valueOf(timeoutSeconds));
        command.add("-o");
        command.add(options);
        return command;
    }

    List<String> buildPgCtlPromoteCommandArgs(Path dataDir, int timeoutSeconds) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgCtlPath());
        command.add("-D");
        command.add(dataDir.toString());
        command.add("promote");
        command.add("-w");
        command.add("-t");
        command.add(String.valueOf(timeoutSeconds));
        return command;
    }

    List<String> buildPgCtlNormalStartCommandArgs(Path dataDir, int timeoutSeconds) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgCtlPath());
        command.add("-D");
        command.add(dataDir.toString());
        command.add("start");
        command.add("-w");
        command.add("-t");
        command.add(String.valueOf(timeoutSeconds));
        return command;
    }

    List<String> buildPgCtlStatusCommandArgs(Path dataDir) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgCtlPath());
        command.add("-D");
        command.add(dataDir.toString());
        command.add("status");
        return command;
    }

    private void rollbackStartupFromBaseline(BackupArtifact artifact,
                                             Path dataDir,
                                             Path baselineDir) throws Exception {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        if (!pg.isPhysicalReplayRollbackOnFailure()) {
            log.warn("PostgreSQL内建物理回放失败，已关闭自动回滚: walStart={}, walEnd={}",
                    artifact.getWalStartPosition(), artifact.getWalEndPosition());
            return;
        }

        int stopTimeout = Math.max(pg.getPhysicalReplayStopTimeoutSeconds(), 1);
        int rollbackStartTimeout = Math.max(pg.getPhysicalReplayRollbackStartTimeoutSeconds(), 1);

        executeProcess(buildPgCtlStopCommandArgs(dataDir), stopTimeout, "pg_ctl stop before rollback");
        Path failedSnapshot = restoreDataDirectoryFromBaseline(dataDir, baselineDir);
        int startExit = executeProcess(buildPgCtlNormalStartCommandArgs(dataDir, rollbackStartTimeout),
                rollbackStartTimeout,
                "pg_ctl start rollback");
        if (startExit == 0) {
            boolean healthy = probeRollbackStartupHealth(dataDir);
            if (healthy) {
                log.warn("PostgreSQL内建物理回放失败后已自动回滚并恢复启动，健康探针通过: snapshot={}", failedSnapshot);
            } else {
                log.error("PostgreSQL内建物理回放失败后已回滚启动，但健康探针未通过: snapshot={}", failedSnapshot);
            }
        } else {
            log.error("PostgreSQL内建物理回放失败且自动回滚启动失败: snapshot={}", failedSnapshot);
        }
    }

    private boolean probeRollbackStartupHealth(Path dataDir) throws Exception {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        if (!pg.isPhysicalReplayRollbackHealthProbeEnabled()) {
            return true;
        }

        int maxAttempts = Math.max(pg.getPhysicalReplayRollbackHealthProbeMaxAttempts(), 1);
        int intervalSeconds = Math.max(pg.getPhysicalReplayRollbackHealthProbeIntervalSeconds(), 1);
        int timeoutSeconds = Math.max(pg.getPhysicalReplayRollbackHealthProbeTimeoutSeconds(), 1);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean processHealthy = executeProcess(buildPgCtlStatusCommandArgs(dataDir),
                    timeoutSeconds,
                    "pg_ctl status rollback probe") == 0;
            boolean sqlHealthy = isRollbackSqlProbeHealthy(timeoutSeconds);
            boolean businessHealthy = isRollbackBusinessProbeHealthy(timeoutSeconds);
            if (processHealthy && sqlHealthy && businessHealthy) {
                return true;
            }

            log.warn("PostgreSQL回滚健康探针未通过: attempt={}/{}, processHealthy={}, sqlHealthy={}, businessHealthy={}",
                    attempt, maxAttempts, processHealthy, sqlHealthy, businessHealthy);
            if (attempt < maxAttempts) {
                try {
                    TimeUnit.SECONDS.sleep(intervalSeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("PostgreSQL回滚健康探针被中断");
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isRollbackSqlProbeHealthy(int timeoutSeconds) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        String database = resolveRollbackProbeDatabase();

        try (Connection connection = openRollbackProbeConnection(timeoutSeconds, database);
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds);
            try (ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                return resultSet.next();
            }
        } catch (Exception e) {
            log.warn("PostgreSQL回滚后SQL探针失败: host={}, port={}, database={}, reason={}",
                    pg.getHost(), pg.getPort(), database, e.getMessage());
            return false;
        }
    }

    private boolean isRollbackBusinessProbeHealthy(int timeoutSeconds) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        if (!pg.isPhysicalReplayRollbackBusinessProbeEnabled()) {
            return true;
        }

        String probeSql = pg.getPhysicalReplayRollbackBusinessProbeSql();
        if (probeSql == null || probeSql.isBlank()) {
            log.warn("PostgreSQL回滚后业务一致性探针未配置SQL");
            return false;
        }
        String expectedValue = pg.getPhysicalReplayRollbackBusinessProbeExpectedValue();
        String database = resolveRollbackProbeDatabase();

        try (Connection connection = openRollbackProbeConnection(timeoutSeconds, database);
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds);
            try (ResultSet resultSet = statement.executeQuery(probeSql)) {
                if (!resultSet.next()) {
                    log.warn("PostgreSQL回滚后业务一致性探针无结果: sql={}", probeSql);
                    return false;
                }
                Object firstColumn = resultSet.getObject(1);
                String actualValue = firstColumn == null ? "" : String.valueOf(firstColumn).trim();
                if (expectedValue == null || expectedValue.isBlank()) {
                    return !actualValue.isBlank();
                }
                String normalizedExpected = expectedValue.trim();
                boolean matched = normalizedExpected.equals(actualValue);
                if (!matched) {
                    log.warn("PostgreSQL回滚后业务一致性探针结果不匹配: expected={}, actual={}",
                            normalizedExpected, actualValue);
                }
                return matched;
            }
        } catch (Exception e) {
            log.warn("PostgreSQL回滚后业务一致性探针失败: sql={}, reason={}", probeSql, e.getMessage());
            return false;
        }
    }

    private Connection openRollbackProbeConnection(int timeoutSeconds, String database) throws Exception {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", pg.getHost(), pg.getPort(), database);
        Properties properties = new Properties();
        properties.setProperty("user", pg.getUsername() == null ? "" : pg.getUsername());
        properties.setProperty("password", pg.getPassword() == null ? "" : pg.getPassword());
        properties.setProperty("connectTimeout", String.valueOf(timeoutSeconds));
        properties.setProperty("socketTimeout", String.valueOf(timeoutSeconds));
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    private String resolveRollbackProbeDatabase() {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        if (pg.getDatabase() == null || pg.getDatabase().isBlank()) {
            return "postgres";
        }
        return pg.getDatabase();
    }

    private Path restoreDataDirectoryFromBaseline(Path dataDir, Path baselineDir) throws Exception {
        if (!Files.isDirectory(baselineDir)) {
            throw new IllegalStateException("回滚基线目录不存在: " + baselineDir);
        }
        Path parent = dataDir.getParent();
        if (parent == null) {
            throw new IllegalStateException("数据目录缺少父目录，无法回滚: " + dataDir);
        }
        Files.createDirectories(parent);
        Path failedSnapshot = parent.resolve(PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX + LocalDateTime.now()
                .toString()
                .replace(":", "-")
                .replace(".", "-"));

        if (Files.exists(dataDir)) {
            Files.move(dataDir, failedSnapshot, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(dataDir);
        copyDirectoryRecursively(baselineDir, dataDir);
        return failedSnapshot;
    }

    private void copyDirectoryRecursively(Path source, Path target) throws Exception {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path destination = target.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("复制目录失败: " + path + " -> " + target, e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    private void prunePhysicalBaselines(Path baselineRootDir) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        int keepLatest = Math.max(pg.getPhysicalReplayKeepLatestBaselines(), 1);
        try (Stream<Path> stream = Files.list(baselineRootDir)) {
            List<Path> baselines = stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
            if (baselines.size() <= keepLatest) {
                return;
            }
            for (int i = keepLatest; i < baselines.size(); i++) {
                deleteDirectoryRecursively(baselines.get(i));
            }
        } catch (Exception e) {
            log.warn("清理历史物理基线失败: {}", baselineRootDir, e);
        }
    }

    private void deleteDirectoryRecursively(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    private int executeProcess(List<String> command, int timeoutSeconds, String stage) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        applyPostgresPassword(processBuilder, backupProperties.getPostgres().getPassword());
        processBuilder.redirectErrorStream(true);

        logCommand(command);
        Process process = processBuilder.start();
        consumeProcessOutput(process.getInputStream());

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            log.error("{} 执行超时", stage);
            return -1;
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("{} 执行失败, 退出码: {}", stage, exitCode);
        }
        return exitCode;
    }

    private void cleanupRecoverySignal(Path recoverySignal) {
        try {
            Files.deleteIfExists(recoverySignal);
        } catch (IOException e) {
            log.warn("清理 recovery.signal 失败: {}", recoverySignal, e);
        }
    }

    String normalizeRecoveryTargetAction(String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            return "promote";
        }
        String action = rawAction.trim().toLowerCase(Locale.ROOT);
        if ("pause".equals(action) || "promote".equals(action) || "shutdown".equals(action)) {
            return action;
        }
        throw new UnsupportedOperationException("内建物理回放 recovery_target_action 不支持: " + rawAction);
    }

    private String escapePostgresLiteral(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    private void resolveAndValidateWalRangeForReplay(BackupArtifact artifact, File file) throws Exception {
        String start = normalizeWalLsnOrNull(artifact.getWalStartPosition(), "WAL起始位点");
        String end = normalizeWalLsnOrNull(artifact.getWalEndPosition(), "WAL结束位点");

        if (start == null) {
            start = normalizeWalLsnOrNull(readHeaderLsn(file, WAL_HEADER_START_PREFIX), "WAL起始位点");
        }
        if (end == null) {
            end = normalizeWalLsnOrNull(readHeaderLsn(file, WAL_HEADER_END_PREFIX), "WAL结束位点");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("WAL增量回放缺少wal-start/wal-end位点信息");
        }

        validateWalRange(start, end);
        artifact.setWalStartPosition(start);
        artifact.setWalEndPosition(end);
    }

    private String readHeaderLsn(File file, String headerPrefix) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            int scanned = 0;
            while ((line = reader.readLine()) != null && scanned < REPLAYABLE_HEADER_SCAN_LINES) {
                String normalizedLine = line.trim();
                if (normalizedLine.startsWith(headerPrefix)) {
                    String lsn = normalizedLine.substring(headerPrefix.length()).trim();
                    return lsn.isBlank() ? null : lsn;
                }
                scanned++;
            }
        }
        return null;
    }

    String renderWalReplayCommand(String template, BackupArtifact artifact, String targetDatabase) {
        String command = template;
        command = command.replace("${artifact}", shellEscape(artifact.getFile().getAbsolutePath()));
        command = command.replace("${targetDatabase}", shellEscape(targetDatabase == null ? "" : targetDatabase));
        command = command.replace("${walStart}", shellEscape(artifact.getWalStartPosition() == null ? "" : artifact.getWalStartPosition()));
        command = command.replace("${walEnd}", shellEscape(artifact.getWalEndPosition() == null ? "" : artifact.getWalEndPosition()));
        command = command.replace("${host}", shellEscape(backupProperties.getPostgres().getHost()));
        command = command.replace("${port}", shellEscape(String.valueOf(backupProperties.getPostgres().getPort())));
        command = command.replace("${username}", shellEscape(backupProperties.getPostgres().getUsername()));
        // ${password} is intentionally NOT substituted here; the password is injected via
        // the PGPASSWORD environment variable in applyPostgresPassword() to avoid leaking
        // credentials into the command line or process arguments.
        return command;
    }

    private String shellEscape(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private boolean containsLine(File file, String expectedLine) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            int scanned = 0;
            while ((line = reader.readLine()) != null && scanned < REPLAYABLE_HEADER_SCAN_LINES) {
                if (expectedLine.equals(line.trim())) {
                    return true;
                }
                scanned++;
            }
        }
        return false;
    }

    private List<String> queryUserTables(BackupRequest.DatabaseConfig dbConfig) throws Exception {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                dbConfig.getHost(), dbConfig.getPort(), dbConfig.getDatabase());
        String sql = "SELECT quote_ident(schemaname) || '.' || quote_ident(tablename) AS full_name "
                + "FROM pg_tables "
                + "WHERE schemaname NOT IN ('pg_catalog', 'information_schema') "
                + "ORDER BY schemaname, tablename";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbConfig.getUsername(), dbConfig.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<String> tables = new ArrayList<>();
            while (resultSet.next()) {
                String fullName = resultSet.getString("full_name");
                if (fullName != null && !fullName.isBlank()) {
                    tables.add(fullName);
                }
            }
            return tables;
        }
    }

    private void writeIncrementalPreamble(File backupFile,
                                          String startPosition,
                                          String endPosition,
                                          List<String> tableNames) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(REPLAYABLE_INCREMENTAL_MARKER).append('\n');
        sb.append("-- PostgreSQL replayable incremental snapshot").append('\n');
        sb.append(WAL_HEADER_START_PREFIX).append(' ').append(startPosition).append('\n');
        sb.append(WAL_HEADER_END_PREFIX).append(' ').append(endPosition).append('\n');
        sb.append("\\set ON_ERROR_STOP on").append('\n');
        if (tableNames == null || tableNames.isEmpty()) {
            sb.append("-- no user tables found, keep schema-only state").append('\n');
        } else {
            sb.append("TRUNCATE TABLE ")
                    .append(String.join(", ", tableNames))
                    .append(" RESTART IDENTITY CASCADE;")
                    .append('\n');
        }
        sb.append('\n');
        Files.writeString(backupFile.toPath(), sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    protected BackupArtifact doBackup(BackupRequest request, BackupHistory history) throws Exception {
        String backupType = request.getBackupType();
        if (backupType == null) {
            throw new IllegalArgumentException("备份类型不能为空");
        }
        if ("full".equalsIgnoreCase(backupType)) {
            return executeFull(request);
        }
        if ("incremental".equalsIgnoreCase(backupType)) {
            IncrementalBackupRequest incrementalRequest;
            if (request instanceof IncrementalBackupRequest existingRequest) {
                incrementalRequest = existingRequest;
            } else {
                incrementalRequest = new IncrementalBackupRequest();
                incrementalRequest.setTaskId(request.getTaskId());
                incrementalRequest.setDatasourceType(request.getDatasourceType());
                incrementalRequest.setBackupType(request.getBackupType());
                incrementalRequest.setStrategyJson(request.getStrategyJson());
                incrementalRequest.setStoragePolicyJson(request.getStoragePolicyJson());
                incrementalRequest.setDatabaseConfig(request.getDatabaseConfig());
                incrementalRequest.setStartTime(request.getStartTime());
                incrementalRequest.setStartPosition(request.getStartPosition());
                incrementalRequest.setParameters(request.getParameters());
            }
            return executeIncremental(incrementalRequest);
        }
        throw new IllegalArgumentException("不支持的备份类型: " + backupType);
    }

    private BackupArtifact buildIncrementalArtifact(IncrementalBackupRequest request,
                                                    File backupFile,
                                                    String walStartPosition,
                                                    String walEndPosition) {
        LocalDateTime startTime = request.getStartTime() == null ? LocalDateTime.now() : request.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();
        return BackupArtifact.builder()
                .file(backupFile)
                .backupType("incremental")
                .fileSize(backupFile.length())
                .startTime(startTime)
                .endTime(endTime)
                .walStartPosition(walStartPosition)
                .walEndPosition(walEndPosition)
                .durationSeconds(ChronoUnit.SECONDS.between(startTime, endTime))
                .build();
    }

    List<String> buildPgWalDumpCommandArgs(String startPosition, String endPosition) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgWalDumpPath());
        command.add("--path=" + backupProperties.getIncremental().getPostgres().getWalDir());
        command.add("--start=" + startPosition);
        command.add("--end=" + endPosition);
        return command;
    }

    String resolveStartWalPosition(IncrementalBackupRequest request) {
        String startPosition = request.getStartPosition();
        if (startPosition == null || startPosition.isBlank()) {
            throw new IllegalArgumentException("PostgreSQL增量备份起始WAL位点不能为空");
        }

        return normalizeWalLsn(startPosition, "PostgreSQL增量备份起始WAL位点");
    }

    private void validateWalRange(String startPosition, String endPosition) {
        long[] start = parseWalLsn(startPosition);
        long[] end = parseWalLsn(endPosition);
        if (Long.compareUnsigned(start[0], end[0]) > 0
                || (start[0] == end[0] && Long.compareUnsigned(start[1], end[1]) > 0)) {
            throw new IllegalArgumentException("PostgreSQL增量备份WAL区间非法，起始位点晚于结束位点");
        }
    }

    private String normalizeWalLsnOrNull(String rawLsn, String fieldName) {
        if (rawLsn == null || rawLsn.isBlank()) {
            return null;
        }
        return normalizeWalLsn(rawLsn, fieldName);
    }

    private String normalizeWalLsn(String rawLsn, String fieldName) {
        if (rawLsn == null || rawLsn.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        String normalized = rawLsn.trim().toUpperCase(Locale.ROOT);
        if (!WAL_LSN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + "格式非法: " + rawLsn);
        }
        return normalized;
    }

    private long[] parseWalLsn(String lsn) {
        String[] parts = lsn.split("/", 2);
        return new long[]{
                Long.parseUnsignedLong(parts[0], 16),
                Long.parseUnsignedLong(parts[1], 16)
        };
    }

    private void cleanupTempFile(File backupFile, String outputFile) {
        if (backupFile == null || !backupFile.exists()) {
            return;
        }
        try {
            Files.delete(backupFile.toPath());
        } catch (IOException cleanupEx) {
            log.warn("清理临时备份文件失败: {}", outputFile, cleanupEx);
        }
    }

    private BackupRequest.DatabaseConfig resolveDatabaseConfig(BackupRequest request) {
        BackupRequest.DatabaseConfig input = request == null ? null : request.getDatabaseConfig();
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();

        BackupRequest.DatabaseConfig resolved = new BackupRequest.DatabaseConfig();
        resolved.setHost(valueOrDefault(input == null ? null : input.getHost(), pg.getHost()));
        resolved.setPort(input != null && input.getPort() != null ? input.getPort() : pg.getPort());
        resolved.setUsername(valueOrDefault(input == null ? null : input.getUsername(), pg.getUsername()));
        resolved.setPassword(valueOrDefault(input == null ? null : input.getPassword(), pg.getPassword()));
        resolved.setDatabase(valueOrDefault(input == null ? null : input.getDatabase(), pg.getDatabase()));
        resolved.setCharset(valueOrDefault(input == null ? null : input.getCharset(), "UTF8"));

        if (resolved.getDatabase() == null || resolved.getDatabase().isBlank()) {
            throw new IllegalArgumentException("数据库名称不能为空");
        }
        return resolved;
    }

    private String valueOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private List<String> buildPgDumpCommandArgs(BackupRequest.DatabaseConfig dbConfig, String outputFile) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgDumpPath());
        command.add("-h");
        command.add(dbConfig.getHost());
        command.add("-p");
        command.add(String.valueOf(dbConfig.getPort()));
        command.add("-U");
        command.add(dbConfig.getUsername());
        command.add("-d");
        command.add(dbConfig.getDatabase());
        command.add("--format=plain");
        command.add("--no-owner");
        command.add("--no-privileges");
        command.add("-f");
        command.add(outputFile);
        return command;
    }

    List<String> buildPgDumpDataOnlyCommandArgs(BackupRequest.DatabaseConfig dbConfig) {
        List<String> command = new ArrayList<>();
        command.add(backupProperties.getPostgres().getPgDumpPath());
        command.add("-h");
        command.add(dbConfig.getHost());
        command.add("-p");
        command.add(String.valueOf(dbConfig.getPort()));
        command.add("-U");
        command.add(dbConfig.getUsername());
        command.add("-d");
        command.add(dbConfig.getDatabase());
        command.add("--format=plain");
        command.add("--data-only");
        command.add("--no-owner");
        command.add("--no-privileges");
        return command;
    }

    private List<String> buildPsqlCommandArgs(String targetDatabase, File inputFile) {
        BackupProperties.PostgresConfig pg = backupProperties.getPostgres();
        String database = (targetDatabase == null || targetDatabase.isBlank()) ? pg.getDatabase() : targetDatabase;
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("目标数据库不能为空");
        }

        List<String> command = new ArrayList<>();
        command.add(pg.getPsqlPath());
        command.add("-h");
        command.add(pg.getHost());
        command.add("-p");
        command.add(String.valueOf(pg.getPort()));
        command.add("-U");
        command.add(pg.getUsername());
        command.add("-d");
        command.add(database);
        command.add("-f");
        command.add(inputFile.getAbsolutePath());
        return command;
    }

    private void applyPostgresPassword(ProcessBuilder processBuilder, String password) {
        if (password != null && !password.isBlank()) {
            processBuilder.environment().put("PGPASSWORD", password);
        }
    }

    private void consumeProcessOutput(InputStream inputStream) {
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("PostgreSQL进程输出: {}", line);
                }
            } catch (IOException e) {
                log.debug("读取PostgreSQL进程输出失败", e);
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
    }

    private String getOutputFilePath(BackupRequest request, String type) {
        String basePath = backupProperties.getStorage().getLocal().getBasePath();
        if (basePath == null || basePath.isBlank()) {
            basePath = backupProperties.getPostgres().getBackupPath();
        }

        String timestamp = LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
        Long taskId = request == null ? null : request.getTaskId();
        String taskSegment = taskId == null ? "0" : String.valueOf(taskId);
        String extension;
        if ("incremental".equalsIgnoreCase(type)) {
            extension = isWalDumpReplayMode(resolveReplayMode()) ? "wal.log" : "sql";
        } else {
            extension = "sql";
        }
        String filename = String.format("%s_%s_%s.%s", taskSegment, type, timestamp, extension);

        Path outputPath = Paths.get(basePath, "postgresql", filename);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            log.error("创建目录失败: {}", outputPath.getParent(), e);
        }
        return outputPath.toString();
    }
}
