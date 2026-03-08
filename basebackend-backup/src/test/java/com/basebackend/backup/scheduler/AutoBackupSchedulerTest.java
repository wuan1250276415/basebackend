package com.basebackend.backup.scheduler;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.entity.BackupTask;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.domain.mapper.BackupTaskMapper;
import com.basebackend.backup.infrastructure.executor.BackupRequest;
import com.basebackend.backup.infrastructure.executor.IncrementalBackupRequest;
import com.basebackend.backup.infrastructure.executor.impl.MySqlBackupExecutor;
import com.basebackend.backup.infrastructure.executor.impl.PostgresBackupExecutor;
import com.basebackend.backup.service.BackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoBackupScheduler 调度器测试")
class AutoBackupSchedulerTest {

    @Mock
    private BackupService backupService;
    @Mock
    private MySqlBackupExecutor mySqlBackupExecutor;
    @Mock
    private PostgresBackupExecutor postgresBackupExecutor;
    @Mock
    private BackupHistoryMapper backupHistoryMapper;
    @Mock
    private BackupTaskMapper backupTaskMapper;

    private BackupProperties backupProperties;
    private AutoBackupScheduler scheduler;

    @BeforeEach
    void setUp() {
        backupProperties = new BackupProperties();
        backupProperties.setEnabled(true);
        backupProperties.getDatabase().setHost("mysql-host");
        backupProperties.getDatabase().setPort(3306);
        backupProperties.getDatabase().setUsername("mysql-user");
        backupProperties.getDatabase().setPassword("mysql-pass");
        backupProperties.getDatabase().setDatabase("mysql_db");
        backupProperties.getPostgres().setHost("pg-host");
        backupProperties.getPostgres().setPort(5432);
        backupProperties.getPostgres().setUsername("pg-user");
        backupProperties.getPostgres().setPassword("pg-pass");
        backupProperties.getPostgres().setDatabase("pg_db");

        scheduler = new AutoBackupScheduler(
                List.of(backupService),
                backupProperties,
                mySqlBackupExecutor,
                postgresBackupExecutor,
                backupHistoryMapper,
                backupTaskMapper
        );
    }

    @Test
    @DisplayName("PostgreSQL全量调度应走执行器链路")
    void shouldRoutePostgresqlFullBackupToExecutorPipeline() throws Exception {
        when(backupService.getDatasourceType()).thenReturn("postgresql");
        when(backupTaskMapper.selectByDatasourceType("postgresql"))
                .thenReturn(List.of(task(101L, "postgresql")));
        when(postgresBackupExecutor.execute(any(BackupRequest.class))).thenReturn(successHistory(900L));

        scheduler.autoFullBackup();

        ArgumentCaptor<BackupRequest> captor = ArgumentCaptor.forClass(BackupRequest.class);
        verify(postgresBackupExecutor).execute(captor.capture());
        verify(mySqlBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(backupService, never()).fullBackup();

        BackupRequest request = captor.getValue();
        assertThat(request.getDatasourceType()).isEqualTo("postgresql");
        assertThat(request.getTaskId()).isEqualTo(101L);
        assertThat(request.getDatabaseConfig().getHost()).isEqualTo("pg-host");
        assertThat(request.getDatabaseConfig().getPort()).isEqualTo(5432);
        assertThat(request.getDatabaseConfig().getUsername()).isEqualTo("pg-user");
        assertThat(request.getDatabaseConfig().getDatabase()).isEqualTo("pg_db");
    }

    @Test
    @DisplayName("PostgreSQL任务ID查询应兼容postgres别名")
    void shouldFallbackToPostgresAliasWhenResolvingTaskId() throws Exception {
        when(backupService.getDatasourceType()).thenReturn("postgres");
        when(backupTaskMapper.selectByDatasourceType("postgresql")).thenReturn(List.of());
        when(backupTaskMapper.selectByDatasourceType("postgres"))
                .thenReturn(List.of(task(202L, "postgres")));
        when(postgresBackupExecutor.execute(any(BackupRequest.class))).thenReturn(successHistory(901L));

        scheduler.autoFullBackup();

        ArgumentCaptor<BackupRequest> captor = ArgumentCaptor.forClass(BackupRequest.class);
        verify(postgresBackupExecutor).execute(captor.capture());
        verify(backupTaskMapper).selectByDatasourceType("postgresql");
        verify(backupTaskMapper).selectByDatasourceType("postgres");

        BackupRequest request = captor.getValue();
        assertThat(request.getDatasourceType()).isEqualTo("postgresql");
        assertThat(request.getTaskId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("MySQL增量调度应走执行器并使用最近增量终点位点")
    void shouldRouteMysqlIncrementalToExecutorPipelineWithLatestIncrementalPosition() throws Exception {
        backupProperties.setIncrementalBackupEnabled(true);
        when(backupService.getDatasourceType()).thenReturn("mysql");
        when(backupTaskMapper.selectByDatasourceType("mysql"))
                .thenReturn(List.of(task(301L, "mysql")));

        BackupHistory latestFull = new BackupHistory();
        latestFull.setId(1001L);
        latestFull.setBinlogEnd("mysql-bin.000010:120");
        when(backupHistoryMapper.selectLatestFullBackup(301L)).thenReturn(latestFull);

        BackupHistory latestIncremental = new BackupHistory();
        latestIncremental.setId(1002L);
        latestIncremental.setBinlogEnd("mysql-bin.000010:280");
        when(backupHistoryMapper.selectLatestIncrementalByBaseFullId(1001L)).thenReturn(latestIncremental);

        when(mySqlBackupExecutor.execute(any(BackupRequest.class))).thenReturn(successIncrementalHistory(901L));

        scheduler.autoIncrementalBackup();

        ArgumentCaptor<BackupRequest> captor = ArgumentCaptor.forClass(BackupRequest.class);
        verify(mySqlBackupExecutor).execute(captor.capture());
        verify(backupService, never()).incrementalBackup();

        BackupRequest request = captor.getValue();
        assertThat(request).isInstanceOf(IncrementalBackupRequest.class);
        IncrementalBackupRequest incrementalRequest = (IncrementalBackupRequest) request;
        assertThat(incrementalRequest.getTaskId()).isEqualTo(301L);
        assertThat(incrementalRequest.getDatasourceType()).isEqualTo("mysql");
        assertThat(incrementalRequest.getBackupType()).isEqualTo("incremental");
        assertThat(incrementalRequest.getBaseFullBackupId()).isEqualTo(1001L);
        assertThat(incrementalRequest.getStartPosition()).isEqualTo("mysql-bin.000010:280");
        assertThat(incrementalRequest.getBinlogStartPosition()).isEqualTo("mysql-bin.000010:280");
    }

    @Test
    @DisplayName("MySQL增量调度缺少全量基线时不应调用执行器")
    void shouldSkipMysqlIncrementalExecutorWhenNoFullBackup() throws Exception {
        backupProperties.setIncrementalBackupEnabled(true);
        when(backupService.getDatasourceType()).thenReturn("mysql");
        when(backupTaskMapper.selectByDatasourceType("mysql"))
                .thenReturn(List.of(task(401L, "mysql")));
        when(backupHistoryMapper.selectLatestFullBackup(401L)).thenReturn(null);

        scheduler.autoIncrementalBackup();

        verify(mySqlBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(backupService, never()).incrementalBackup();
    }

    @Test
    @DisplayName("PostgreSQL增量调度应走执行器并使用最近增量终点LSN")
    void shouldRoutePostgresqlIncrementalToExecutorPipelineWithLatestWalPosition() throws Exception {
        backupProperties.setIncrementalBackupEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        when(backupService.getDatasourceType()).thenReturn("postgresql");
        when(backupTaskMapper.selectByDatasourceType("postgresql"))
                .thenReturn(List.of(task(501L, "postgresql")));

        BackupHistory latestFull = new BackupHistory();
        latestFull.setId(2001L);
        latestFull.setWalEnd("0/16B5A20");
        when(backupHistoryMapper.selectLatestFullBackup(501L)).thenReturn(latestFull);

        BackupHistory latestIncremental = new BackupHistory();
        latestIncremental.setId(2002L);
        latestIncremental.setWalEnd("0/16B6F90");
        when(backupHistoryMapper.selectLatestIncrementalByBaseFullId(2001L)).thenReturn(latestIncremental);

        when(postgresBackupExecutor.execute(any(BackupRequest.class))).thenReturn(successIncrementalHistory(902L));

        scheduler.autoIncrementalBackup();

        ArgumentCaptor<BackupRequest> captor = ArgumentCaptor.forClass(BackupRequest.class);
        verify(postgresBackupExecutor).execute(captor.capture());
        verify(mySqlBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(backupService, never()).incrementalBackup();

        BackupRequest request = captor.getValue();
        assertThat(request).isInstanceOf(IncrementalBackupRequest.class);
        IncrementalBackupRequest incrementalRequest = (IncrementalBackupRequest) request;
        assertThat(incrementalRequest.getTaskId()).isEqualTo(501L);
        assertThat(incrementalRequest.getDatasourceType()).isEqualTo("postgresql");
        assertThat(incrementalRequest.getBackupType()).isEqualTo("incremental");
        assertThat(incrementalRequest.getBaseFullBackupId()).isEqualTo(2001L);
        assertThat(incrementalRequest.getStartPosition()).isEqualTo("0/16B6F90");
        assertThat(incrementalRequest.getDatabaseConfig().getHost()).isEqualTo("pg-host");
        assertThat(incrementalRequest.getDatabaseConfig().getPort()).isEqualTo(5432);
        assertThat(incrementalRequest.getDatabaseConfig().getUsername()).isEqualTo("pg-user");
        assertThat(incrementalRequest.getDatabaseConfig().getDatabase()).isEqualTo("pg_db");
    }

    @Test
    @DisplayName("PostgreSQL增量调度缺少LSN起点时不应调用执行器")
    void shouldSkipPostgresqlIncrementalExecutorWhenWalStartMissing() throws Exception {
        backupProperties.setIncrementalBackupEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayEnabled(true);
        when(backupService.getDatasourceType()).thenReturn("postgresql");
        when(backupTaskMapper.selectByDatasourceType("postgresql"))
                .thenReturn(List.of(task(601L, "postgresql")));

        BackupHistory latestFull = new BackupHistory();
        latestFull.setId(3001L);
        latestFull.setWalEnd(null);
        when(backupHistoryMapper.selectLatestFullBackup(601L)).thenReturn(latestFull);
        when(backupHistoryMapper.selectLatestIncrementalByBaseFullId(3001L)).thenReturn(null);

        scheduler.autoIncrementalBackup();

        verify(postgresBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(mySqlBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(backupService, never()).incrementalBackup();
    }

    @Test
    @DisplayName("PostgreSQL未启用回放能力时应跳过自动增量调度")
    void shouldSkipPostgresqlIncrementalWhenReplayDisabled() throws Exception {
        backupProperties.setIncrementalBackupEnabled(true);
        backupProperties.getPostgres().setIncrementalReplayEnabled(false);
        when(backupService.getDatasourceType()).thenReturn("postgresql");

        scheduler.autoIncrementalBackup();

        verify(postgresBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(mySqlBackupExecutor, never()).execute(any(BackupRequest.class));
        verify(backupService, never()).incrementalBackup();
    }

    private BackupTask task(Long id, String datasourceType) {
        BackupTask task = new BackupTask();
        task.setId(id);
        task.setDatasourceType(datasourceType);
        task.setEnabled(true);
        return task;
    }

    private BackupHistory successHistory(Long historyId) {
        LocalDateTime now = LocalDateTime.now();
        BackupHistory history = new BackupHistory();
        history.setId(historyId);
        history.setStatus("SUCCESS");
        history.setBackupType("full");
        history.setStartedAt(now.minusMinutes(1));
        history.setFinishedAt(now);
        history.setFileSize(1024L);
        return history;
    }

    private BackupHistory successIncrementalHistory(Long historyId) {
        BackupHistory history = successHistory(historyId);
        history.setBackupType("incremental");
        return history;
    }
}
