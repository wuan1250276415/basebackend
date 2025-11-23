package com.basebackend.backup.infrastructure.executor;

import com.basebackend.backup.domain.entity.BackupHistory;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 增量链对象
 * 表示一个完整的增量备份链（包含基线全量备份和所有增量备份）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncrementalChain {

    /**
     * 链ID
     */
    private String chainId;

    /**
     * 基线全量备份
     */
    private BackupHistory fullBackup;

    /**
     * 增量备份列表（按时间排序）
     */
    private List<BackupHistory> incrementalBackups;

    /**
     * 链创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 链状态：ACTIVE（活跃）、COMPLETE（完整）、BROKEN（断裂）
     */
    private ChainStatus status;

    /**
     * 增量备份总数
     */
    private int incrementalCount;

    /**
     * 链的总大小（字节）
     */
    private Long totalSize;

    /**
     * 链中最早的增量备份时间
     */
    private LocalDateTime earliestIncrementalTime;

    /**
     * 链中最新的增量备份时间
     */
    private LocalDateTime latestIncrementalTime;

    /**
     * 是否断裂（存在缺失的增量备份）
     */
    private boolean isBroken;

    /**
     * 链状态枚举
     */
    public enum ChainStatus {
        ACTIVE,    // 活跃链（正在接收新的增量备份）
        COMPLETE,  // 完整链（所有增量备份都存在）
        BROKEN,    // 断裂链（有缺失的增量备份）
        EXPIRED    // 已过期
    }

    /**
     * 检查链是否有效
     */
    public boolean isValid() {
        return fullBackup != null &&
               fullBackup.isSuccess() &&
               !isBroken &&
               status != ChainStatus.EXPIRED;
    }

    /**
     * 检查是否可以应用到指定时间点
     */
    public boolean canRestoreTo(LocalDateTime targetTime) {
        if (!isValid()) {
            return false;
        }

        // 全量备份必须早于目标时间
        if (fullBackup.getStartedAt().isAfter(targetTime)) {
            return false;
        }

        // 最近的增量备份不能早于目标时间
        if (latestIncrementalTime != null && latestIncrementalTime.isBefore(targetTime)) {
            return false;
        }

        return true;
    }

    /**
     * 获取应用到目标时间需要的备份列表
     */
    public List<BackupHistory> getBackupsToRestore(LocalDateTime targetTime) {
        if (!canRestoreTo(targetTime)) {
            return null;
        }

        List<BackupHistory> backups = new java.util.ArrayList<>();
        backups.add(fullBackup);

        for (BackupHistory incremental : incrementalBackups) {
            if (!incremental.getStartedAt().isAfter(targetTime)) {
                backups.add(incremental);
            } else {
                break;
            }
        }

        return backups;
    }

    /**
     * 检查链中是否存在缺失的增量备份
     */
    public void checkChainIntegrity() {
        if (incrementalBackups == null || incrementalBackups.isEmpty()) {
            isBroken = false;
            status = ChainStatus.ACTIVE;
            return;
        }

        // 检查增量备份之间的时间连续性
        LocalDateTime lastTime = fullBackup.getStartedAt();
        isBroken = false;

        for (BackupHistory incremental : incrementalBackups) {
            // 这里可以检查binlog位置的连续性
            if (incremental.getStartedAt().isBefore(lastTime)) {
                isBroken = true;
                break;
            }
            lastTime = incremental.getStartedAt();
        }

        // 更新状态
        if (isBroken) {
            status = ChainStatus.BROKEN;
        } else {
            status = ChainStatus.COMPLETE;
        }
    }

    /**
     * 获取链中最后一个增量备份
     */
    public BackupHistory getLastIncrementalBackup() {
        if (incrementalBackups == null || incrementalBackups.isEmpty()) {
            return null;
        }
        return incrementalBackups.get(incrementalBackups.size() - 1);
    }

    /**
     * 计算链的总大小
     */
    public void calculateTotalSize() {
        long total = 0;
        if (fullBackup != null && fullBackup.getFileSize() != null) {
            total += fullBackup.getFileSize();
        }

        if (incrementalBackups != null) {
            for (BackupHistory incremental : incrementalBackups) {
                if (incremental.getFileSize() != null) {
                    total += incremental.getFileSize();
                }
            }
        }

        this.totalSize = total;
    }
}
