package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.entity.BackupTask;
import com.basebackend.backup.domain.entity.RestoreRecord;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.domain.mapper.BackupTaskMapper;
import com.basebackend.backup.domain.mapper.RestoreRecordMapper;
import com.basebackend.backup.infrastructure.executor.BackupArtifact;
import com.basebackend.backup.infrastructure.executor.IncrementalChain;
import com.basebackend.backup.infrastructure.executor.RestoreRequest;
import com.basebackend.backup.infrastructure.monitoring.BackupMetricsRegistrar;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestoreService 恢复服务测试")
class RestoreServiceTest {

    @Mock
    private BackupHistoryMapper backupHistoryMapper;
    @Mock
    private BackupTaskMapper backupTaskMapper;
    @Mock
    private RestoreRecordMapper restoreRecordMapper;
    @Mock
    private IncrementalChainManager incrementalChainManager;
    @Mock
    private MySqlBackupExecutor mysqlBackupExecutor;
    @Mock
    private PostgresBackupExecutor postgresBackupExecutor;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private ChecksumService checksumService;
    @Mock
    private BackupMetricsRegistrar backupMetricsRegistrar;

    private RestoreService restoreService;
    private BackupProperties backupProperties;

    @BeforeEach
    void setUp() {
        backupProperties = new BackupProperties();
        restoreService = new RestoreService(
                backupHistoryMapper,
                backupTaskMapper,
                restoreRecordMapper,
                incrementalChainManager,
                mysqlBackupExecutor,
                postgresBackupExecutor,
                backupProperties,
                storageProvider,
                checksumService,
                backupMetricsRegistrar
        );
    }

    @Test
    @DisplayName("应从存储位置下载文件并执行恢复")
    void shouldRestoreFromStorageLocation() throws Exception {
        // Given
        BackupHistory backup = new BackupHistory();
        backup.setId(100L);
        backup.setTaskId(1L);
        backup.setStatus("SUCCESS");
        backup.setBackupType("full");
        backup.setStartedAt(LocalDateTime.now().minusHours(1));
        backup.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"full/test.sql\",\"success\":true}]");

        when(backupHistoryMapper.selectById(100L)).thenReturn(backup);
        when(storageProvider.exists("backup", "full/test.sql")).thenReturn(true);
        when(storageProvider.download("backup", "full/test.sql"))
                .thenReturn(new ByteArrayInputStream("CREATE TABLE t(id INT);".getBytes(StandardCharsets.UTF_8)));
        when(mysqlBackupExecutor.restore(any(BackupArtifact.class), eq("target_db"))).thenReturn(true);
        BackupTask task = new BackupTask();
        task.setId(1L);
        task.setDatasourceType("mysql");
        when(backupTaskMapper.selectById(1L)).thenReturn(task);
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);

        RestoreRequest request = RestoreRequest.builder()
                .taskId(1L)
                .historyId(100L)
                .targetDatabase("target_db")
                .operator("tester")
                .build();

        // When
        boolean result = restoreService.restoreToBackup(request);

        // Then
        assertThat(result).isTrue();
        verify(storageProvider).download("backup", "full/test.sql");
        verify(mysqlBackupExecutor).restore(any(BackupArtifact.class), eq("target_db"));
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreSuccess(anyLong());
        verify(restoreRecordMapper).updateById(any(RestoreRecord.class));
    }

    @Test
    @DisplayName("缺少存储位置信息时应失败")
    void shouldFailWhenStorageLocationsMissing() {
        // Given
        BackupHistory backup = new BackupHistory();
        backup.setId(200L);
        backup.setTaskId(2L);
        backup.setStatus("SUCCESS");
        backup.setBackupType("full");
        backup.setStorageLocations(null);

        when(backupHistoryMapper.selectById(200L)).thenReturn(backup);
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);

        RestoreRequest request = RestoreRequest.builder()
                .taskId(2L)
                .historyId(200L)
                .operator("tester")
                .build();

        // Then
        assertThatThrownBy(() -> restoreService.restoreToBackup(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少存储位置信息");
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreFailure(anyLong());
    }

    @Test
    @DisplayName("PostgreSQL数据源应路由到Postgres执行器")
    void shouldRoutePostgresqlDatasource() throws Exception {
        BackupHistory backup = new BackupHistory();
        backup.setId(300L);
        backup.setTaskId(3L);
        backup.setStatus("SUCCESS");
        backup.setBackupType("full");
        backup.setStartedAt(LocalDateTime.now().minusMinutes(30));
        backup.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"full/pg.sql\",\"success\":true}]");

        when(backupHistoryMapper.selectById(300L)).thenReturn(backup);
        when(storageProvider.exists("backup", "full/pg.sql")).thenReturn(true);
        when(storageProvider.download("backup", "full/pg.sql"))
                .thenReturn(new ByteArrayInputStream("select 1;".getBytes(StandardCharsets.UTF_8)));
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);
        when(postgresBackupExecutor.restore(any(BackupArtifact.class), eq(null))).thenReturn(true);

        BackupTask task = new BackupTask();
        task.setId(3L);
        task.setDatasourceType("postgresql");
        when(backupTaskMapper.selectById(3L)).thenReturn(task);

        RestoreRequest request = RestoreRequest.builder()
                .taskId(3L)
                .historyId(300L)
                .operator("tester")
                .build();

        boolean result = restoreService.restoreToBackup(request);

        assertThat(result).isTrue();
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreSuccess(anyLong());
        verify(postgresBackupExecutor).restore(any(BackupArtifact.class), eq(null));
    }

    @Test
    @DisplayName("PostgreSQL PITR 包含增量链时应快速失败且不执行恢复")
    void shouldFailFastWhenPostgresqlPitrContainsIncrementalBackups() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fullTime = now.minusHours(1);
        LocalDateTime incrementalTime = now.minusMinutes(30);

        BackupHistory full = new BackupHistory();
        full.setId(401L);
        full.setTaskId(4L);
        full.setBackupType("full");
        full.setStatus("SUCCESS");
        full.setStartedAt(fullTime);

        BackupHistory incremental = new BackupHistory();
        incremental.setId(402L);
        incremental.setTaskId(4L);
        incremental.setBackupType("incremental");
        incremental.setStatus("SUCCESS");
        incremental.setStartedAt(incrementalTime);
        incremental.setBaseFullId(401L);

        IncrementalChain chain = IncrementalChain.builder()
                .chainId("chain_4_401")
                .fullBackup(full)
                .incrementalBackups(List.of(incremental))
                .latestIncrementalTime(incrementalTime)
                .isBroken(false)
                .build();

        when(incrementalChainManager.buildChainToTime(4L, incrementalTime)).thenReturn(chain);
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);

        BackupTask task = new BackupTask();
        task.setId(4L);
        task.setDatasourceType("postgresql");
        when(backupTaskMapper.selectById(4L)).thenReturn(task);

        RestoreRequest request = RestoreRequest.builder()
                .taskId(4L)
                .targetTime(incrementalTime)
                .operator("tester")
                .build();

        assertThatThrownBy(() -> restoreService.restoreToPoint(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("PostgreSQL增量回放尚未实现");

        verify(postgresBackupExecutor, never()).restore(any(BackupArtifact.class), any());
        verify(mysqlBackupExecutor, never()).restore(any(BackupArtifact.class), any());
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreFailure(anyLong());
    }

    @Test
    @DisplayName("启用PostgreSQL回放能力后应允许应用增量链恢复")
    void shouldAllowPostgresqlIncrementalRestoreWhenReplayEnabled() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fullTime = now.minusHours(1);
        LocalDateTime incrementalTime = now.minusMinutes(30);

        BackupHistory full = new BackupHistory();
        full.setId(501L);
        full.setTaskId(5L);
        full.setBackupType("full");
        full.setStatus("SUCCESS");
        full.setStartedAt(fullTime);
        full.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"full/pg-full.sql\",\"success\":true}]");

        BackupHistory incremental = new BackupHistory();
        incremental.setId(502L);
        incremental.setTaskId(5L);
        incremental.setBackupType("incremental");
        incremental.setStatus("SUCCESS");
        incremental.setStartedAt(incrementalTime);
        incremental.setBaseFullId(501L);
        incremental.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"incremental/pg-inc.sql\",\"success\":true}]");

        IncrementalChain chain = IncrementalChain.builder()
                .chainId("chain_5_501")
                .fullBackup(full)
                .incrementalBackups(List.of(incremental))
                .latestIncrementalTime(incrementalTime)
                .isBroken(false)
                .build();

        when(incrementalChainManager.buildChainToTime(5L, incrementalTime)).thenReturn(chain);
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);

        BackupTask task = new BackupTask();
        task.setId(5L);
        task.setDatasourceType("postgresql");
        when(backupTaskMapper.selectById(5L)).thenReturn(task);

        when(storageProvider.exists("backup", "full/pg-full.sql")).thenReturn(true);
        when(storageProvider.exists("backup", "incremental/pg-inc.sql")).thenReturn(true);
        when(storageProvider.download("backup", "full/pg-full.sql"))
                .thenReturn(new ByteArrayInputStream("select 1;".getBytes(StandardCharsets.UTF_8)));
        when(storageProvider.download("backup", "incremental/pg-inc.sql"))
                .thenReturn(new ByteArrayInputStream("select 2;".getBytes(StandardCharsets.UTF_8)));
        when(postgresBackupExecutor.restore(any(BackupArtifact.class), eq("target_db"))).thenReturn(true);

        RestoreRequest request = RestoreRequest.builder()
                .taskId(5L)
                .targetTime(incrementalTime)
                .targetDatabase("target_db")
                .operator("tester")
                .build();

        boolean result = restoreService.restoreToPoint(request);

        assertThat(result).isTrue();
        verify(postgresBackupExecutor, times(2)).restore(any(BackupArtifact.class), eq("target_db"));
        verify(mysqlBackupExecutor, never()).restore(any(BackupArtifact.class), any());
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreSuccess(anyLong());
    }

    @Test
    @DisplayName("PostgreSQL增量链恢复中执行器异常时应标记失败")
    void shouldFailPostgresqlIncrementalRestoreWhenExecutorThrows() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fullTime = now.minusHours(1);
        LocalDateTime incrementalTime = now.minusMinutes(20);

        BackupHistory full = new BackupHistory();
        full.setId(601L);
        full.setTaskId(6L);
        full.setBackupType("full");
        full.setStatus("SUCCESS");
        full.setStartedAt(fullTime);
        full.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"full/pg-full-601.sql\",\"success\":true}]");

        BackupHistory incremental = new BackupHistory();
        incremental.setId(602L);
        incremental.setTaskId(6L);
        incremental.setBackupType("incremental");
        incremental.setStatus("SUCCESS");
        incremental.setStartedAt(incrementalTime);
        incremental.setBaseFullId(601L);
        incremental.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"incremental/pg-inc-602.wal.log\",\"success\":true}]");

        IncrementalChain chain = IncrementalChain.builder()
                .chainId("chain_6_601")
                .fullBackup(full)
                .incrementalBackups(List.of(incremental))
                .latestIncrementalTime(incrementalTime)
                .isBroken(false)
                .build();

        when(incrementalChainManager.buildChainToTime(6L, incrementalTime)).thenReturn(chain);
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);

        BackupTask task = new BackupTask();
        task.setId(6L);
        task.setDatasourceType("postgresql");
        when(backupTaskMapper.selectById(6L)).thenReturn(task);

        when(storageProvider.exists("backup", "full/pg-full-601.sql")).thenReturn(true);
        when(storageProvider.exists("backup", "incremental/pg-inc-602.wal.log")).thenReturn(true);
        when(storageProvider.download("backup", "full/pg-full-601.sql"))
                .thenReturn(new ByteArrayInputStream("select 1;".getBytes(StandardCharsets.UTF_8)));
        when(storageProvider.download("backup", "incremental/pg-inc-602.wal.log"))
                .thenReturn(new ByteArrayInputStream("wal dump".getBytes(StandardCharsets.UTF_8)));

        when(postgresBackupExecutor.restore(any(BackupArtifact.class), eq("target_db")))
                .thenReturn(true)
                .thenThrow(new UnsupportedOperationException("wal-replay-command"));

        RestoreRequest request = RestoreRequest.builder()
                .taskId(6L)
                .targetTime(incrementalTime)
                .targetDatabase("target_db")
                .operator("tester")
                .build();

        assertThatThrownBy(() -> restoreService.restoreToPoint(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("wal-replay-command");

        verify(postgresBackupExecutor, times(2)).restore(any(BackupArtifact.class), eq("target_db"));
        verify(mysqlBackupExecutor, never()).restore(any(BackupArtifact.class), any());
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreFailure(anyLong());
    }

    @Test
    @DisplayName("PostgreSQL增量链恢复中执行器返回false时应失败")
    void shouldFailPostgresqlIncrementalRestoreWhenExecutorReturnsFalse() throws Exception {
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fullTime = now.minusHours(1);
        LocalDateTime incrementalTime = now.minusMinutes(10);

        BackupHistory full = new BackupHistory();
        full.setId(701L);
        full.setTaskId(7L);
        full.setBackupType("full");
        full.setStatus("SUCCESS");
        full.setStartedAt(fullTime);
        full.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"full/pg-full-701.sql\",\"success\":true}]");

        BackupHistory incremental = new BackupHistory();
        incremental.setId(702L);
        incremental.setTaskId(7L);
        incremental.setBackupType("incremental");
        incremental.setStatus("SUCCESS");
        incremental.setStartedAt(incrementalTime);
        incremental.setBaseFullId(701L);
        incremental.setStorageLocations("[{\"bucket\":\"backup\",\"key\":\"incremental/pg-inc-702.wal.log\",\"success\":true}]");

        IncrementalChain chain = IncrementalChain.builder()
                .chainId("chain_7_701")
                .fullBackup(full)
                .incrementalBackups(List.of(incremental))
                .latestIncrementalTime(incrementalTime)
                .isBroken(false)
                .build();

        when(incrementalChainManager.buildChainToTime(7L, incrementalTime)).thenReturn(chain);
        when(restoreRecordMapper.insert(any(RestoreRecord.class))).thenReturn(1);
        when(restoreRecordMapper.updateById(any(RestoreRecord.class))).thenReturn(1);

        BackupTask task = new BackupTask();
        task.setId(7L);
        task.setDatasourceType("postgresql");
        when(backupTaskMapper.selectById(7L)).thenReturn(task);

        when(storageProvider.exists("backup", "full/pg-full-701.sql")).thenReturn(true);
        when(storageProvider.exists("backup", "incremental/pg-inc-702.wal.log")).thenReturn(true);
        when(storageProvider.download("backup", "full/pg-full-701.sql"))
                .thenReturn(new ByteArrayInputStream("select 1;".getBytes(StandardCharsets.UTF_8)));
        when(storageProvider.download("backup", "incremental/pg-inc-702.wal.log"))
                .thenReturn(new ByteArrayInputStream("wal dump".getBytes(StandardCharsets.UTF_8)));

        when(postgresBackupExecutor.restore(any(BackupArtifact.class), eq("target_db")))
                .thenReturn(true)
                .thenReturn(false);

        RestoreRequest request = RestoreRequest.builder()
                .taskId(7L)
                .targetTime(incrementalTime)
                .targetDatabase("target_db")
                .operator("tester")
                .build();

        assertThatThrownBy(() -> restoreService.restoreToPoint(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("应用备份失败");

        verify(postgresBackupExecutor, times(2)).restore(any(BackupArtifact.class), eq("target_db"));
        verify(mysqlBackupExecutor, never()).restore(any(BackupArtifact.class), any());
        verify(backupMetricsRegistrar).recordRestoreStart();
        verify(backupMetricsRegistrar, atLeastOnce()).recordRestoreFailure(anyLong());
    }
}
