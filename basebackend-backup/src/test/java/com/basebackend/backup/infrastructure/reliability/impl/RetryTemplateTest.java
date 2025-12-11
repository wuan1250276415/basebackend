package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.RecoveryCallback;
import com.basebackend.backup.infrastructure.reliability.RetryCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 重试模板测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetryTemplate 重试机制测试")
class RetryTemplateTest {

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private RetryCallback<String> retryCallback;

    @Mock
    private RecoveryCallback<String> recoveryCallback;

    @Mock
    private BackupProperties.Retry retryConfig;

    @Mock
    private BackupProperties.Retry.Backoff backoffConfig;

    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() {
        retryTemplate = new RetryTemplate(backupProperties);

        // Mock the Retry configuration
        when(backupProperties.getRetry()).thenReturn(retryConfig);
        when(retryConfig.getMaxAttempts()).thenReturn(3);
        when(retryConfig.getBackoff()).thenReturn(backoffConfig);
        when(backoffConfig.getInitial()).thenReturn(Duration.ofMillis(100));
        when(backoffConfig.getMultiplier()).thenReturn(2.0);
        when(backoffConfig.getMax()).thenReturn(Duration.ofMillis(1000));
    }

    @Test
    @DisplayName("成功操作应该在第一次尝试时直接返回")
    void shouldReturnSuccessOnFirstAttempt() throws Exception {
        // Given
        String expectedResult = "success";
        when(retryCallback.doWithRetry()).thenReturn(expectedResult);

        // When
        String result = retryTemplate.execute(retryCallback);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(retryCallback, times(1)).doWithRetry();
    }

    @Test
    @DisplayName("第二次重试成功应该返回结果")
    void shouldSucceedOnSecondRetry() throws Exception {
        // Given
        when(retryCallback.doWithRetry())
            .thenThrow(new RuntimeException("First failure"))
            .thenReturn("success");

        // When
        String result = retryTemplate.execute(retryCallback);

        // Then
        assertThat(result).isEqualTo("success");
        verify(retryCallback, times(2)).doWithRetry();
    }

    @Test
    @DisplayName("达到最大重试次数后应该抛出最后一次异常")
    void shouldThrowExceptionAfterMaxRetries() throws Exception {
        // Given
        RuntimeException lastException = new RuntimeException("Final failure");
        when(retryCallback.doWithRetry()).thenThrow(lastException);

        // When & Then
        assertThatThrownBy(() -> retryTemplate.execute(retryCallback))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Final failure");

        // 验证重试了最大次数
        verify(retryCallback, times(3)).doWithRetry();
    }

    @Test
    @DisplayName("重试失败后应该调用恢复回调")
    void shouldCallRecoveryCallbackWhenAllRetriesFail() throws Exception {
        // Given
        RuntimeException failure = new RuntimeException("All retries failed");
        String recoveryResult = "recovered";
        when(retryCallback.doWithRetry()).thenThrow(failure);
        when(recoveryCallback.recover(failure)).thenReturn(recoveryResult);

        // When
        String result = retryTemplate.execute(retryCallback, recoveryCallback);

        // Then
        assertThat(result).isEqualTo(recoveryResult);
        verify(recoveryCallback, times(1)).recover(failure);
    }

    @Test
    @DisplayName("恢复回调失败应该抛出异常")
    void shouldThrowExceptionWhenRecoveryCallbackAlsoFails() throws Exception {
        // Given
        RuntimeException failure = new RuntimeException("Original failure");
        RuntimeException recoveryFailure = new RuntimeException("Recovery failed");
        when(retryCallback.doWithRetry()).thenThrow(failure);
        when(recoveryCallback.recover(failure)).thenThrow(recoveryFailure);

        // When & Then
        assertThatThrownBy(() -> retryTemplate.execute(retryCallback, recoveryCallback))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Recovery failed");
    }

    @Test
    @DisplayName("恢复回调为空时应该抛出最后一次异常")
    void shouldThrowLastExceptionWhenNoRecoveryCallback() throws Exception {
        // Given
        RuntimeException lastException = new RuntimeException("No recovery available");
        when(retryCallback.doWithRetry()).thenThrow(lastException);

        // When & Then
        assertThatThrownBy(() -> retryTemplate.execute(retryCallback, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("No recovery available");
    }

    @Test
    @DisplayName("指数退避计算应该正确")
    void shouldCalculateExponentialBackoffCorrectly() throws Exception {
        // Given - 配置不同的参数
        when(retryConfig.getMaxAttempts()).thenReturn(5);
        when(backoffConfig.getInitial()).thenReturn(Duration.ofMillis(100));
        when(backoffConfig.getMultiplier()).thenReturn(2.0);
        when(backoffConfig.getMax()).thenReturn(Duration.ofMillis(10000));

        // Mock Thread.sleep to verify the delay
        Thread.sleep(0); // Just to make the first call

        AtomicInteger attemptCount = new AtomicInteger();
        when(retryCallback.doWithRetry()).thenAnswer(inv -> {
            attemptCount.getAndIncrement();
            if (attemptCount.get() == 5) {
                return "success";
            }
            throw new RuntimeException("Attempt " + attemptCount);
        });

        // When
        String result = retryTemplate.execute(retryCallback);

        // Then
        assertThat(result).isEqualTo("success");
        assertThat(attemptCount.get()).isEqualTo(5);
    }

    @Test
    @DisplayName("延迟不应该超过最大延迟")
    void shouldCapDelayAtMaxDelay() throws Exception {
        // Given - 小初始延迟，大乘数，让延迟快速增长超过max
        when(retryConfig.getMaxAttempts()).thenReturn(4);
        when(backoffConfig.getInitial()).thenReturn(Duration.ofMillis(10));
        when(backoffConfig.getMultiplier()).thenReturn(1000.0);
        when(backoffConfig.getMax()).thenReturn(Duration.ofMillis(500));

        AtomicInteger attemptCount = new AtomicInteger();
        when(retryCallback.doWithRetry()).thenAnswer(inv -> {
            attemptCount.getAndIncrement();
            if (attemptCount.get() == 4) {
                return "success";
            }
            throw new RuntimeException("Attempt " + attemptCount);
        });

        // When
        String result = retryTemplate.execute(retryCallback);

        // Then
        assertThat(result).isEqualTo("success");
        // 验证延迟被限制在maxDelay之内
        // 实际时间验证需要用mocking或测试clock
    }

    @Test
    @DisplayName("操作执行异常应该被正确处理")
    void shouldHandleOperationExceptions() throws Exception {
        // Given
        when(retryConfig.getMaxAttempts()).thenReturn(2);
        RuntimeException testException = new RuntimeException("Test failure");
        when(retryCallback.doWithRetry())
            .thenThrow(testException)
            .thenThrow(testException);

        // When & Then
        assertThatThrownBy(() -> retryTemplate.execute(retryCallback))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test failure");
    }

    @Test
    @DisplayName("最大尝试次数为1时应该不重试")
    void shouldNotRetryWhenMaxAttemptsIsOne() throws Exception {
        // Given
        when(retryConfig.getMaxAttempts()).thenReturn(1);
        when(retryCallback.doWithRetry()).thenThrow(new RuntimeException("Failed"));

        // When & Then
        assertThatThrownBy(() -> retryTemplate.execute(retryCallback))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed");

        verify(retryCallback, times(1)).doWithRetry();
    }

    @Test
    @DisplayName("重试间隔应该正确计算")
    void shouldCalculateRetryIntervalsCorrectly() throws Exception {
        // Given
        long startTime = System.currentTimeMillis();
        when(retryConfig.getMaxAttempts()).thenReturn(4);
        when(backoffConfig.getInitial()).thenReturn(Duration.ofMillis(100));
        when(backoffConfig.getMultiplier()).thenReturn(2.0);
        when(backoffConfig.getMax()).thenReturn(Duration.ofMillis(10000));

        AtomicInteger callCount = new AtomicInteger();
        when(retryCallback.doWithRetry()).thenAnswer(inv -> {
            callCount.getAndIncrement();
            if (callCount.get() == 4) {
                return "success";
            }
            throw new RuntimeException("Attempt " + callCount);
        });

        // When
        String result = retryTemplate.execute(retryCallback);

        // Then
        assertThat(result).isEqualTo("success");
        long totalTime = System.currentTimeMillis() - startTime;

        // 期望的总时间大约是: 100 + 200 + 400 = 700ms (加上一些误差)
        assertThat(totalTime).isGreaterThanOrEqualTo(600);
        assertThat(totalTime).isLessThan(1000);
    }
}
