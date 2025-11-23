package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.infrastructure.executor.BackupArtifact;
import com.basebackend.backup.infrastructure.executor.BackupRequest;
import com.basebackend.backup.infrastructure.reliability.Checksum;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 抽象备份执行器
 * 提供通用备份流程模板
 */
@Slf4j
public abstract class AbstractBackupExecutor {

    @Autowired
    protected LockManager lockManager;

    @Autowired
    protected RetryTemplate retryTemplate;

    @Autowired
    protected StorageProvider storageProvider;

    @Autowired
    protected ChecksumService checksumService;

    @Autowired
    protected BackupHistoryMapper backupHistoryMapper;

    /**
     * 安全地记录命令（隐藏敏感信息）
     */
    protected void logCommand(List<String> command) {
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

    /**
     * 执行备份的模板方法
     */
    public BackupHistory execute(BackupRequest request) throws Exception {
        String lockKey = "backup:" + request.getDatasourceType() + ":" + request.getTaskId();

        return retryTemplate.execute(() ->
            lockManager.withLock(lockKey, () -> {
                // 1. 记录备份开始
                BackupHistory history = createHistoryRecord(request);

                try {
                    log.info("开始执行备份任务: {}, 类型: {}", request.getTaskId(), request.getBackupType());

                    // 2. 执行备份
                    BackupArtifact artifact = doBackup(request, history);

                    // 3. 计算校验和
                    Checksum checksum = calculateChecksum(artifact);

                    // 4. 存储备份文件
                    StorageResult storageResult = storeBackup(artifact);

                    // 5. 更新记录
                    updateHistoryRecord(history, artifact, checksum, storageResult);

                    log.info("备份任务执行成功: {}, 文件: {}", request.getTaskId(), artifact.getFile());

                    return history;

                } catch (Exception e) {
                    // 更新失败记录
                    updateFailureRecord(history, e);
                    log.error("备份任务执行失败: {}", request.getTaskId(), e);
                    throw e;
                }
            })
        );
    }

    /**
     * 抽象方法：执行具体备份逻辑
     */
    protected abstract BackupArtifact doBackup(BackupRequest request, BackupHistory history) throws Exception;

    /**
     * 创建备份历史记录
     */
    private BackupHistory createHistoryRecord(BackupRequest request) {
        BackupHistory history = new BackupHistory();
        history.setTaskId(request.getTaskId());
        history.setTaskName("备份任务_" + request.getTaskId());
        history.setStatus("RUNNING");
        history.setBackupType(request.getBackupType());
        history.setStartedAt(LocalDateTime.now());
        history.setStartedAtMs(System.currentTimeMillis());

        backupHistoryMapper.insert(history);

        log.debug("创建备份历史记录: {}", history.getId());
        return history;
    }

    /**
     * 计算校验和
     */
    protected Checksum calculateChecksum(BackupArtifact artifact) throws Exception {
        if (artifact.getFile() == null || !artifact.getFile().exists()) {
            throw new IllegalArgumentException("备份文件不存在");
        }

        Path filePath = artifact.getFile().toPath();
        return checksumService.computeChecksum(filePath);
    }

    /**
     * 存储备份文件
     */
    protected StorageResult storeBackup(BackupArtifact artifact) throws Exception {
        String bucket = "backup";
        String key = generateStorageKey(artifact);

        log.info("开始存储备份文件: {}", key);

        // 构建上传请求（使用try-with-resources避免资源泄漏）
        try (FileInputStream fis = new FileInputStream(artifact.getFile())) {
            UploadRequest uploadRequest = new UploadRequest();
            uploadRequest.setBucket(bucket);
            uploadRequest.setKey(key);
            uploadRequest.setInputStream(fis);
            uploadRequest.setSize(artifact.getFileSize());
            uploadRequest.setContentType("application/octet-stream");

            // 调用存储提供者实际上传文件
            StorageResult storageResult = storageProvider.upload(uploadRequest);

            log.info("备份文件存储完成: {}, 结果: {}", key, storageResult.isSuccess() ? "成功" : "失败");

            return storageResult;
        }
    }

    /**
     * 生成存储键
     */
    private String generateStorageKey(BackupArtifact artifact) {
        String timestamp = LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
        String type = artifact.getBackupType();
        return String.format("%s/%s_%s.sql", type, type, timestamp);
    }

    /**
     * 更新成功记录
     */
    private void updateHistoryRecord(BackupHistory history, BackupArtifact artifact,
                                   Checksum checksum, StorageResult storageResult) {
        history.setStatus("SUCCESS");
        history.setFinishedAt(LocalDateTime.now());
        history.setFinishedAtMs(System.currentTimeMillis());
        history.setDurationSeconds((int) (history.getFinishedAtMs() - history.getStartedAtMs()) / 1000);
        history.setFileSize(artifact.getFileSize());
        history.setChecksumMd5(checksum.getMd5());
        history.setChecksumSha256(checksum.getSha256());
        history.setStorageLocations(storageResult.getLocation());

        backupHistoryMapper.updateById(history);
    }

    /**
     * 更新失败记录
     */
    private void updateFailureRecord(BackupHistory history, Exception e) {
        history.setStatus("FAILED");
        history.setFinishedAt(LocalDateTime.now());
        history.setFinishedAtMs(System.currentTimeMillis());
        history.setErrorMessage(e.getMessage());
        history.setDurationSeconds((int) (history.getFinishedAtMs() - history.getStartedAtMs()) / 1000);

        backupHistoryMapper.updateById(history);
    }
}
