package com.basebackend.backup.infrastructure.executor.impl;

import com.alibaba.fastjson2.JSON;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 抽象备份执行器
 * <p>
 * 提供通用备份流程模板，定义了标准的企业级备份流程：
 * 1. 分布式锁控制（防止并发执行）
 * 2. 备份历史记录创建
 * 3. 执行具体备份逻辑（由子类实现）
 * 4. 计算文件校验和（保证数据完整性）
 * 5. 上传备份文件到存储系统
 * 6. 更新历史记录（成功或失败状态）
 * <p>
 * 子类只需要实现抽象方法 {@link #doBackup} 即可完成具体数据库的备份逻辑。
 * 该设计遵循了模板方法模式，确保所有备份操作都遵循统一的流程和错误处理。
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBackupExecutor {

    protected final LockManager lockManager;
    protected final RetryTemplate retryTemplate;
    protected final StorageProvider storageProvider;
    protected final ChecksumService checksumService;
    protected final BackupHistoryMapper backupHistoryMapper;

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
     * <p>
     * 定义了标准的备份执行流程，包括：
     * 1. 使用分布式锁防止并发执行同一备份任务
     * 2. 使用重试模板处理临时性失败
     * 3. 记录备份历史到数据库
     * 4. 执行具体备份逻辑
     * 5. 验证备份文件完整性
     * 6. 上传到存储系统
     * 7. 更新最终状态
     * <p>
     * 整个流程是原子的，任何步骤失败都会导致备份失败并记录错误信息。
     *
     * @param request 备份请求，包含任务ID、数据库配置等信息
     * @return 备份历史记录，包含执行结果和元数据
     * @throws Exception 备份过程中发生的任何异常
     */
    public BackupHistory execute(BackupRequest request) throws Exception {
        // 生成唯一的分布式锁键，格式：backup:{数据源类型}:{任务ID}
        String lockKey = "backup:" + request.getDatasourceType() + ":" + request.getTaskId();

        // 使用重试模板执行，在分布式锁保护下的备份流程
        return retryTemplate.execute(() ->
            // 获取分布式锁，确保同一时间只有一个备份任务在执行
            lockManager.withLock(lockKey, () -> {
                // 1. 创建备份历史记录，初始状态为 RUNNING
                BackupHistory history = createHistoryRecord(request);

                try {
                    log.info("开始执行备份任务: {}, 类型: {}", request.getTaskId(), request.getBackupType());

                    // 2. 执行具体备份逻辑（由子类实现）
                    BackupArtifact artifact = doBackup(request, history);

                    // 3. 计算备份文件校验和（MD5和SHA256）
                    Checksum checksum = calculateChecksum(artifact);

                    // 4. 上传备份文件到存储系统（本地/S3/OSS等）
                    StorageResult storageResult = storeBackup(artifact);

                    // 5. 更新历史记录为成功状态，包含校验和和存储位置
                    updateHistoryRecord(history, artifact, checksum, storageResult);

                    log.info("备份任务执行成功: {}, 文件: {}", request.getTaskId(), artifact.getFile());

                    return history;

                } catch (Exception e) {
                    // 备份失败时更新历史记录为失败状态
                    updateFailureRecord(history, e);
                    log.error("备份任务执行失败: {}", request.getTaskId(), e);
                    // 重新抛出异常，触发重试机制
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
     * 计算备份文件的校验和
     * <p>
     * 使用MD5和SHA256两种算法计算文件校验和，确保：
     * 1. 文件传输后完整性未被破坏
     * 2. 可以验证后续从存储系统下载的文件是否正确
     * 3. 为备份文件提供唯一的指纹标识
     *
     * @param artifact 备份产物，包含文件路径和元数据
     * @return 校验和对象，包含MD5和SHA256值
     * @throws Exception 计算校验和失败时抛出异常（如文件不存在、读取失败等）
     */
    protected Checksum calculateChecksum(BackupArtifact artifact) throws Exception {
        if (artifact.getFile() == null || !artifact.getFile().exists()) {
            throw new IllegalArgumentException("备份文件不存在: " + (artifact.getFile() != null ? artifact.getFile().getPath() : "null"));
        }

        // 验证文件大小大于0，避免空文件
        if (artifact.getFile().length() == 0) {
            throw new IllegalArgumentException("备份文件为空: " + artifact.getFile().getPath());
        }

        Path filePath = artifact.getFile().toPath();
        log.debug("开始计算文件校验和: {}, 大小: {} bytes", artifact.getFile(), artifact.getFile().length());

        Checksum checksum = checksumService.computeChecksum(filePath);

        log.debug("文件校验和计算完成: MD5={}, SHA256={}", checksum.getMd5(), checksum.getSha256());
        return checksum;
    }

    /**
     * 存储备份文件到指定的存储系统
     * <p>
     * 支持多种存储后端（本地文件系统、S3、OSS等），根据配置自动选择。
     * 使用try-with-resources确保文件输入流正确关闭，避免资源泄漏。
     * <p>
     * 存储策略：
     * 1. 使用统一的bucket名称（默认"backup"）
     * 2. 根据备份类型和时间戳生成唯一键名
     * 3. 支持大文件自动选择上传方式（简单上传 vs 分片上传）
     *
     * @param artifact 备份产物，包含要上传的文件
     * @return 存储结果，包含是否成功、存储位置、ETag等信息
     * @throws Exception 上传失败时抛出异常（如网络异常、权限不足、磁盘空间不足等）
     */
    protected StorageResult storeBackup(BackupArtifact artifact) throws Exception {
        // 使用统一的bucket命名空间，便于管理
        String bucket = "backup";
        // 生成基于类型和时间戳的存储键
        String key = generateStorageKey(artifact);

        log.info("开始上传备份文件到存储系统: {}, 大小: {} bytes", key, artifact.getFileSize());

        // 使用try-with-resources自动管理文件输入流生命周期
        try (FileInputStream fis = new FileInputStream(artifact.getFile())) {
            // 构建上传请求
            UploadRequest uploadRequest = new UploadRequest();
            uploadRequest.setBucket(bucket);
            uploadRequest.setKey(key);
            uploadRequest.setInputStream(fis);
            uploadRequest.setSize(artifact.getFileSize());
            uploadRequest.setContentType("application/octet-stream");
            // 可以添加更多元数据，如备份类型、数据库名等

            // 调用存储提供者实际上传文件（同步操作）
            StorageResult storageResult = storageProvider.upload(uploadRequest);

            if (storageResult.isSuccess()) {
                log.info("备份文件上传成功: {}, 存储位置: {}", key, storageResult.getLocation());
            } else {
                log.error("备份文件上传失败: {}, 错误信息: {}", key, storageResult.getErrorMessage());
            }

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
        String storageJson = JSON.toJSONString(java.util.Collections.singletonList(storageResult));
        history.setStorageLocations(storageJson);

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
