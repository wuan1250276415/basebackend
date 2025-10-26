package com.basebackend.observability.cleanup;

import com.basebackend.observability.mapper.JvmMetricsMapper;
import com.basebackend.observability.mapper.SlowSqlRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 数据清理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final JvmMetricsMapper jvmMetricsMapper;
    private final SlowSqlRecordMapper slowSqlRecordMapper;

    /**
     * 清理过期的JVM指标数据
     * 默认保留7天
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void cleanupJvmMetrics() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            int deleted = jvmMetricsMapper.deleteExpired(cutoffTime);
            
            log.info("Cleaned up {} expired JVM metrics records", deleted);
            
        } catch (Exception e) {
            log.error("Failed to cleanup JVM metrics", e);
        }
    }

    /**
     * 清理过期的慢SQL记录
     * 默认保留30天
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    @Transactional
    public void cleanupSlowSqlRecords() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            
            // 使用MyBatis Plus的delete方法
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.basebackend.observability.entity.SlowSqlRecord> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.lt(com.basebackend.observability.entity.SlowSqlRecord::getTimestamp, cutoffTime);
            
            int deleted = slowSqlRecordMapper.delete(wrapper);
            
            log.info("Cleaned up {} expired slow SQL records", deleted);
            
        } catch (Exception e) {
            log.error("Failed to cleanup slow SQL records", e);
        }
    }

    /**
     * 手动触发清理
     */
    public CleanupResult manualCleanup(int retentionDays) {
        CleanupResult result = new CleanupResult();
        
        try {
            // 清理JVM指标
            LocalDateTime jvmCutoff = LocalDateTime.now().minusDays(retentionDays);
            int jvmDeleted = jvmMetricsMapper.deleteExpired(jvmCutoff);
            result.setJvmMetricsDeleted(jvmDeleted);
            
            // 清理慢SQL
            LocalDateTime sqlCutoff = LocalDateTime.now().minusDays(retentionDays);
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.basebackend.observability.entity.SlowSqlRecord> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.lt(com.basebackend.observability.entity.SlowSqlRecord::getTimestamp, sqlCutoff);
            int sqlDeleted = slowSqlRecordMapper.delete(wrapper);
            result.setSlowSqlDeleted(sqlDeleted);
            
            result.setSuccess(true);
            result.setMessage("清理完成");
            
            log.info("Manual cleanup completed: JVM={}, SQL={}", jvmDeleted, sqlDeleted);
            
        } catch (Exception e) {
            log.error("Manual cleanup failed", e);
            result.setSuccess(false);
            result.setMessage("清理失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取存储统计
     */
    public StorageStatistics getStorageStatistics() {
        StorageStatistics stats = new StorageStatistics();
        
        try {
            // 统计JVM指标数量
            long jvmCount = jvmMetricsMapper.selectCount(null);
            stats.setJvmMetricsCount(jvmCount);
            
            // 统计慢SQL数量
            long sqlCount = slowSqlRecordMapper.selectCount(null);
            stats.setSlowSqlCount(sqlCount);
            
            // 估算存储大小（简化计算）
            stats.setEstimatedSizeMB((jvmCount * 0.5 + sqlCount * 1.0) / 1024);
            
        } catch (Exception e) {
            log.error("Failed to get storage statistics", e);
        }
        
        return stats;
    }

    /**
     * 清理结果
     */
    public static class CleanupResult {
        private boolean success;
        private String message;
        private int jvmMetricsDeleted;
        private int slowSqlDeleted;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getJvmMetricsDeleted() { return jvmMetricsDeleted; }
        public void setJvmMetricsDeleted(int jvmMetricsDeleted) { this.jvmMetricsDeleted = jvmMetricsDeleted; }
        public int getSlowSqlDeleted() { return slowSqlDeleted; }
        public void setSlowSqlDeleted(int slowSqlDeleted) { this.slowSqlDeleted = slowSqlDeleted; }
    }

    /**
     * 存储统计
     */
    public static class StorageStatistics {
        private long jvmMetricsCount;
        private long slowSqlCount;
        private double estimatedSizeMB;

        public long getJvmMetricsCount() { return jvmMetricsCount; }
        public void setJvmMetricsCount(long jvmMetricsCount) { this.jvmMetricsCount = jvmMetricsCount; }
        public long getSlowSqlCount() { return slowSqlCount; }
        public void setSlowSqlCount(long slowSqlCount) { this.slowSqlCount = slowSqlCount; }
        public double getEstimatedSizeMB() { return estimatedSizeMB; }
        public void setEstimatedSizeMB(double estimatedSizeMB) { this.estimatedSizeMB = estimatedSizeMB; }
    }
}
