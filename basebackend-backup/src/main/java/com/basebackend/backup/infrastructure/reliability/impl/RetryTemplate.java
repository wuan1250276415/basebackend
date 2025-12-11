package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.RecoveryCallback;
import com.basebackend.backup.infrastructure.reliability.RetryCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeoutException;

/**
 * 重试模板
 * <p>
 * 实现指数退避重试机制，用于处理备份过程中的临时性失败。
 * 支持自定义最大重试次数、初始延迟、退避倍数和最大延迟。
 *
 * @author BaseBackend
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryTemplate {

    private final BackupProperties backupProperties;

    /**
     * 执行重试操作
     *
     * @param callback 要重试的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 所有重试都失败时抛出最后一次异常
     */
    public <T> T execute(RetryCallback<T> callback) throws Exception {
        return execute(callback, null);
    }

    /**
     * 执行重试操作（带恢复回调）
     *
     * @param callback 要重试的操作
     * @param recovery 重试失败后的恢复操作
     * @param <T> 返回类型
     * @return 操作结果或恢复结果
     * @throws Exception 所有重试和恢复都失败时抛出异常
     */
    public <T> T execute(RetryCallback<T> callback, RecoveryCallback<T> recovery) throws Exception {
        int maxAttempts = backupProperties.getRetry().getMaxAttempts();
        long initialDelay = backupProperties.getRetry().getBackoff().getInitial().toMillis();
        double multiplier = backupProperties.getRetry().getBackoff().getMultiplier();
        long maxDelay = backupProperties.getRetry().getBackoff().getMax().toMillis();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("执行重试操作, 尝试次数: {}/{}", attempt, maxAttempts);
                return callback.doWithRetry();
            } catch (Exception e) {
                lastException = e;
                log.warn("重试操作失败, 尝试次数: {}, 错误: {}", attempt, e.getMessage());

                // 最后一次尝试失败
                if (attempt == maxAttempts) {
                    log.error("所有重试都失败, 已达最大尝试次数: {}", maxAttempts);
                    break;
                }

                // 计算延迟时间（指数退避）
                long delay = (long) (initialDelay * Math.pow(multiplier, attempt - 1));
                delay = Math.min(delay, maxDelay);

                log.info("等待 {}ms 后进行第 {} 次重试", delay, attempt + 1);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }

        // 所有重试都失败，尝试恢复
        if (recovery != null) {
            log.info("尝试执行恢复操作");
            try {
                return recovery.recover(lastException);
            } catch (Exception recoveryException) {
                log.error("恢复操作也失败", recoveryException);
                throw recoveryException;
            }
        }

        // 没有恢复策略，抛出最后一次异常
        throw lastException;
    }
}
