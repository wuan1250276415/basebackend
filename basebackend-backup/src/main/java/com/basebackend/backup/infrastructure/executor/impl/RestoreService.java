package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.entity.BackupTask;
import com.basebackend.backup.domain.entity.RestoreRecord;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.domain.mapper.BackupTaskMapper;
import com.basebackend.backup.domain.mapper.RestoreRecordMapper;
import com.basebackend.backup.infrastructure.executor.*;
import com.basebackend.backup.infrastructure.monitoring.BackupMetricsRegistrar;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * PITR（时间点恢复）服务
 * <p>
 * 负责执行数据库的点到时间恢复操作，支持：
 * <ul>
 *   <li>恢复到指定时间点</li>
 *   <li>恢复到指定备份</li>
 *   <li>增量链恢复</li>
 * </ul>
 *
 * @author BaseBackend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestoreService {

    private final BackupHistoryMapper backupHistoryMapper;
    private final BackupTaskMapper backupTaskMapper;
    private final RestoreRecordMapper restoreRecordMapper;
    private final IncrementalChainManager incrementalChainManager;
    private final MySqlBackupExecutor mysqlBackupExecutor;
    private final PostgresBackupExecutor postgresBackupExecutor;
    private final BackupProperties backupProperties;
    private final StorageProvider storageProvider;
    private final ChecksumService checksumService;
    @Nullable
    private final BackupMetricsRegistrar backupMetricsRegistrar;

    /**
     * 执行PITR恢复
     *
     * @param request 恢复请求
     * @return 恢复是否成功
     */
    public boolean restoreToPoint(RestoreRequest request) throws Exception {
        log.info("开始PITR恢复: 任务ID={}, 目标时间={}, 操作者={}",
            request.getTaskId(), request.getTargetTime(), request.getOperator());
        long startMs = System.currentTimeMillis();
        recordRestoreStart();

        // 1. 创建恢复记录
        RestoreRecord record = createRestoreRecord(request);

        try {
            // 2. 构建增量链
            IncrementalChain chain = incrementalChainManager.buildChainToTime(
                request.getTaskId(), request.getTargetTime());

            if (chain == null || !chain.isValid()) {
                throw new IllegalStateException("无法构建有效的增量链");
            }

            // 3. 验证增量链是否支持恢复到指定时间
            if (!chain.canRestoreTo(request.getTargetTime())) {
                throw new IllegalStateException("增量链不支持恢复到指定时间点: " + request.getTargetTime());
            }

            // 4. 获取应用到目标时间需要的备份列表
            List<BackupHistory> backups = chain.getBackupsToRestore(request.getTargetTime());

            if (backups == null || backups.isEmpty()) {
                throw new IllegalStateException("没有找到可用的备份");
            }

            log.info("找到 {} 个备份用于恢复", backups.size());

            String datasourceType = resolveDatasourceType(request.getTaskId(), request.getDataSourceConfig());

            // 5. 执行恢复
            boolean success = executeRestore(backups, request, datasourceType);

            // 6. 更新恢复记录
            updateRestoreRecord(record, success);
            recordRestoreEnd(success, startMs);

            log.info("PITR恢复完成: {}", success ? "成功" : "失败");

            return success;

        } catch (Exception e) {
            recordRestoreEnd(false, startMs);
            // 更新失败记录
            record.setStatus("FAILED");
            record.setFinishedAt(LocalDateTime.now());
            record.setFinishedAtMs(System.currentTimeMillis());
            record.setErrorMessage(e.getMessage());
            restoreRecordMapper.updateById(record);

            log.error("PITR恢复失败", e);
            throw e;
        }
    }

    /**
     * 恢复到指定备份
     *
     * @param request 恢复请求
     * @return 恢复是否成功
     */
    public boolean restoreToBackup(RestoreRequest request) throws Exception {
        log.info("开始恢复到备份: 备份ID={}, 操作者={}",
            request.getHistoryId(), request.getOperator());
        long startMs = System.currentTimeMillis();
        recordRestoreStart();

        // 1. 创建恢复记录
        RestoreRecord record = createRestoreRecord(request);

        try {
            // 2. 获取指定备份
            BackupHistory backup = backupHistoryMapper.selectById(request.getHistoryId());

            if (backup == null) {
                throw new IllegalArgumentException("备份记录不存在: " + request.getHistoryId());
            }

            if (!backup.isSuccess()) {
                throw new IllegalArgumentException("备份已失败，无法恢复: " + request.getHistoryId());
            }

            // 3. 从存储加载真实备份文件，构建artifact
            BackupArtifact artifact = buildArtifactForRestore(backup);

            // 4. 执行恢复
            boolean success = false;
            Long taskId = request.getTaskId() != null ? request.getTaskId() : backup.getTaskId();
            String datasourceType = resolveDatasourceType(taskId, request.getDataSourceConfig());
            if ("postgresql".equals(datasourceType) && backup.isIncremental() && !isPostgresIncrementalReplayEnabled()) {
                throw new UnsupportedOperationException("PostgreSQL增量回放尚未实现，当前仅支持全量备份恢复");
            }

            try {
                switch (datasourceType) {
                    case "mysql":
                        success = mysqlBackupExecutor.restore(artifact, request.getTargetDatabase());
                        break;
                    case "postgresql":
                        success = postgresBackupExecutor.restore(artifact, request.getTargetDatabase());
                        break;
                    default:
                        throw new UnsupportedOperationException("不支持的数据源类型: " + datasourceType);
                }
            } finally {
                cleanupTempArtifact(artifact);
            }

            // 5. 更新恢复记录
            updateRestoreRecord(record, success);
            recordRestoreEnd(success, startMs);

            log.info("恢复到备份完成: {}", success ? "成功" : "失败");

            return success;

        } catch (Exception e) {
            recordRestoreEnd(false, startMs);
            // 更新失败记录
            record.setStatus("FAILED");
            record.setFinishedAt(LocalDateTime.now());
            record.setFinishedAtMs(System.currentTimeMillis());
            record.setErrorMessage(e.getMessage());
            restoreRecordMapper.updateById(record);

            log.error("恢复到备份失败", e);
            throw e;
        }
    }

    /**
     * 执行具体恢复操作
     */
    private boolean executeRestore(List<BackupHistory> backups, RestoreRequest request,
                                   String datasourceType) throws Exception {
        log.info("开始执行恢复操作，共 {} 个备份", backups.size());

        List<BackupHistory> orderedBackups = new ArrayList<>(backups);
        orderedBackups.sort(Comparator.comparing(BackupHistory::getStartedAt));

        if ("postgresql".equals(datasourceType)
                && orderedBackups.stream().anyMatch(BackupHistory::isIncremental)
                && !isPostgresIncrementalReplayEnabled()) {
            throw new UnsupportedOperationException("PostgreSQL增量回放尚未实现，当前仅支持恢复到全量备份时点");
        }

        // 按时间顺序应用备份（全量 -> 增量）
        for (int i = 0; i < orderedBackups.size(); i++) {
            BackupHistory backup = orderedBackups.get(i);

            log.info("应用第 {} 个备份: {}, 类型={}, 时间={}",
                i + 1, backup.getId(), backup.getBackupType(), backup.getStartedAt());

            BackupArtifact artifact = buildArtifactForRestore(backup);
            boolean success;
            try {
                switch (datasourceType) {
                    case "mysql":
                        success = mysqlBackupExecutor.restore(artifact, request.getTargetDatabase());
                        break;
                    case "postgresql":
                        success = postgresBackupExecutor.restore(artifact, request.getTargetDatabase());
                        break;
                    default:
                        throw new UnsupportedOperationException("不支持的数据源类型: " + datasourceType);
                }
            } finally {
                cleanupTempArtifact(artifact);
            }

            if (!success) {
                throw new IllegalStateException("应用备份失败: historyId=" + backup.getId());
            }
        }

        log.info("所有备份应用完成");
        return true;
    }

    /**
     * 基于备份记录构建恢复产物
     */
    private BackupArtifact buildArtifactForRestore(BackupHistory backup) {
        if (backup.getStorageLocations() == null || backup.getStorageLocations().isBlank()) {
            throw new IllegalStateException("备份记录缺少存储位置信息: historyId=" + backup.getId());
        }

        List<StorageResult> locations = JsonUtils.parseObject(
            backup.getStorageLocations(), new TypeReference<List<StorageResult>>() {});
        if (locations == null || locations.isEmpty()) {
            throw new IllegalStateException("无法解析备份存储位置: historyId=" + backup.getId());
        }

        StorageResult selected = selectAvailableLocation(locations, backup.getId());
        Path tempFile = downloadToTempFile(selected, backup);
        verifyDownloadedBackup(tempFile, backup);

        return BackupArtifact.builder()
            .file(tempFile.toFile())
            .backupType(backup.getBackupType())
            .fileSize(Files.exists(tempFile) ? tempFile.toFile().length() : backup.getFileSize())
            .binlogStartPosition(backup.getBinlogStart())
            .binlogEndPosition(backup.getBinlogEnd())
            .walStartPosition(backup.getWalStart())
            .walEndPosition(backup.getWalEnd())
            .build();
    }

    private StorageResult selectAvailableLocation(List<StorageResult> locations, Long historyId) {
        for (StorageResult location : locations) {
            String bucket = location.getBucket();
            String key = location.getKey();
            String path = location.getLocation();

            // 优先使用 bucket/key 走统一存储抽象
            if (bucket != null && !bucket.isBlank() && key != null && !key.isBlank()) {
                try {
                    if (storageProvider.exists(bucket, key)) {
                        return location;
                    }
                } catch (Exception ex) {
                    log.warn("检测存储位置可用性失败，尝试下一个位置: historyId={}, bucket={}, key={}",
                        historyId, bucket, key, ex);
                }
            }

            // 回退：直接使用本地路径（适配历史数据）
            if (path != null && !path.isBlank() && Files.exists(Path.of(path))) {
                return location;
            }
        }

        throw new IllegalStateException("没有可用的备份存储位置: historyId=" + historyId);
    }

    private Path downloadToTempFile(StorageResult location, BackupHistory backup) {
        String suffix = resolveSuffix(location, backup);
        try {
            Path tempFile = Files.createTempFile("restore-" + backup.getId() + "-", suffix);

            if (location.getBucket() != null && !location.getBucket().isBlank()
                && location.getKey() != null && !location.getKey().isBlank()) {
                try (InputStream inputStream = storageProvider.download(location.getBucket(), location.getKey())) {
                    Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (location.getLocation() != null && !location.getLocation().isBlank()) {
                try (InputStream inputStream = new FileInputStream(location.getLocation())) {
                    Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                throw new IllegalStateException("存储位置信息不完整: historyId=" + backup.getId());
            }

            return tempFile;
        } catch (Exception e) {
            throw new IllegalStateException("下载备份文件失败: historyId=" + backup.getId(), e);
        }
    }

    private void verifyDownloadedBackup(Path tempFile, BackupHistory backup) {
        String expectedMd5 = backup.getChecksumMd5();
        String expectedSha256 = backup.getChecksumSha256();
        if ((expectedMd5 == null || expectedMd5.isBlank())
            && (expectedSha256 == null || expectedSha256.isBlank())) {
            return;
        }
        try {
            boolean ok = checksumService.verifyChecksum(tempFile, expectedMd5, expectedSha256);
            if (!ok) {
                throw new IllegalStateException("下载备份文件校验失败: historyId=" + backup.getId());
            }
        } catch (Exception e) {
            throw new IllegalStateException("下载备份文件校验异常: historyId=" + backup.getId(), e);
        }
    }

    private String resolveSuffix(StorageResult location, BackupHistory backup) {
        String key = location.getKey();
        String path = location.getLocation();
        String candidate = key != null ? key : path;
        if (candidate != null) {
            int idx = candidate.lastIndexOf('.');
            if (idx >= 0 && idx < candidate.length() - 1) {
                return candidate.substring(idx);
            }
        }
        return backup.isIncremental() ? ".incremental.sql" : ".full.sql";
    }

    private void cleanupTempArtifact(BackupArtifact artifact) {
        if (artifact == null || artifact.getFile() == null) {
            return;
        }
        try {
            Files.deleteIfExists(artifact.getFile().toPath());
        } catch (Exception e) {
            log.warn("清理恢复临时文件失败: {}", artifact.getFile().getAbsolutePath(), e);
        }
    }

    /**
     * 创建恢复记录
     */
    private RestoreRecord createRestoreRecord(RestoreRequest request) {
        RestoreRecord record = new RestoreRecord();
        record.setTaskId(request.getTaskId());
        record.setHistoryId(request.getHistoryId());
        record.setTargetPoint(request.getTargetTime() != null ?
            request.getTargetTime().toString() : null);
        record.setStatus("RUNNING");
        record.setStartedAt(LocalDateTime.now());
        record.setStartedAtMs(System.currentTimeMillis());
        record.setOperator(request.getOperator());
        record.setRemark(request.getRemark());

        restoreRecordMapper.insert(record);

        log.debug("创建恢复记录: {}", record.getId());
        return record;
    }

    /**
     * 更新恢复记录
     */
    private void updateRestoreRecord(RestoreRecord record, boolean success) {
        record.setStatus(success ? "SUCCESS" : "FAILED");
        record.setFinishedAt(LocalDateTime.now());
        record.setFinishedAtMs(System.currentTimeMillis());
        record.setDurationSeconds((int) (record.getFinishedAtMs() - record.getStartedAtMs()) / 1000);

        restoreRecordMapper.updateById(record);
    }

    /**
     * 获取数据源类型
     */
    private String resolveDatasourceType(Long taskId, RestoreRequest.DataSourceConfig dataSourceConfig) {
        if (dataSourceConfig != null) {
            String inlineType = normalizeDatasourceType(dataSourceConfig.getDatasourceType());
            if (inlineType != null) {
                return inlineType;
            }
        }

        if (taskId == null) {
            throw new IllegalStateException("无法确定数据源类型: taskId为空且请求中未提供datasourceType");
        }

        BackupTask task = backupTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("无法确定数据源类型: 任务不存在, taskId=" + taskId);
        }

        String taskType = normalizeDatasourceType(task.getDatasourceType());
        if (taskType == null) {
            throw new IllegalStateException("无法确定数据源类型: taskId=" + taskId + ", datasourceType为空");
        }
        return taskType;
    }

    private String normalizeDatasourceType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        if ("postgres".equals(normalized)) {
            return "postgresql";
        }
        return normalized;
    }

    private void recordRestoreStart() {
        if (backupMetricsRegistrar != null) {
            backupMetricsRegistrar.recordRestoreStart();
        }
    }

    private void recordRestoreEnd(boolean success, long startMs) {
        if (backupMetricsRegistrar == null) {
            return;
        }
        long durationMs = Math.max(System.currentTimeMillis() - startMs, 0);
        if (success) {
            backupMetricsRegistrar.recordRestoreSuccess(durationMs);
        } else {
            backupMetricsRegistrar.recordRestoreFailure(durationMs);
        }
    }

    private boolean isPostgresIncrementalReplayEnabled() {
        return backupProperties.getPostgres().isIncrementalReplayEnabled();
    }

    /**
     * 获取任务最近的PITR恢复记录
     */
    public RestoreRecord getLatestPITR(Long taskId) {
        return restoreRecordMapper.selectLatestPITR(taskId);
    }

    /**
     * 获取恢复统计信息
     */
    public RestoreStatistics getRestoreStatistics(Long taskId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        int total = restoreRecordMapper.countByTimeRangeAndStatus(taskId, startDate, LocalDateTime.now(), null);
        int success = restoreRecordMapper.countByTimeRangeAndStatus(taskId, startDate, LocalDateTime.now(), "SUCCESS");
        int failed = restoreRecordMapper.countByTimeRangeAndStatus(taskId, startDate, LocalDateTime.now(), "FAILED");

        return RestoreStatistics.builder()
            .taskId(taskId)
            .totalCount(total)
            .successCount(success)
            .failedCount(failed)
            .successRate(total > 0 ? (double) success / total * 100 : 0)
            .periodDays(days)
            .build();
    }
}
