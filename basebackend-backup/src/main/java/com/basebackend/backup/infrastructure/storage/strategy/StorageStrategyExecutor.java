package com.basebackend.backup.infrastructure.storage.strategy;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 存储策略执行器
 * 支持单副本和多副本存储策略
 */
@Slf4j
@Component
public class StorageStrategyExecutor {

    private final BackupProperties backupProperties;
    private final LockManager lockManager;
    private final RetryTemplate retryTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<String, StorageProvider> providerMap;

    public StorageStrategyExecutor(BackupProperties backupProperties,
                                   LockManager lockManager,
                                   RetryTemplate retryTemplate,
                                   java.util.List<StorageProvider> storageProviders) {
        this.backupProperties = backupProperties;
        this.lockManager = lockManager;
        this.retryTemplate = retryTemplate;
        this.providerMap = storageProviders.stream()
            .collect(Collectors.toMap(p -> p.getStorageType().toLowerCase(), Function.identity(), (a, b) -> a));
    }

    /**
     * 执行存储策略
     *
     * @param file 备份文件
     * @param bucket 存储桶名称
     * @param key 对象键名
     * @return 存储结果列表
     * @throws Exception 存储失败时抛出异常
     */
    public List<StorageResult> execute(File file, String bucket, String key) throws Exception {
        boolean multiReplicaEnabled = backupProperties.getStorage().getMultiReplica().isEnabled();

        if (multiReplicaEnabled) {
            return executeMultiReplica(file, bucket, key);
        } else {
            return executeSingleReplica(file, bucket, key);
        }
    }

    /**
     * 单副本存储
     * 根据配置选择本地存储或S3存储
     */
    private List<StorageResult> executeSingleReplica(File file, String bucket, String key) throws Exception {
        BackupProperties.Storage storageConfig = backupProperties.getStorage();

        // 根据启用状态选择存储类型
        StorageProvider storageProvider = chooseStorageProvider(storageConfig);

        log.info("执行单副本存储: provider={}, bucket={}, key={}",
            storageProvider.getStorageType(), bucket, key);

        StorageResult result = uploadWithRetry(storageProvider, file, bucket, key);

        List<StorageResult> results = new ArrayList<>();
        results.add(result);

        return results;
    }

    /**
     * 多副本存储
     * 同时存储到多个位置
     */
    private List<StorageResult> executeMultiReplica(File file, String bucket, String key) throws Exception {
        BackupProperties.Storage.MultiReplica multiReplicaConfig =
            backupProperties.getStorage().getMultiReplica();

        log.info("执行多副本存储: 总数={}, bucket={}, key={}",
            multiReplicaConfig.getReplicas().size(), bucket, key);

        List<CompletableFuture<StorageResult>> futures = new ArrayList<>();
        List<StorageResult> results = new ArrayList<>();

        // 按优先级顺序执行副本存储
        for (BackupProperties.Storage.MultiReplica.Replica replica :
             multiReplicaConfig.getReplicas()) {

            if (!replica.isEnabled()) {
                log.debug("跳过禁用的副本: {}", replica.getType());
                continue;
            }

            CompletableFuture<StorageResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    StorageProvider storageProvider = createStorageProvider(replica.getType());
                    return uploadWithRetry(storageProvider, file, bucket, key);
                } catch (Exception e) {
                    log.error("副本存储失败: type={}", replica.getType(), e);
                    return null;
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有副本完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        allFutures.get();

        // 收集结果
        for (CompletableFuture<StorageResult> future : futures) {
            try {
                StorageResult result = future.get();
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("获取副本结果失败", e);
            }
        }

        log.info("多副本存储完成: 成功={}/{}, bucket={}, key={}",
            results.size(), futures.size(), bucket, key);

        // 验证至少有一个副本成功
        if (results.isEmpty()) {
            throw new RuntimeException("所有副本存储均失败");
        }

        return results;
    }

    /**
     * 使用重试机制上传文件
     */
    private StorageResult uploadWithRetry(StorageProvider storageProvider, File file,
                                         String bucket, String key) throws Exception {
        String lockKey = "storage:" + storageProvider.getStorageType() + ":" + bucket + ":" + key;

        return retryTemplate.execute(() ->
            lockManager.withLock(lockKey, () -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    UploadRequest request = new UploadRequest();
                    request.setBucket(bucket);
                    request.setKey(key);
                    request.setInputStream(fis);
                    request.setSize(file.length());
                    request.setContentType("application/octet-stream");

                    return storageProvider.upload(request);
                }
            })
        );
    }

    /**
     * 根据配置选择存储提供者
     */
    private StorageProvider chooseStorageProvider(BackupProperties.Storage config) {
        if (config.getS3().isEnabled()) {
            return createStorageProvider("s3");
        }
        return createStorageProvider("local");
    }

    /**
     * 创建指定类型的存储提供者
     */
    private StorageProvider createStorageProvider(String type) {
        StorageProvider provider = providerMap.get(type.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("不支持的存储类型: " + type);
        }
        return provider;
    }

    /**
     * 验证多副本存储结果
     *
     * @param results 存储结果列表
     * @return 验证是否通过（至少50%副本成功）
     */
    public boolean validateMultiReplica(List<StorageResult> results) {
        if (results.isEmpty()) {
            log.warn("没有可验证的副本结果");
            return false;
        }

        // 检查至少50%的副本成功
        int successCount = (int) results.stream()
            .filter(StorageResult::isSuccess)
            .count();

        double successRate = (double) successCount / results.size();

        boolean isValid = successRate >= 0.5;
        log.info("多副本验证结果: 成功={}/{} ({}%), 验证={}",
            successCount, results.size(),
            String.format("%.1f", successRate * 100),
            isValid ? "通过" : "失败");

        return isValid;
    }

    /**
     * 获取多副本存储统计信息
     */
    public MultiReplicaStats getStats(List<StorageResult> results) {
        int total = results.size();
        int success = (int) results.stream().filter(StorageResult::isSuccess).count();
        int failed = total - success;

        long totalSize = results.stream()
            .mapToLong(StorageResult::getSize)
            .sum();

        return MultiReplicaStats.builder()
            .totalReplicas(total)
            .successReplicas(success)
            .failedReplicas(failed)
            .successRate(total > 0 ? (double) success / total : 0)
            .totalSize(totalSize)
            .build();
    }

    /**
     * 清理过期副本
     *
     * @param results 存储结果列表
     * @param retentionDays 保留天数
     */
    public void cleanupExpiredReplicas(List<StorageResult> results, int retentionDays) {
        log.info("开始清理过期副本, 保留天数={}", retentionDays);

        results.forEach(result -> {
            if (result.isSuccess()) {
                try {
                    // 这里可以实现基于时间的清理逻辑
                    // 暂时跳过，因为S3等云存储通常通过生命周期策略处理
                    log.debug("跳过副本清理（建议使用云存储生命周期策略）: {}",
                        result.getLocation());
                } catch (Exception e) {
                    log.error("清理副本失败", e);
                }
            }
        });
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("存储策略执行器已关闭");
        }
    }

    /**
     * 多副本存储统计信息
     */
    public static class MultiReplicaStats {
        private int totalReplicas;
        private int successReplicas;
        private int failedReplicas;
        private double successRate;
        private long totalSize;

        // Getters and builder
        public int getTotalReplicas() {
            return totalReplicas;
        }

        public void setTotalReplicas(int totalReplicas) {
            this.totalReplicas = totalReplicas;
        }

        public int getSuccessReplicas() {
            return successReplicas;
        }

        public void setSuccessReplicas(int successReplicas) {
            this.successReplicas = successReplicas;
        }

        public int getFailedReplicas() {
            return failedReplicas;
        }

        public void setFailedReplicas(int failedReplicas) {
            this.failedReplicas = failedReplicas;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private MultiReplicaStats stats = new MultiReplicaStats();

            public Builder totalReplicas(int totalReplicas) {
                stats.setTotalReplicas(totalReplicas);
                return this;
            }

            public Builder successReplicas(int successReplicas) {
                stats.setSuccessReplicas(successReplicas);
                return this;
            }

            public Builder failedReplicas(int failedReplicas) {
                stats.setFailedReplicas(failedReplicas);
                return this;
            }

            public Builder successRate(double successRate) {
                stats.setSuccessRate(successRate);
                return this;
            }

            public Builder totalSize(long totalSize) {
                stats.setTotalSize(totalSize);
                return this;
            }

            public MultiReplicaStats build() {
                return stats;
            }
        }
    }
}
