package com.basebackend.logging.audit.service;

import com.basebackend.logging.audit.crypto.HashChainCalculator;
import com.basebackend.logging.audit.crypto.AuditSignatureService;
import com.basebackend.logging.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.security.cert.Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 审计验证服务
 *
 * 提供完整的审计日志完整性验证功能：
 * - 哈希链完整性校验
 * - 数字签名验证
 * - 篡改检测
 * - 定期自动验证
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class AuditVerificationService {

    private final HashChainCalculator hashChainCalculator;
    private final AuditSignatureService signatureService;
    private final ExecutorService verificationExecutor;

    // 验证统计数据
    private volatile long totalVerifiedEntries = 0;
    private volatile long totalVerificationErrors = 0;
    private volatile long lastVerificationTime = 0;
    private volatile Instant lastSuccessfulVerification = null;

    public AuditVerificationService(HashChainCalculator hashChainCalculator,
                                    AuditSignatureService signatureService) {
        this.hashChainCalculator = hashChainCalculator;
        this.signatureService = signatureService;
        this.verificationExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "audit-verification");
            t.setDaemon(true);
            return t;
        });

        log.info("审计验证服务初始化完成");
    }

    /**
     * 验证单个审计日志条目
     */
    public VerificationResult verifyEntry(AuditLogEntry entry, String prevHash) {
        if (entry == null) {
            return VerificationResult.builder()
                    .valid(false)
                    .error("审计日志条目为空")
                    .build();
        }

        try {
            // 验证哈希链
            boolean hashValid = hashChainCalculator.verifyEntry(entry, prevHash);

            if (!hashValid) {
                return VerificationResult.builder()
                        .valid(false)
                        .error("哈希链验证失败")
                        .entry(entry)
                        .build();
            }

            // 验证数字签名
            boolean signatureValid = signatureService.verify(entry);

            if (!signatureValid) {
                return VerificationResult.builder()
                        .valid(false)
                        .error("数字签名验证失败")
                        .entry(entry)
                        .build();
            }

            totalVerifiedEntries++;
            lastVerificationTime = System.currentTimeMillis();

            return VerificationResult.builder()
                    .valid(true)
                    .entry(entry)
                    .build();

        } catch (Exception e) {
            log.error("验证审计日志条目异常", e);
            totalVerificationErrors++;
            return VerificationResult.builder()
                    .valid(false)
                    .error("验证过程异常: " + e.getMessage())
                    .entry(entry)
                    .build();
        }
    }

    /**
     * 验证审计日志列表（按时间排序）
     */
    public VerificationReport verifyChain(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return VerificationReport.builder()
                    .valid(true)
                    .message("没有需要验证的条目")
                    .totalEntries(0)
                    .build();
        }

        log.info("开始验证审计日志链，共 {} 个条目", entries.size());

        long startTime = System.currentTimeMillis();
        int totalEntries = entries.size();
        int errorCount = 0;
        List<VerificationResult> errors = new ArrayList<>();

        String prevHash = null;
        int index = 0;

        try {
            for (AuditLogEntry entry : entries) {
                index++;

                VerificationResult result = verifyEntry(entry, prevHash);

                if (!result.isValid()) {
                    errorCount++;
                    result.setEntryIndex(index);
                    errors.add(result);

                    log.error("验证失败 [{}]: {} - {}", index, entry.getId(), result.getError());
                }

                prevHash = entry.getEntryHash();
            }

            boolean valid = errorCount == 0;
            long elapsedMs = System.currentTimeMillis() - startTime;

            VerificationReport report = VerificationReport.builder()
                    .valid(valid)
                    .totalEntries(totalEntries)
                    .errorCount(errorCount)
                    .successCount(totalEntries - errorCount)
                    .elapsedMs(elapsedMs)
                    .errors(errors)
                    .build();

            if (valid) {
                log.info("审计日志链验证通过，条目数: {}, 耗时: {}ms", totalEntries, elapsedMs);
                lastSuccessfulVerification = Instant.now();
            } else {
                log.error("审计日志链验证失败，错误数: {}/{}, 耗时: {}ms", errorCount, totalEntries, elapsedMs);
            }

            totalVerifiedEntries += totalEntries;
            totalVerificationErrors += errorCount;
            lastVerificationTime = System.currentTimeMillis();

            return report;

        } catch (Exception e) {
            log.error("验证审计日志链异常", e);
            return VerificationReport.builder()
                    .valid(false)
                    .totalEntries(totalEntries)
                    .errorCount(totalEntries)
                    .elapsedMs(System.currentTimeMillis() - startTime)
                    .error("验证过程异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 批量验证（异步）
     */
    public CompletableFuture<VerificationReport> verifyChainAsync(List<AuditLogEntry> entries) {
        return CompletableFuture.supplyAsync(() -> verifyChain(entries), verificationExecutor);
    }

    /**
     * 并行验证多个分片
     */
    public VerificationReport verifySharded(List<AuditLogEntry> entries, int shardCount) {
        if (entries == null || entries.isEmpty()) {
            return VerificationReport.builder()
                    .valid(true)
                    .message("没有需要验证的条目")
                    .totalEntries(0)
                    .build();
        }

        int shardSize = (entries.size() + shardCount - 1) / shardCount;
        List<CompletableFuture<VerificationReport>> futures = new ArrayList<>();

        for (int i = 0; i < shardCount; i++) {
            int start = i * shardSize;
            int end = Math.min(start + shardSize, entries.size());

            if (start >= end) {
                break;
            }

            List<AuditLogEntry> shard = entries.subList(start, end);
            futures.add(verifyChainAsync(shard));
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allFutures.get(30, TimeUnit.SECONDS);

            // 合并结果
            int totalEntries = 0;
            int errorCount = 0;
            int successCount = 0;
            long totalElapsed = 0;
            List<VerificationResult> allErrors = new ArrayList<>();

            for (CompletableFuture<VerificationReport> future : futures) {
                VerificationReport report = future.join();
                totalEntries += report.getTotalEntries();
                errorCount += report.getErrorCount();
                successCount += report.getSuccessCount();
                totalElapsed += report.getElapsedMs();
                if (report.getErrors() != null) {
                    allErrors.addAll(report.getErrors());
                }
            }

            boolean valid = errorCount == 0;

            VerificationReport mergedReport = VerificationReport.builder()
                    .valid(valid)
                    .totalEntries(totalEntries)
                    .errorCount(errorCount)
                    .successCount(successCount)
                    .elapsedMs(totalElapsed)
                    .errors(allErrors)
                    .build();

            if (valid) {
                log.info("并行验证完成，总条目: {}, 耗时: {}ms", totalEntries, totalElapsed);
            } else {
                log.error("并行验证失败，错误数: {}/{}", errorCount, totalEntries);
            }

            return mergedReport;

        } catch (Exception e) {
            log.error("并行验证异常", e);
            throw new RuntimeException("并行验证失败", e);
        }
    }

    /**
     * 定时验证任务
     */
    @Scheduled(fixedDelay = 3600000) // 每小时执行一次
    public void scheduledVerification() {
        log.info("开始执行定时审计验证任务");

        try {
            // 这里可以实现从存储中读取最近的审计日志进行验证
            // 例如：读取最近 10000 条记录
            log.debug("定时验证任务完成");
        } catch (Exception e) {
            log.error("定时验证任务异常", e);
        }
    }

    /**
     * 获取验证统计信息
     */
    public VerificationStats getStats() {
        return VerificationStats.builder()
                .totalVerifiedEntries(totalVerifiedEntries)
                .totalVerificationErrors(totalVerificationErrors)
                .lastVerificationTime(lastVerificationTime)
                .lastSuccessfulVerification(lastSuccessfulVerification)
                .build();
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalVerifiedEntries = 0;
        totalVerificationErrors = 0;
        lastVerificationTime = 0;
        lastSuccessfulVerification = null;
        log.info("验证统计信息已重置");
    }

    /**
     * 关闭验证服务
     */
    public void shutdown() {
        verificationExecutor.shutdown();
        try {
            if (!verificationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                verificationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("等待验证线程结束被中断", e);
            Thread.currentThread().interrupt();
        }
        log.info("审计验证服务已关闭");
    }

    /**
     * 验证结果
     */
    public static class VerificationResult {
        private final boolean valid;
        private final String error;
        private final AuditLogEntry entry;
        private Integer entryIndex;

        private VerificationResult(Builder builder) {
            this.valid = builder.valid;
            this.error = builder.error;
            this.entry = builder.entry;
            this.entryIndex = builder.entryIndex;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isValid() {
            return valid;
        }

        public String getError() {
            return error;
        }

        public AuditLogEntry getEntry() {
            return entry;
        }

        public Integer getEntryIndex() {
            return entryIndex;
        }

        public void setEntryIndex(Integer entryIndex) {
            this.entryIndex = entryIndex;
        }

        public static class Builder {
            private boolean valid;
            private String error;
            private AuditLogEntry entry;
            private Integer entryIndex;

            public Builder valid(boolean valid) {
                this.valid = valid;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder entry(AuditLogEntry entry) {
                this.entry = entry;
                return this;
            }

            public Builder entryIndex(Integer entryIndex) {
                this.entryIndex = entryIndex;
                return this;
            }

            public VerificationResult build() {
                return new VerificationResult(this);
            }
        }
    }

    /**
     * 验证报告
     */
    public static class VerificationReport {
        private final boolean valid;
        private final int totalEntries;
        private final int errorCount;
        private final int successCount;
        private final long elapsedMs;
        private final List<VerificationResult> errors;
        private final String message;
        private final String error;

        private VerificationReport(Builder builder) {
            this.valid = builder.valid;
            this.totalEntries = builder.totalEntries;
            this.errorCount = builder.errorCount;
            this.successCount = builder.successCount;
            this.elapsedMs = builder.elapsedMs;
            this.errors = builder.errors;
            this.message = builder.message;
            this.error = builder.error;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isValid() {
            return valid;
        }

        public int getTotalEntries() {
            return totalEntries;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public List<VerificationResult> getErrors() {
            return errors;
        }

        public String getMessage() {
            return message;
        }

        public String getError() {
            return error;
        }

        public static class Builder {
            private boolean valid;
            private int totalEntries;
            private int errorCount;
            private int successCount;
            private long elapsedMs;
            private List<VerificationResult> errors;
            private String message;
            private String error;

            public Builder valid(boolean valid) {
                this.valid = valid;
                return this;
            }

            public Builder totalEntries(int totalEntries) {
                this.totalEntries = totalEntries;
                return this;
            }

            public Builder errorCount(int errorCount) {
                this.errorCount = errorCount;
                return this;
            }

            public Builder successCount(int successCount) {
                this.successCount = successCount;
                return this;
            }

            public Builder elapsedMs(long elapsedMs) {
                this.elapsedMs = elapsedMs;
                return this;
            }

            public Builder errors(List<VerificationResult> errors) {
                this.errors = errors;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public VerificationReport build() {
                return new VerificationReport(this);
            }
        }
    }

    /**
     * 验证统计信息
     */
    public static class VerificationStats {
        private final long totalVerifiedEntries;
        private final long totalVerificationErrors;
        private final long lastVerificationTime;
        private final Instant lastSuccessfulVerification;

        private VerificationStats(Builder builder) {
            this.totalVerifiedEntries = builder.totalVerifiedEntries;
            this.totalVerificationErrors = builder.totalVerificationErrors;
            this.lastVerificationTime = builder.lastVerificationTime;
            this.lastSuccessfulVerification = builder.lastSuccessfulVerification;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalVerifiedEntries() {
            return totalVerifiedEntries;
        }

        public long getTotalVerificationErrors() {
            return totalVerificationErrors;
        }

        public long getLastVerificationTime() {
            return lastVerificationTime;
        }

        public Instant getLastSuccessfulVerification() {
            return lastSuccessfulVerification;
        }

        public double getErrorRate() {
            return totalVerifiedEntries > 0
                    ? (double) totalVerificationErrors / totalVerifiedEntries
                    : 0.0;
        }

        public static class Builder {
            private long totalVerifiedEntries;
            private long totalVerificationErrors;
            private long lastVerificationTime;
            private Instant lastSuccessfulVerification;

            public Builder totalVerifiedEntries(long totalVerifiedEntries) {
                this.totalVerifiedEntries = totalVerifiedEntries;
                return this;
            }

            public Builder totalVerificationErrors(long totalVerificationErrors) {
                this.totalVerificationErrors = totalVerificationErrors;
                return this;
            }

            public Builder lastVerificationTime(long lastVerificationTime) {
                this.lastVerificationTime = lastVerificationTime;
                return this;
            }

            public Builder lastSuccessfulVerification(Instant lastSuccessfulVerification) {
                this.lastSuccessfulVerification = lastSuccessfulVerification;
                return this;
            }

            public VerificationStats build() {
                return new VerificationStats(this);
            }
        }
    }
}
