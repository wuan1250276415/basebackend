package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.infrastructure.executor.BackupArtifact;
import com.basebackend.backup.infrastructure.executor.BackupRequest;
import com.basebackend.backup.infrastructure.executor.IncrementalBackupRequest;
import com.basebackend.backup.infrastructure.monitoring.BackupMetricsRegistrar;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostgresBackupExecutor 增量配置测试")
class PostgresBackupExecutorTest {

    @Mock
    private LockManager lockManager;
    @Mock
    private RetryTemplate retryTemplate;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private ChecksumService checksumService;
    @Mock
    private BackupHistoryMapper backupHistoryMapper;
    @Mock
    private PostgresWalParser walParser;

    private BackupProperties backupProperties;
    private PostgresBackupExecutor executor;

    @BeforeEach
    void setUp() {
        backupProperties = new BackupProperties();
        backupProperties.getPostgres().setPgWalDumpPath("custom_pg_waldump");
        backupProperties.getIncremental().getPostgres().setWalDir("/data/pg_wal");

        executor = new PostgresBackupExecutor(
                lockManager,
                retryTemplate,
                storageProvider,
                checksumService,
                backupHistoryMapper,
                null,
                backupProperties,
                walParser
        );
    }

    @Test
    @DisplayName("应基于配置构建pg_waldump命令参数")
    void shouldBuildPgWalDumpCommandArgsByProperties() {
        List<String> command = executor.buildPgWalDumpCommandArgs("0/16B5A20", "0/16B6F90");

        assertThat(command).containsExactly(
                "custom_pg_waldump",
                "--path=/data/pg_wal",
                "--start=0/16B5A20",
                "--end=0/16B6F90"
        );
    }

    @Test
    @DisplayName("应基于配置构建pg_basebackup命令参数")
    void shouldBuildPgBasebackupCommandArgsByProperties() {
        backupProperties.getPostgres().setPgBasebackupPath("custom_pg_basebackup");
        backupProperties.getPostgres().setHost("pg-host");
        backupProperties.getPostgres().setPort(5433);
        backupProperties.getPostgres().setUsername("pg-user");
        backupProperties.getPostgres().setPhysicalReplayBasebackupFastCheckpoint(false);

        List<String> command = executor.buildPgBasebackupCommandArgs(Path.of("/tmp/pg-baseline"));

        assertThat(command).containsExactly(
                "custom_pg_basebackup",
                "-h", "pg-host",
                "-p", "5433",
                "-U", "pg-user",
                "-D", "/tmp/pg-baseline",
                "-Fp",
                "-X", "none",
                "-c", "spread",
                "-P"
        );
    }

    @Test
    @DisplayName("pg_basebackup fast checkpoint 开启时应使用fast模式")
    void shouldBuildPgBasebackupCommandArgsWithFastCheckpoint() {
        backupProperties.getPostgres().setPgBasebackupPath("custom_pg_basebackup");
        backupProperties.getPostgres().setHost("pg-host");
        backupProperties.getPostgres().setPort(5432);
        backupProperties.getPostgres().setUsername("pg-user");
        backupProperties.getPostgres().setPhysicalReplayBasebackupFastCheckpoint(true);

        List<String> command = executor.buildPgBasebackupCommandArgs(Path.of("/tmp/pg-baseline-fast"));

        assertThat(command).contains("-c", "fast");
        assertThat(command).doesNotContain("spread");
    }

    @Test
    @DisplayName("应构建可回放增量快照的pg_dump data-only命令参数")
    void shouldBuildPgDumpDataOnlyCommandArgs() {
        BackupRequest.DatabaseConfig dbConfig = new BackupRequest.DatabaseConfig();
        dbConfig.setHost("pg-host");
        dbConfig.setPort(5432);
        dbConfig.setUsername("pg-user");
        dbConfig.setDatabase("pg_db");

        List<String> command = executor.buildPgDumpDataOnlyCommandArgs(dbConfig);

        assertThat(command.get(0)).isEqualTo("pg_dump");
        assertThat(command).contains(
                "-h", "pg-host",
                "-p", "5432",
                "-U", "pg-user",
                "-d", "pg_db",
                "--data-only"
        );
    }

    @Test
    @DisplayName("应标准化并验证WAL起始位点")
    void shouldNormalizeAndValidateWalStartPosition() {
        IncrementalBackupRequest request = new IncrementalBackupRequest();
        request.setStartPosition("0/16b6f90");

        String normalized = executor.resolveStartWalPosition(request);

        assertThat(normalized).isEqualTo("0/16B6F90");
    }

    @Test
    @DisplayName("WAL起始位点为空时应抛异常")
    void shouldThrowWhenWalStartPositionMissing() {
        IncrementalBackupRequest request = new IncrementalBackupRequest();
        request.setStartPosition(" ");

        assertThatThrownBy(() -> executor.resolveStartWalPosition(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("起始WAL位点不能为空");
    }

    @Test
    @DisplayName("WAL起始位点格式非法时应抛异常")
    void shouldThrowWhenWalStartPositionMalformed() {
        IncrementalBackupRequest request = new IncrementalBackupRequest();
        request.setStartPosition("16B6F90");

        assertThatThrownBy(() -> executor.resolveStartWalPosition(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("格式非法");
    }

    @Test
    @DisplayName("未启用回放能力时不应声明增量备份能力")
    void shouldHideIncrementalBackupFeatureWhenReplayDisabled() {
        assertThat(executor.getSupportedFeatures()).doesNotContain("incremental_backup");
    }

    @Test
    @DisplayName("启用回放能力后应声明增量备份能力")
    void shouldExposeIncrementalBackupFeatureWhenReplayEnabled() {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);

        assertThat(executor.getSupportedFeatures()).contains("incremental_backup");
    }

    @Test
    @DisplayName("未启用回放能力时执行增量备份应被拒绝")
    void shouldRejectIncrementalExecutionWhenReplayDisabled() {
        IncrementalBackupRequest request = new IncrementalBackupRequest();

        assertThatThrownBy(() -> executor.executeIncremental(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("能力未启用");
    }

    @Test
    @DisplayName("应识别快照型增量文件格式标记")
    void shouldAcceptReplayableIncrementalFormat() throws Exception {
        Path incrementalFile = Files.createTempFile("pg-inc-replayable", ".sql");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.REPLAYABLE_INCREMENTAL_MARKER + "\n-- replayable\n");
            assertThat(executor.detectIncrementalFormat(incrementalFile.toFile()))
                    .isEqualTo(PostgresBackupExecutor.REPLAYABLE_INCREMENTAL_MARKER);
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("应兼容旧版快照增量文件头")
    void shouldAcceptLegacyReplayableIncrementalHeader() throws Exception {
        Path incrementalFile = Files.createTempFile("pg-inc-legacy-replayable", ".sql");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.LEGACY_REPLAYABLE_SNAPSHOT_HEADER + "\n-- replayable\n");
            assertThat(executor.detectIncrementalFormat(incrementalFile.toFile()))
                    .isEqualTo(PostgresBackupExecutor.REPLAYABLE_INCREMENTAL_MARKER);
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("启用回放能力后应拒绝未知格式增量文件")
    void shouldRejectNonReplayableIncrementalArtifactOnRestore() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);

        Path incrementalFile = Files.createTempFile("pg-inc-legacy", ".log");
        try {
            Files.writeString(incrementalFile, "-- legacy wal dump without replay marker\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("未知增量备份格式");
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external模式应暴露WAL回放能力标识")
    void shouldExposeWalExternalFeatureWhenReplayModeIsWalExternal() {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");

        assertThat(executor.getSupportedFeatures()).contains("wal_external_replay");
    }

    @Test
    @DisplayName("wal_physical_builtin模式应暴露内建物理回放能力标识")
    void shouldExposeWalPhysicalBuiltinFeatureWhenReplayModeIsBuiltin() {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");

        assertThat(executor.getSupportedFeatures()).contains("wal_physical_builtin_replay");
    }

    @Test
    @DisplayName("应识别wal_physical_builtin回放模式")
    void shouldResolveWalPhysicalBuiltinReplayMode() {
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");

        assertThat(executor.resolveReplayMode())
                .isEqualTo(PostgresBackupExecutor.REPLAY_MODE_WAL_PHYSICAL_BUILTIN);
    }

    @Test
    @DisplayName("应识别WAL导出型增量文件格式标记")
    void shouldDetectWalDumpIncrementalFormat() throws Exception {
        Path incrementalFile = Files.createTempFile("pg-inc-wal", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n");
            assertThat(executor.detectIncrementalFormat(incrementalFile.toFile()))
                    .isEqualTo(PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER);
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external模式缺少回放命令时应拒绝恢复")
    void shouldRejectWalDumpRestoreWhenReplayCommandMissing() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-restore", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .walStartPosition("0/16")
                    .walEndPosition("0/17")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("wal-replay-command");
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("应渲染WAL回放命令模板占位符")
    void shouldRenderWalReplayCommandTemplate() {
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setHost("pg-host");
        backupProperties.getPostgres().setPort(5432);
        backupProperties.getPostgres().setUsername("pg-user");
        backupProperties.getPostgres().setPassword("secret");

        BackupArtifact artifact = BackupArtifact.builder()
                .file(Path.of("/tmp/inc.wal.log").toFile())
                .backupType("incremental")
                .walStartPosition("0/16")
                .walEndPosition("0/17")
                .build();

        String rendered = executor.renderWalReplayCommand(
                "replay --file ${artifact} --db ${targetDatabase} --start ${walStart} --end ${walEnd} --host ${host}",
                artifact,
                "target_db");

        assertThat(rendered).contains("/tmp/inc.wal.log");
        assertThat(rendered).contains("target_db");
        assertThat(rendered).contains("0/16");
        assertThat(rendered).contains("0/17");
        assertThat(rendered).contains("pg-host");
    }

    @Test
    @DisplayName("wal_external 模板缺少artifact占位符时应拒绝恢复")
    void shouldRejectWalReplayTemplateWithoutArtifactPlaceholder() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("replay --db ${targetDatabase}");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-template", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .walStartPosition("0/16")
                    .walEndPosition("0/17")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("${artifact}");
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external 恢复缺少WAL区间信息时应拒绝执行")
    void shouldRejectWalDumpRestoreWhenWalRangeMissing() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("cat ${artifact} >/dev/null");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-missing-range", ".log");
        try {
            Files.writeString(incrementalFile, PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("wal-start/wal-end");
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external 恢复WAL区间逆序时应拒绝执行")
    void shouldRejectWalDumpRestoreWhenWalRangeReversed() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("cat ${artifact} >/dev/null");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-reversed-range", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/20\n-- wal-end: 0/10\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .walStartPosition("0/20")
                    .walEndPosition("0/10")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("起始位点晚于结束位点");
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external 恢复命令执行成功时应返回true")
    void shouldReturnTrueWhenWalReplayCommandSucceeds() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("cat ${artifact} >/dev/null");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-success", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isTrue();
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external 恢复命令执行失败时应返回false")
    void shouldReturnFalseWhenWalReplayCommandFails() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("cat ${artifact} >/dev/null && exit 9");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-fail", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isFalse();
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_external 无变更增量应跳过回放命令")
    void shouldSkipWalReplayWhenNoChanges() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_external");
        backupProperties.getPostgres().setWalReplayCommand("");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-no-change", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n"
                            + PostgresBackupExecutor.WAL_DUMP_NO_CHANGES_HEADER + "\n"
                            + "-- wal-position: 0/16\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isTrue();
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("logical_snapshot模式不应回放wal-dump增量")
    void shouldRejectWalDumpRestoreInLogicalSnapshotMode() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("logical_snapshot");

        Path incrementalFile = Files.createTempFile("pg-inc-wal-logical-mode", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("不支持WAL导出型恢复");
        } finally {
            Files.deleteIfExists(incrementalFile);
        }
    }

    @Test
    @DisplayName("wal_physical_builtin模式模板缺少%f/%p时应拒绝恢复")
    void shouldRejectBuiltinPhysicalReplayWhenRestoreCommandTemplateInvalid() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}");

        Path dataDir = Files.createTempDirectory("pg-physical-data");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());

        Path incrementalFile = Files.createTempFile("pg-inc-wal-physical-invalid-template", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("%f")
                    .hasMessageContaining("%p");
        } finally {
            Files.deleteIfExists(incrementalFile);
            Files.deleteIfExists(dataDir);
            Files.deleteIfExists(archiveDir);
        }
    }

    @Test
    @DisplayName("wal_physical_builtin模式应执行内建物理回放编排")
    void shouldRestoreByBuiltinPhysicalReplay() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPgBasebackupPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayRecoveryTargetAction("promote");
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(3);

        Path dataDir = Files.createTempDirectory("pg-physical-data-success");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-success");
        Path baselineDir = Files.createTempDirectory("pg-physical-baseline-success");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineDir.toString());

        Path incrementalFile = Files.createTempFile("pg-inc-wal-physical-success", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/16\n-- wal-end: 0/17\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isTrue();
            assertThat(Files.exists(dataDir.resolve(PostgresBackupExecutor.PHYSICAL_RECOVERY_SIGNAL_FILE))).isFalse();
        } finally {
            Files.deleteIfExists(incrementalFile);
            deleteRecursively(dataDir);
            deleteRecursively(archiveDir);
            deleteRecursively(baselineDir);
        }
    }

    @Test
    @DisplayName("wal_physical_builtin恢复失败时应自动回滚并尝试正常启动")
    void shouldRollbackStartupWhenBuiltinPhysicalReplayFails() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackOnFailure(true);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeEnabled(false);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(3);

        Path dataDir = Files.createTempDirectory("pg-physical-data-rollback");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-rollback");
        Path baselineRootDir = Files.createTempDirectory("pg-physical-baseline-rollback");
        Files.writeString(dataDir.resolve("current.data"), "before-replay");

        Path pgCtlScript = Files.createTempFile("pg-ctl-mock", ".sh");
        Files.writeString(pgCtlScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "HAS_START=0\n"
                        + "HAS_OPTIONS=0\n"
                        + "for ARG in \"$@\"; do\n"
                        + "  [ \"$ARG\" = \"start\" ] && HAS_START=1\n"
                        + "  [ \"$ARG\" = \"-o\" ] && HAS_OPTIONS=1\n"
                        + "done\n"
                        + "if [ \"$HAS_START\" = \"1\" ] && [ \"$HAS_OPTIONS\" = \"1\" ]; then\n"
                        + "  exit 1\n"
                        + "fi\n"
                        + "exit 0\n");
        pgCtlScript.toFile().setExecutable(true);

        Path pgBasebackupScript = Files.createTempFile("pg-basebackup-mock", ".sh");
        Files.writeString(pgBasebackupScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "OUT_DIR=\"\"\n"
                        + "while [ \"$#\" -gt 0 ]; do\n"
                        + "  if [ \"$1\" = \"-D\" ]; then\n"
                        + "    shift\n"
                        + "    OUT_DIR=\"$1\"\n"
                        + "  fi\n"
                        + "  shift\n"
                        + "done\n"
                        + "mkdir -p \"$OUT_DIR\"\n"
                        + "echo baseline > \"$OUT_DIR/base.marker\"\n"
                        + "exit 0\n");
        pgBasebackupScript.toFile().setExecutable(true);

        backupProperties.getPostgres().setPgCtlPath(pgCtlScript.toString());
        backupProperties.getPostgres().setPgBasebackupPath(pgBasebackupScript.toString());
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineRootDir.toString());

        Path incrementalFile = Files.createTempFile("pg-inc-wal-physical-rollback", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/21\n-- wal-end: 0/22\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isFalse();
            assertThat(Files.exists(dataDir.resolve("base.marker"))).isTrue();
            assertThat(Files.exists(dataDir.resolve(PostgresBackupExecutor.PHYSICAL_RECOVERY_SIGNAL_FILE))).isFalse();

            try (var children = Files.list(dataDir.getParent())) {
                assertThat(children.anyMatch(path -> path.getFileName().toString()
                        .startsWith(PostgresBackupExecutor.PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX))).isTrue();
            }
        } finally {
            Files.deleteIfExists(incrementalFile);
            Files.deleteIfExists(pgCtlScript);
            Files.deleteIfExists(pgBasebackupScript);
            Path parent = dataDir.getParent();
            if (parent != null && Files.exists(parent)) {
                try (var children = Files.list(parent)) {
                    for (Path child : children.toList()) {
                        String name = child.getFileName() == null ? "" : child.getFileName().toString();
                        if (name.startsWith("pg-physical-data-rollback")
                                || name.startsWith("pg-physical-archive-rollback")
                                || name.startsWith("pg-physical-baseline-rollback")
                                || name.startsWith(PostgresBackupExecutor.PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX)) {
                            deleteRecursively(child);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("关闭自动回滚时失败后不应回置数据目录")
    void shouldNotRollbackWhenRollbackOnFailureDisabled() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackOnFailure(false);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(3);

        Path dataDir = Files.createTempDirectory("pg-physical-data-no-rollback");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-no-rollback");
        Path baselineRootDir = Files.createTempDirectory("pg-physical-baseline-no-rollback");
        Files.writeString(dataDir.resolve("current.data"), "before-replay");

        Path pgCtlScript = Files.createTempFile("pg-ctl-mock-no-rollback", ".sh");
        Files.writeString(pgCtlScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "HAS_START=0\n"
                        + "HAS_OPTIONS=0\n"
                        + "for ARG in \"$@\"; do\n"
                        + "  [ \"$ARG\" = \"start\" ] && HAS_START=1\n"
                        + "  [ \"$ARG\" = \"-o\" ] && HAS_OPTIONS=1\n"
                        + "done\n"
                        + "if [ \"$HAS_START\" = \"1\" ] && [ \"$HAS_OPTIONS\" = \"1\" ]; then\n"
                        + "  exit 1\n"
                        + "fi\n"
                        + "exit 0\n");
        pgCtlScript.toFile().setExecutable(true);

        Path pgBasebackupScript = Files.createTempFile("pg-basebackup-mock-no-rollback", ".sh");
        Files.writeString(pgBasebackupScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "OUT_DIR=\"\"\n"
                        + "while [ \"$#\" -gt 0 ]; do\n"
                        + "  if [ \"$1\" = \"-D\" ]; then\n"
                        + "    shift\n"
                        + "    OUT_DIR=\"$1\"\n"
                        + "  fi\n"
                        + "  shift\n"
                        + "done\n"
                        + "mkdir -p \"$OUT_DIR\"\n"
                        + "echo baseline > \"$OUT_DIR/base.marker\"\n"
                        + "exit 0\n");
        pgBasebackupScript.toFile().setExecutable(true);

        backupProperties.getPostgres().setPgCtlPath(pgCtlScript.toString());
        backupProperties.getPostgres().setPgBasebackupPath(pgBasebackupScript.toString());
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineRootDir.toString());

        Path incrementalFile = Files.createTempFile("pg-inc-wal-physical-no-rollback", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/31\n-- wal-end: 0/32\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isFalse();
            assertThat(Files.exists(dataDir.resolve("current.data"))).isTrue();
            assertThat(Files.exists(dataDir.resolve("base.marker"))).isFalse();
            assertThat(Files.exists(dataDir.resolve(PostgresBackupExecutor.PHYSICAL_RECOVERY_SIGNAL_FILE))).isFalse();

            try (var children = Files.list(dataDir.getParent())) {
                assertThat(children.anyMatch(path -> path.getFileName().toString()
                        .startsWith(PostgresBackupExecutor.PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX))).isFalse();
            }
        } finally {
            Files.deleteIfExists(incrementalFile);
            Files.deleteIfExists(pgCtlScript);
            Files.deleteIfExists(pgBasebackupScript);
            Path parent = dataDir.getParent();
            if (parent != null && Files.exists(parent)) {
                try (var children = Files.list(parent)) {
                    for (Path child : children.toList()) {
                        String name = child.getFileName() == null ? "" : child.getFileName().toString();
                        if (name.startsWith("pg-physical-data-no-rollback")
                                || name.startsWith("pg-physical-archive-no-rollback")
                                || name.startsWith("pg-physical-baseline-no-rollback")
                                || name.startsWith(PostgresBackupExecutor.PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX)) {
                            deleteRecursively(child);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("回放成功后应按保留数清理历史物理基线")
    void shouldPrunePhysicalBaselinesAfterReplaySuccess() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPgBasebackupPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayRecoveryTargetAction("promote");
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(2);

        Path dataDir = Files.createTempDirectory("pg-physical-data-prune");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-prune");
        Path baselineRootDir = Files.createTempDirectory("pg-physical-baseline-prune");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineRootDir.toString());

        Files.createDirectories(baselineRootDir.resolve("2000-01-01T00-00-00-000_old"));
        Files.createDirectories(baselineRootDir.resolve("2000-01-02T00-00-00-000_old"));
        Files.createDirectories(baselineRootDir.resolve("2000-01-03T00-00-00-000_old"));

        Path incrementalFile = Files.createTempFile("pg-inc-wal-physical-prune", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/41\n-- wal-end: 0/42\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isTrue();
            try (var stream = Files.list(baselineRootDir)) {
                List<Path> baselines = stream.filter(Files::isDirectory).toList();
                assertThat(baselines).hasSize(2);
                assertThat(baselines)
                        .anySatisfy(path -> assertThat(path.getFileName().toString()).contains("0_41_0_42"))
                        .anySatisfy(path -> assertThat(path.getFileName().toString())
                                .isEqualTo("2000-01-03T00-00-00-000_old"));
            }
        } finally {
            Files.deleteIfExists(incrementalFile);
            deleteRecursively(dataDir);
            deleteRecursively(archiveDir);
            deleteRecursively(baselineRootDir);
        }
    }

    @Test
    @DisplayName("失败回滚启动后应执行进程与SQL健康探针")
    void shouldProbeProcessAndSqlAfterRollbackStart() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackOnFailure(true);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(3);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeEnabled(true);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeMaxAttempts(1);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeIntervalSeconds(1);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeTimeoutSeconds(1);
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeEnabled(true);
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeSql("SELECT 'OK'::text");
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeExpectedValue("OK");
        backupProperties.getPostgres().setHost("127.0.0.1");
        backupProperties.getPostgres().setPort(1);
        backupProperties.getPostgres().setDatabase("probe_db");

        Path dataDir = Files.createTempDirectory("pg-physical-data-probe");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-probe");
        Path baselineRootDir = Files.createTempDirectory("pg-physical-baseline-probe");
        Files.writeString(dataDir.resolve("current.data"), "before-replay");

        Path statusMarker = Files.createTempFile("pg-rollback-status-marker", ".flag");
        Files.deleteIfExists(statusMarker);

        Path pgCtlScript = Files.createTempFile("pg-ctl-mock-probe", ".sh");
        Files.writeString(pgCtlScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "HAS_START=0\n"
                        + "HAS_OPTIONS=0\n"
                        + "HAS_STATUS=0\n"
                        + "for ARG in \"$@\"; do\n"
                        + "  [ \"$ARG\" = \"start\" ] && HAS_START=1\n"
                        + "  [ \"$ARG\" = \"-o\" ] && HAS_OPTIONS=1\n"
                        + "  [ \"$ARG\" = \"status\" ] && HAS_STATUS=1\n"
                        + "done\n"
                        + "if [ \"$HAS_STATUS\" = \"1\" ]; then\n"
                        + "  touch \"" + statusMarker + "\"\n"
                        + "  exit 0\n"
                        + "fi\n"
                        + "if [ \"$HAS_START\" = \"1\" ] && [ \"$HAS_OPTIONS\" = \"1\" ]; then\n"
                        + "  exit 1\n"
                        + "fi\n"
                        + "exit 0\n");
        pgCtlScript.toFile().setExecutable(true);

        Path pgBasebackupScript = Files.createTempFile("pg-basebackup-mock-probe", ".sh");
        Files.writeString(pgBasebackupScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "OUT_DIR=\"\"\n"
                        + "while [ \"$#\" -gt 0 ]; do\n"
                        + "  if [ \"$1\" = \"-D\" ]; then\n"
                        + "    shift\n"
                        + "    OUT_DIR=\"$1\"\n"
                        + "  fi\n"
                        + "  shift\n"
                        + "done\n"
                        + "mkdir -p \"$OUT_DIR\"\n"
                        + "echo baseline > \"$OUT_DIR/base.marker\"\n"
                        + "exit 0\n");
        pgBasebackupScript.toFile().setExecutable(true);

        backupProperties.getPostgres().setPgCtlPath(pgCtlScript.toString());
        backupProperties.getPostgres().setPgBasebackupPath(pgBasebackupScript.toString());
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineRootDir.toString());

        RollbackProbeFakeDriver fakeDriver = new RollbackProbeFakeDriver(
                "jdbc:postgresql://127.0.0.1:1/probe_db",
                "SELECT 'OK'::text",
                "OK");
        DriverManager.registerDriver(fakeDriver);
        Path incrementalFile = Files.createTempFile("pg-inc-wal-physical-probe", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/51\n-- wal-end: 0/52\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThat(executor.restore(artifact, "target_db")).isFalse();
            assertThat(Files.exists(statusMarker)).isTrue();
            assertThat(fakeDriver.getSqlProbeQueryCount()).isGreaterThan(0);
            assertThat(fakeDriver.getBusinessProbeQueryCount()).isGreaterThan(0);
        } finally {
            DriverManager.deregisterDriver(fakeDriver);
            Files.deleteIfExists(incrementalFile);
            Files.deleteIfExists(pgCtlScript);
            Files.deleteIfExists(pgBasebackupScript);
            Files.deleteIfExists(statusMarker);
            Path parent = dataDir.getParent();
            if (parent != null && Files.exists(parent)) {
                try (var children = Files.list(parent)) {
                    for (Path child : children.toList()) {
                        String name = child.getFileName() == null ? "" : child.getFileName().toString();
                        if (name.startsWith("pg-physical-data-probe")
                                || name.startsWith("pg-physical-archive-probe")
                                || name.startsWith("pg-physical-baseline-probe")
                                || name.startsWith(PostgresBackupExecutor.PHYSICAL_ROLLBACK_SNAPSHOT_PREFIX)) {
                            deleteRecursively(child);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("启用业务一致性探针但SQL为空时应拒绝恢复")
    void shouldRejectWhenBusinessProbeEnabledButSqlMissing() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPgBasebackupPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(2);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeEnabled(true);
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeEnabled(true);
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeSql(" ");

        Path dataDir = Files.createTempDirectory("pg-physical-data-business-probe-invalid");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-business-probe-invalid");
        Path baselineDir = Files.createTempDirectory("pg-physical-baseline-business-probe-invalid");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineDir.toString());

        Path incrementalFile = Files.createTempFile("pg-inc-wal-business-probe-invalid", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/61\n-- wal-end: 0/62\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("businessProbeSql");
        } finally {
            Files.deleteIfExists(incrementalFile);
            deleteRecursively(dataDir);
            deleteRecursively(archiveDir);
            deleteRecursively(baselineDir);
        }
    }

    @Test
    @DisplayName("未启用健康探针时不允许单独启用业务一致性探针")
    void shouldRejectWhenBusinessProbeEnabledWithoutHealthProbe() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPgBasebackupPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(2);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeEnabled(false);
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeEnabled(true);
        backupProperties.getPostgres().setPhysicalReplayRollbackBusinessProbeSql("SELECT 1");

        Path dataDir = Files.createTempDirectory("pg-physical-data-business-probe-no-health");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-business-probe-no-health");
        Path baselineDir = Files.createTempDirectory("pg-physical-baseline-business-probe-no-health");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineDir.toString());

        Path incrementalFile = Files.createTempFile("pg-inc-wal-business-probe-no-health", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/71\n-- wal-end: 0/72\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executor.restore(artifact, "target_db"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("必须先启用回滚健康探针");
        } finally {
            Files.deleteIfExists(incrementalFile);
            deleteRecursively(dataDir);
            deleteRecursively(archiveDir);
            deleteRecursively(baselineDir);
        }
    }

    @Test
    @DisplayName("pg_basebackup失败后应自动清理残留并上报指标")
    void shouldCleanupFailedBaselineAndRecordMetrics() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(2);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeEnabled(false);
        backupProperties.getPostgres().setPhysicalReplayBaselineCleanupOnBasebackupFailure(true);

        Path dataDir = Files.createTempDirectory("pg-physical-data-basebackup-fail-cleanup");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-basebackup-fail-cleanup");
        Path baselineRootDir = Files.createTempDirectory("pg-physical-baseline-basebackup-fail-cleanup");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineRootDir.toString());

        Path pgBasebackupScript = Files.createTempFile("pg-basebackup-fail-cleanup", ".sh");
        Files.writeString(pgBasebackupScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "OUT_DIR=\"\"\n"
                        + "while [ \"$#\" -gt 0 ]; do\n"
                        + "  if [ \"$1\" = \"-D\" ]; then\n"
                        + "    shift\n"
                        + "    OUT_DIR=\"$1\"\n"
                        + "  fi\n"
                        + "  shift\n"
                        + "done\n"
                        + "mkdir -p \"$OUT_DIR\"\n"
                        + "echo partial > \"$OUT_DIR/partial.marker\"\n"
                        + "exit 13\n");
        pgBasebackupScript.toFile().setExecutable(true);
        backupProperties.getPostgres().setPgBasebackupPath(pgBasebackupScript.toString());

        BackupMetricsRegistrar metricsRegistrar = Mockito.mock(BackupMetricsRegistrar.class);
        PostgresBackupExecutor executorWithMetrics = new PostgresBackupExecutor(
                lockManager,
                retryTemplate,
                storageProvider,
                checksumService,
                backupHistoryMapper,
                metricsRegistrar,
                backupProperties,
                walParser
        );

        Path incrementalFile = Files.createTempFile("pg-inc-wal-basebackup-fail-cleanup", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/81\n-- wal-end: 0/82\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executorWithMetrics.restore(artifact, "target_db"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已自动清理残留目录");
            try (var children = Files.list(baselineRootDir)) {
                assertThat(children.toList()).isEmpty();
            }

            Mockito.verify(metricsRegistrar).recordPostgresPhysicalBaselineFailure();
            Mockito.verify(metricsRegistrar).recordPostgresPhysicalBaselineCleanup(true);
            Mockito.verify(metricsRegistrar, Mockito.never()).recordPostgresPhysicalBaselineCleanup(false);
        } finally {
            Files.deleteIfExists(incrementalFile);
            Files.deleteIfExists(pgBasebackupScript);
            deleteRecursively(dataDir);
            deleteRecursively(archiveDir);
            deleteRecursively(baselineRootDir);
        }
    }

    @Test
    @DisplayName("关闭清理策略时pg_basebackup失败应保留残留目录")
    void shouldKeepFailedBaselineWhenCleanupDisabled() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayMode("wal_physical_builtin");
        backupProperties.getPostgres().setPgCtlPath("/bin/echo");
        backupProperties.getPostgres().setPhysicalReplayRestoreCommandTemplate("cp ${archiveDir}/%f \"%p\"");
        backupProperties.getPostgres().setPhysicalReplayStopTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayRollbackStartTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayBasebackupTimeoutSeconds(10);
        backupProperties.getPostgres().setPhysicalReplayKeepLatestBaselines(2);
        backupProperties.getPostgres().setPhysicalReplayRollbackHealthProbeEnabled(false);
        backupProperties.getPostgres().setPhysicalReplayBaselineCleanupOnBasebackupFailure(false);

        Path dataDir = Files.createTempDirectory("pg-physical-data-basebackup-fail-keep");
        Path archiveDir = Files.createTempDirectory("pg-physical-archive-basebackup-fail-keep");
        Path baselineRootDir = Files.createTempDirectory("pg-physical-baseline-basebackup-fail-keep");
        backupProperties.getPostgres().setPhysicalReplayDataDir(dataDir.toString());
        backupProperties.getPostgres().setPhysicalReplayArchiveDir(archiveDir.toString());
        backupProperties.getPostgres().setPhysicalReplayBaselineDir(baselineRootDir.toString());

        Path pgBasebackupScript = Files.createTempFile("pg-basebackup-fail-keep", ".sh");
        Files.writeString(pgBasebackupScript,
                "#!/bin/sh\n"
                        + "set -e\n"
                        + "OUT_DIR=\"\"\n"
                        + "while [ \"$#\" -gt 0 ]; do\n"
                        + "  if [ \"$1\" = \"-D\" ]; then\n"
                        + "    shift\n"
                        + "    OUT_DIR=\"$1\"\n"
                        + "  fi\n"
                        + "  shift\n"
                        + "done\n"
                        + "mkdir -p \"$OUT_DIR\"\n"
                        + "echo partial > \"$OUT_DIR/partial.marker\"\n"
                        + "exit 14\n");
        pgBasebackupScript.toFile().setExecutable(true);
        backupProperties.getPostgres().setPgBasebackupPath(pgBasebackupScript.toString());

        BackupMetricsRegistrar metricsRegistrar = Mockito.mock(BackupMetricsRegistrar.class);
        PostgresBackupExecutor executorWithMetrics = new PostgresBackupExecutor(
                lockManager,
                retryTemplate,
                storageProvider,
                checksumService,
                backupHistoryMapper,
                metricsRegistrar,
                backupProperties,
                walParser
        );

        Path incrementalFile = Files.createTempFile("pg-inc-wal-basebackup-fail-keep", ".log");
        try {
            Files.writeString(incrementalFile,
                    PostgresBackupExecutor.WAL_DUMP_INCREMENTAL_MARKER + "\n-- wal-start: 0/91\n-- wal-end: 0/92\n");
            BackupArtifact artifact = BackupArtifact.builder()
                    .file(incrementalFile.toFile())
                    .backupType("incremental")
                    .build();

            assertThatThrownBy(() -> executorWithMetrics.restore(artifact, "target_db"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已关闭残留自动清理");
            try (var children = Files.list(baselineRootDir)) {
                List<Path> remained = children.toList();
                assertThat(remained).hasSize(1);
                assertThat(Files.exists(remained.get(0).resolve("partial.marker"))).isTrue();
            }

            Mockito.verify(metricsRegistrar).recordPostgresPhysicalBaselineFailure();
            Mockito.verify(metricsRegistrar, Mockito.never()).recordPostgresPhysicalBaselineCleanup(Mockito.anyBoolean());
        } finally {
            Files.deleteIfExists(incrementalFile);
            Files.deleteIfExists(pgBasebackupScript);
            deleteRecursively(dataDir);
            deleteRecursively(archiveDir);
            deleteRecursively(baselineRootDir);
        }
    }

    private static final class RollbackProbeFakeDriver implements Driver {
        private final String expectedUrlPrefix;
        private final String businessProbeSql;
        private final String businessProbeResult;
        private final AtomicInteger sqlProbeQueryCount = new AtomicInteger(0);
        private final AtomicInteger businessProbeQueryCount = new AtomicInteger(0);

        private RollbackProbeFakeDriver(String expectedUrlPrefix) {
            this(expectedUrlPrefix, null, null);
        }

        private RollbackProbeFakeDriver(String expectedUrlPrefix,
                                        String businessProbeSql,
                                        String businessProbeResult) {
            this.expectedUrlPrefix = expectedUrlPrefix;
            this.businessProbeSql = businessProbeSql;
            this.businessProbeResult = businessProbeResult;
        }

        int getSqlProbeQueryCount() {
            return sqlProbeQueryCount.get();
        }

        int getBusinessProbeQueryCount() {
            return businessProbeQueryCount.get();
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            Connection connection = Mockito.mock(Connection.class);
            Statement statement = Mockito.mock(Statement.class);
            Mockito.when(connection.createStatement()).thenReturn(statement);
            Mockito.when(statement.executeQuery(Mockito.anyString())).thenAnswer(invocation -> {
                String sql = invocation.getArgument(0, String.class);
                if ("SELECT 1".equals(sql)) {
                    sqlProbeQueryCount.incrementAndGet();
                    ResultSet sqlProbeResultSet = Mockito.mock(ResultSet.class);
                    Mockito.when(sqlProbeResultSet.next()).thenReturn(true);
                    return sqlProbeResultSet;
                }
                if (businessProbeSql != null && businessProbeSql.equals(sql)) {
                    businessProbeQueryCount.incrementAndGet();
                    ResultSet businessProbeResultSet = Mockito.mock(ResultSet.class);
                    Mockito.when(businessProbeResultSet.next()).thenReturn(true);
                    Mockito.when(businessProbeResultSet.getObject(1)).thenReturn(businessProbeResult);
                    return businessProbeResultSet;
                }
                throw new SQLException("unexpected sql: " + sql);
            });
            return connection;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith(expectedUrlPrefix);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("not supported");
        }
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            for (Path node : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(node);
            }
        }
    }
}
