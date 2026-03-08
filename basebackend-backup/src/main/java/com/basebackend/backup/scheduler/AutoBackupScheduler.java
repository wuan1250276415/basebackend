package com.basebackend.backup.scheduler;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.entity.BackupTask;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.domain.mapper.BackupTaskMapper;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.enums.BackupType;
import com.basebackend.backup.infrastructure.executor.BackupRequest;
import com.basebackend.backup.infrastructure.executor.IncrementalBackupRequest;
import com.basebackend.backup.infrastructure.executor.impl.MySqlBackupExecutor;
import com.basebackend.backup.infrastructure.executor.impl.PostgresBackupExecutor;
import com.basebackend.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 自动备份调度器
 * <p>
 * 支持所有已注册的数据源（MySQL、PostgreSQL等），
 * 通过注入所有 {@link BackupService} 实现自动执行备份。
 * </p>
 *
 * @author BaseBackend
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "backup", name = "auto-backup-enabled", havingValue = "true")
public class AutoBackupScheduler {

    private final List<BackupService> backupServices;
    private final BackupProperties backupProperties;
    private final MySqlBackupExecutor mySqlBackupExecutor;
    private final PostgresBackupExecutor postgresBackupExecutor;
    private final BackupHistoryMapper backupHistoryMapper;
    private final BackupTaskMapper backupTaskMapper;

    /**
     * 自动全量备份（每天凌晨2点）
     */
    @Scheduled(cron = "${backup.auto-backup-cron:0 0 2 * * ?}")
    public void autoFullBackup() {
        if (!backupProperties.isEnabled()) {
            return;
        }

        for (BackupService service : backupServices) {
            String datasourceType = normalizeDatasourceType(service.getDatasourceType());
            log.info("开始执行自动全量备份任务: datasource={}", datasourceType);
            try {
                BackupRecord record = useExecutorPipeline(datasourceType)
                        ? runFullBackupByExecutor(datasourceType)
                        : service.fullBackup();
                logBackupResult("全量", datasourceType, record);
            } catch (Exception e) {
                log.error("自动全量备份任务失败: datasource={}", datasourceType, e);
            }
        }
    }

    /**
     * 自动清理过期备份（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoCleanExpired() {
        if (!backupProperties.isEnabled()) {
            return;
        }

        for (BackupService service : backupServices) {
            log.info("开始执行自动清理过期备份任务: datasource={}", service.getDatasourceType());
            try {
                int count = service.cleanExpiredBackups();
                log.info("自动清理过期备份任务完成: datasource={}, 清理 {} 个备份",
                        service.getDatasourceType(), count);
            } catch (Exception e) {
                log.error("自动清理过期备份任务失败: datasource={}", service.getDatasourceType(), e);
            }
        }
    }

    /**
     * 自动增量备份（每小时）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoIncrementalBackup() {
        if (!backupProperties.isEnabled() || !backupProperties.isIncrementalBackupEnabled()) {
            return;
        }

        for (BackupService service : backupServices) {
            String datasourceType = normalizeDatasourceType(service.getDatasourceType());
            log.info("开始执行自动增量备份任务: datasource={}", datasourceType);

            if ("postgresql".equals(datasourceType) && !backupProperties.getPostgres().isIncrementalReplayEnabled()) {
                log.warn("跳过PostgreSQL自动增量备份: 未启用incrementalReplayEnabled，避免生成不可恢复增量");
                continue;
            }

            try {
                BackupRecord record = useExecutorIncrementalPipeline(datasourceType)
                        ? runIncrementalBackupByExecutor(datasourceType)
                        : service.incrementalBackup();
                logBackupResult("增量", datasourceType, record);
            } catch (IllegalStateException e) {
                log.warn("自动增量备份任务跳过: datasource={}, reason={}", datasourceType, e.getMessage());
            } catch (Exception e) {
                log.error("自动增量备份任务失败: datasource={}", datasourceType, e);
            }
        }
    }

    private void logBackupResult(String backupType, String datasource, BackupRecord record) {
        if (record == null) {
            log.error("自动{}备份任务失败: datasource={}, record为空", backupType, datasource);
            return;
        }
        if (record.getStatus() == BackupStatus.SUCCESS) {
            log.info("自动{}备份任务完成: datasource={}, backupId={}",
                    backupType, datasource, record.getBackupId());
            return;
        }
        log.error("自动{}备份任务失败: datasource={}, backupId={}, status={}, error={}",
                backupType, datasource, record.getBackupId(), record.getStatus(), record.getErrorMessage());
    }

    private boolean useExecutorPipeline(String datasourceType) {
        return "mysql".equals(datasourceType) || "postgresql".equals(datasourceType);
    }

    private boolean useExecutorIncrementalPipeline(String datasourceType) {
        return "mysql".equals(datasourceType) || "postgresql".equals(datasourceType);
    }

    private BackupRecord runFullBackupByExecutor(String datasourceType) throws Exception {
        BackupRequest request = buildFullBackupRequest(datasourceType);
        BackupHistory history;
        if ("mysql".equals(request.getDatasourceType())) {
            history = mySqlBackupExecutor.execute(request);
        } else if ("postgresql".equals(request.getDatasourceType())) {
            history = postgresBackupExecutor.execute(request);
        } else {
            throw new IllegalArgumentException("不支持的数据源类型: " + request.getDatasourceType());
        }
        return toRecord(history, request.getDatasourceType());
    }

    private BackupRecord runIncrementalBackupByExecutor(String datasourceType) throws Exception {
        IncrementalBackupRequest request = buildIncrementalBackupRequest(datasourceType);
        BackupHistory history;
        if ("mysql".equals(request.getDatasourceType())) {
            history = mySqlBackupExecutor.execute(request);
        } else if ("postgresql".equals(request.getDatasourceType())) {
            history = postgresBackupExecutor.execute(request);
        } else {
            throw new IllegalArgumentException("当前仅支持MySQL/PostgreSQL增量执行器: " + request.getDatasourceType());
        }
        return toRecord(history, request.getDatasourceType());
    }

    private BackupRequest buildFullBackupRequest(String datasourceType) {
        String normalizedType = normalizeDatasourceType(datasourceType);

        BackupRequest request = new BackupRequest();
        request.setTaskId(resolveTaskId(normalizedType));
        request.setDatasourceType(normalizedType);
        request.setBackupType("full");
        request.setStartTime(LocalDateTime.now());

        BackupRequest.DatabaseConfig dbConfig = new BackupRequest.DatabaseConfig();
        if ("postgresql".equals(normalizedType)) {
            dbConfig.setHost(backupProperties.getPostgres().getHost());
            dbConfig.setPort(backupProperties.getPostgres().getPort());
            dbConfig.setUsername(backupProperties.getPostgres().getUsername());
            dbConfig.setPassword(backupProperties.getPostgres().getPassword());
            dbConfig.setDatabase(backupProperties.getPostgres().getDatabase());
        } else {
            dbConfig.setHost(backupProperties.getDatabase().getHost());
            dbConfig.setPort(backupProperties.getDatabase().getPort());
            dbConfig.setUsername(backupProperties.getDatabase().getUsername());
            dbConfig.setPassword(backupProperties.getDatabase().getPassword());
            dbConfig.setDatabase(backupProperties.getDatabase().getDatabase());
        }
        request.setDatabaseConfig(dbConfig);
        return request;
    }

    private IncrementalBackupRequest buildIncrementalBackupRequest(String datasourceType) {
        String normalizedType = normalizeDatasourceType(datasourceType);
        if (!"mysql".equals(normalizedType) && !"postgresql".equals(normalizedType)) {
            throw new IllegalArgumentException("当前仅支持MySQL/PostgreSQL增量执行器: " + normalizedType);
        }

        Long taskId = resolveTaskId(normalizedType);
        BackupHistory latestFull = backupHistoryMapper.selectLatestFullBackup(taskId);
        if (latestFull == null || latestFull.getId() == null) {
            throw new IllegalStateException("未找到可用的全量备份，无法执行增量: taskId=" + taskId);
        }

        BackupHistory latestIncremental = backupHistoryMapper.selectLatestIncrementalByBaseFullId(latestFull.getId());
        String startPosition = resolveIncrementalStartPosition(normalizedType, latestFull, latestIncremental);
        if (startPosition == null || startPosition.isBlank()) {
            throw new IllegalStateException("缺少增量起始位点，无法执行增量: taskId=" + taskId + ", baseFullId=" + latestFull.getId());
        }

        IncrementalBackupRequest request = new IncrementalBackupRequest();
        request.setTaskId(taskId);
        request.setDatasourceType(normalizedType);
        request.setBackupType("incremental");
        request.setStartTime(LocalDateTime.now());
        request.setBaseFullBackupId(latestFull.getId());
        request.setStartPosition(startPosition);
        if ("mysql".equals(normalizedType)) {
            request.setBinlogStartPosition(startPosition);
        }
        request.setDatabaseConfig(buildIncrementalDatabaseConfig(normalizedType));
        return request;
    }

    private String resolveIncrementalStartPosition(String datasourceType,
                                                   BackupHistory latestFull,
                                                   BackupHistory latestIncremental) {
        if ("postgresql".equals(datasourceType)) {
            if (latestIncremental != null && latestIncremental.getWalEnd() != null
                    && !latestIncremental.getWalEnd().isBlank()) {
                return latestIncremental.getWalEnd();
            }
            return latestFull.getWalEnd();
        }

        if (latestIncremental != null && latestIncremental.getBinlogEnd() != null
                && !latestIncremental.getBinlogEnd().isBlank()) {
            return latestIncremental.getBinlogEnd();
        }
        return latestFull.getBinlogEnd();
    }

    private BackupRequest.DatabaseConfig buildIncrementalDatabaseConfig(String datasourceType) {
        BackupRequest.DatabaseConfig dbConfig = new BackupRequest.DatabaseConfig();
        if ("postgresql".equals(datasourceType)) {
            dbConfig.setHost(backupProperties.getPostgres().getHost());
            dbConfig.setPort(backupProperties.getPostgres().getPort());
            dbConfig.setUsername(backupProperties.getPostgres().getUsername());
            dbConfig.setPassword(backupProperties.getPostgres().getPassword());
            dbConfig.setDatabase(backupProperties.getPostgres().getDatabase());
        } else {
            dbConfig.setHost(backupProperties.getDatabase().getHost());
            dbConfig.setPort(backupProperties.getDatabase().getPort());
            dbConfig.setUsername(backupProperties.getDatabase().getUsername());
            dbConfig.setPassword(backupProperties.getDatabase().getPassword());
            dbConfig.setDatabase(backupProperties.getDatabase().getDatabase());
        }
        return dbConfig;
    }

    private Long resolveTaskId(String datasourceType) {
        String normalized = normalizeDatasourceType(datasourceType);
        try {
            List<String> candidateTypes = "postgresql".equals(normalized)
                    ? List.of("postgresql", "postgres")
                    : List.of(normalized);

            for (String candidateType : candidateTypes) {
                List<BackupTask> tasks = backupTaskMapper.selectByDatasourceType(candidateType);
                if (tasks != null && !tasks.isEmpty() && tasks.get(0).getId() != null) {
                    return tasks.get(0).getId();
                }
            }
        } catch (Exception e) {
            log.warn("查询备份任务失败，使用默认taskId: datasource={}", normalized, e);
        }
        return 0L;
    }

    private BackupRecord toRecord(BackupHistory history, String datasourceType) {
        if (history == null) {
            return null;
        }
        BackupType backupType = "incremental".equalsIgnoreCase(history.getBackupType())
                ? BackupType.INCREMENTAL
                : BackupType.FULL;
        return BackupRecord.builder()
                .backupId(history.getId() == null ? null : String.valueOf(history.getId()))
                .backupType(backupType)
                .status(toStatus(history.getStatus()))
                .databaseName(resolveDatabaseName(datasourceType))
                .fileSize(history.getFileSize())
                .startTime(history.getStartedAt())
                .endTime(history.getFinishedAt())
                .duration(history.getDurationSeconds() == null ? null : history.getDurationSeconds().longValue())
                .errorMessage(history.getErrorMessage())
                .createTime(history.getStartedAt())
                .build();
    }

    private String resolveDatabaseName(String datasourceType) {
        if ("postgresql".equals(datasourceType)) {
            return backupProperties.getPostgres().getDatabase();
        }
        return backupProperties.getDatabase().getDatabase();
    }

    private String normalizeDatasourceType(String datasourceType) {
        if (datasourceType == null || datasourceType.isBlank()) {
            return "unknown";
        }
        String normalized = datasourceType.trim().toLowerCase(Locale.ROOT);
        return "postgres".equals(normalized) ? "postgresql" : normalized;
    }

    private BackupStatus toStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return BackupStatus.FAILED;
        }
        try {
            return BackupStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BackupStatus.FAILED;
        }
    }
}
