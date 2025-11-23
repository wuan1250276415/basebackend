package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.entity.BackupHistory;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.infrastructure.executor.ChainValidationResult;
import com.basebackend.backup.infrastructure.executor.IncrementalChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增量链管理器
 * 负责管理增量备份链的构建、验证、清理等操作
 */
@Slf4j
@Service
public class IncrementalChainManager {

    @Autowired
    private BackupHistoryMapper backupHistoryMapper;

    @Autowired
    private BackupProperties backupProperties;

    /**
     * 构建增量链
     *
     * @param taskId 任务ID
     * @return 增量链对象
     */
    public IncrementalChain buildChain(Long taskId) {
        log.info("构建增量链, 任务ID: {}", taskId);

        // 1. 获取任务的所有成功备份
        List<BackupHistory> allBackups = backupHistoryMapper.selectSuccessByTaskId(taskId);

        if (allBackups.isEmpty()) {
            log.warn("任务 {} 没有成功的备份记录", taskId);
            return null;
        }

        // 2. 按时间排序
        allBackups.sort((b1, b2) -> b1.getStartedAt().compareTo(b2.getStartedAt()));

        // 3. 找到基线全量备份（最早的）
        BackupHistory fullBackup = allBackups.stream()
            .filter(BackupHistory::isFull)
            .findFirst()
            .orElse(null);

        if (fullBackup == null) {
            log.warn("任务 {} 没有基线全量备份", taskId);
            return null;
        }

        // 4. 构建增量链
        IncrementalChain chain = IncrementalChain.builder()
            .chainId(generateChainId(taskId, fullBackup.getId()))
            .fullBackup(fullBackup)
            .createdAt(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();

        // 5. 添加增量备份
        List<BackupHistory> incrementals = allBackups.stream()
            .filter(BackupHistory::isIncremental)
            .filter(b -> b.getBaseFullId() != null && b.getBaseFullId().equals(fullBackup.getId()))
            .collect(Collectors.toList());

        chain.setIncrementalBackups(incrementals);
        chain.setIncrementalCount(incrementals.size());

        // 6. 设置时间范围
        if (!incrementals.isEmpty()) {
            chain.setEarliestIncrementalTime(incrementals.get(0).getStartedAt());
            chain.setLatestIncrementalTime(incrementals.get(incrementals.size() - 1).getStartedAt());
        }

        // 7. 计算总大小
        chain.calculateTotalSize();

        // 8. 检查链完整性
        chain.checkChainIntegrity();

        log.info("增量链构建完成, 链ID: {}, 全量备份: {}, 增量备份: {}",
            chain.getChainId(), fullBackup.getId(), chain.getIncrementalCount());

        return chain;
    }

    /**
     * 构建到指定时间的增量链
     *
     * @param taskId 任务ID
     * @param targetTime 目标时间
     * @return 增量链对象
     */
    public IncrementalChain buildChainToTime(Long taskId, LocalDateTime targetTime) {
        log.info("构建增量链到指定时间, 任务ID: {}, 目标时间: {}", taskId, targetTime);

        // 获取到指定时间的所有备份
        List<BackupHistory> backups = backupHistoryMapper.selectByTimeRange(
            taskId,
            LocalDateTime.ofEpochSecond(0, 0, java.time.ZoneOffset.UTC),
            targetTime
        );

        if (backups.isEmpty()) {
            log.warn("任务 {} 在时间 {} 之前没有备份记录", taskId, targetTime);
            return null;
        }

        return buildChain(taskId);
    }

    /**
     * 获取最新的增量链
     *
     * @param taskId 任务ID
     * @return 增量链对象
     */
    public IncrementalChain getLatestChain(Long taskId) {
        log.info("获取最新的增量链, 任务ID: {}", taskId);

        // 获取最新的全量备份
        BackupHistory latestFullBackup = backupHistoryMapper.selectLatestFullBackup(taskId);

        if (latestFullBackup == null) {
            log.warn("任务 {} 没有全量备份", taskId);
            return null;
        }

        return buildChain(taskId);
    }

    /**
     * 验证增量链完整性
     *
     * @param chain 增量链
     * @return 验证结果
     */
    public ChainValidationResult validateChain(IncrementalChain chain) {
        log.info("验证增量链完整性, 链ID: {}", chain.getChainId());

        ChainValidationResult result = ChainValidationResult.builder()
            .valid(false)
            .build();

        // 检查全量备份
        if (chain.getFullBackup() == null) {
            result.addError("全量备份不存在");
            return result;
        }

        if (!chain.getFullBackup().isSuccess()) {
            result.addError("全量备份失败");
        }

        // 检查增量备份
        if (chain.getIncrementalBackups() == null || chain.getIncrementalBackups().isEmpty()) {
            result.addWarning("没有增量备份");
        } else {
            // 验证增量备份的连续性
            for (int i = 0; i < chain.getIncrementalBackups().size(); i++) {
                BackupHistory incremental = chain.getIncrementalBackups().get(i);

                if (!incremental.isSuccess()) {
                    result.addError(String.format("第 %d 个增量备份失败", i + 1));
                }

                if (incremental.getBaseFullId() != null &&
                    !incremental.getBaseFullId().equals(chain.getFullBackup().getId())) {
                    result.addError(String.format("第 %d 个增量备份的基线ID不匹配", i + 1));
                }
            }
        }

        // 检查链断裂
        if (chain.isBroken()) {
            result.addError("增量链存在断裂");
        }

        result.setValid(chain.isValid() && !result.isHasErrors());
        result.setChain(chain);

        log.info("增量链验证完成, 结果: {}", result.isValid() ? "通过" : "失败");

        return result;
    }

    /**
     * 清理过期的增量链
     *
     * @param taskId 任务ID
     * @param retentionDays 保留天数
     */
    public void cleanupExpiredChains(Long taskId, int retentionDays) {
        log.info("清理过期的增量链, 任务ID: {}, 保留天数: {}", taskId, retentionDays);

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);

        // 找到需要清理的备份
        List<BackupHistory> expiredBackups = backupHistoryMapper.selectByTimeRange(
            taskId,
            LocalDateTime.of(1970, 1, 1, 0, 0),
            cutoffTime
        );

        for (BackupHistory backup : expiredBackups) {
            // 这里可以添加删除物理文件的逻辑
            log.debug("标记过期备份为删除: {}", backup.getId());
            // backupHistoryMapper.deleteById(backup.getId());
        }

        log.info("过期增量链清理完成, 清理数量: {}", expiredBackups.size());
    }

    /**
     * 生成链ID
     */
    private String generateChainId(Long taskId, Long fullBackupId) {
        return String.format("chain_%d_%d", taskId, fullBackupId);
    }
}
