package com.basebackend.scheduler.core.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CircuitBreakerService 单元测试。
 * 覆盖断路器状态转换、降级逻辑等核心功能。
 */
@DisplayName("CircuitBreakerService 单元测试")
class CircuitBreakerServiceTest {

    private CircuitBreakerService circuitBreakerService;

    @BeforeEach
    void setUp() {
        // 使用较小的配置值便于测试
        CircuitBreakerService.CircuitBreakerConfig config = new CircuitBreakerService.CircuitBreakerConfig(
                50,  // 失败率阈值50%
                10,  // 滑动窗口大小
                5,   // 最小调用次数
                Duration.ofMillis(100), // 等待时间100ms
                2    // 半开状态允许2次调用
        );
        circuitBreakerService = new CircuitBreakerService(config);
    }

    @Test
    @DisplayName("初始状态为CLOSED")
    void testInitialStateClosed() {
        assertEquals(CircuitBreakerService.State.CLOSED, circuitBreakerService.getState("test"));
    }

    @Test
    @DisplayName("成功执行不改变状态")
    void testSuccessfulExecutionKeepsClosed() {
        // 执行成功操作
        String result = circuitBreakerService.execute("test", () -> "success");

        assertEquals("success", result);
        assertEquals(CircuitBreakerService.State.CLOSED, circuitBreakerService.getState("test"));
    }

    @Test
    @DisplayName("失败率超过阈值时断路器打开")
    void testCircuitOpensOnHighFailureRate() {
        AtomicInteger callCount = new AtomicInteger(0);

        // 执行5次失败（达到最小调用次数，失败率100%）
        for (int i = 0; i < 5; i++) {
            circuitBreakerService.executeWithFallback("test",
                    () -> {
                        callCount.incrementAndGet();
                        throw new RuntimeException("Simulated failure");
                    },
                    () -> "fallback"
            );
        }

        // 验证断路器打开
        assertEquals(CircuitBreakerService.State.OPEN, circuitBreakerService.getState("test"));
    }

    @Test
    @DisplayName("断路器打开时执行降级逻辑")
    void testFallbackWhenOpen() {
        // 先让断路器打开
        for (int i = 0; i < 5; i++) {
            circuitBreakerService.executeWithFallback("test",
                    () -> { throw new RuntimeException("fail"); },
                    () -> "fallback"
            );
        }

        // 验证断路器打开
        assertEquals(CircuitBreakerService.State.OPEN, circuitBreakerService.getState("test"));

        // 再次调用应该直接返回降级结果
        AtomicInteger mainCalled = new AtomicInteger(0);
        String result = circuitBreakerService.executeWithFallback("test",
                () -> {
                    mainCalled.incrementAndGet();
                    return "main";
                },
                () -> "fallback"
        );

        assertEquals("fallback", result);
        assertEquals(0, mainCalled.get()); // 主逻辑不应被调用
    }

    @Test
    @DisplayName("等待时间后断路器转为半开状态")
    void testTransitionToHalfOpen() throws InterruptedException {
        // 让断路器打开
        for (int i = 0; i < 5; i++) {
            circuitBreakerService.executeWithFallback("test",
                    () -> { throw new RuntimeException("fail"); },
                    () -> "fallback"
            );
        }
        assertEquals(CircuitBreakerService.State.OPEN, circuitBreakerService.getState("test"));

        // 等待超过配置的等待时间
        Thread.sleep(150);

        // 下一次调用应该触发状态转换
        circuitBreakerService.executeWithFallback("test",
                () -> "success",
                () -> "fallback"
        );

        // 验证状态（成功后应该回到CLOSED或保持HALF_OPEN）
        CircuitBreakerService.State state = circuitBreakerService.getState("test");
        assertTrue(state == CircuitBreakerService.State.HALF_OPEN || state == CircuitBreakerService.State.CLOSED);
    }

    @Test
    @DisplayName("重置断路器")
    void testReset() {
        // 让断路器打开
        for (int i = 0; i < 5; i++) {
            circuitBreakerService.executeWithFallback("test",
                    () -> { throw new RuntimeException("fail"); },
                    () -> "fallback"
            );
        }
        assertEquals(CircuitBreakerService.State.OPEN, circuitBreakerService.getState("test"));

        // 重置
        circuitBreakerService.reset("test");

        // 验证状态回到CLOSED
        assertEquals(CircuitBreakerService.State.CLOSED, circuitBreakerService.getState("test"));
    }

    @Test
    @DisplayName("不同名称的断路器相互独立")
    void testIndependentCircuitBreakers() {
        // 让breaker1打开
        for (int i = 0; i < 5; i++) {
            circuitBreakerService.executeWithFallback("breaker1",
                    () -> { throw new RuntimeException("fail"); },
                    () -> "fallback"
            );
        }

        // breaker2应该仍然是CLOSED
        assertEquals(CircuitBreakerService.State.OPEN, circuitBreakerService.getState("breaker1"));
        assertEquals(CircuitBreakerService.State.CLOSED, circuitBreakerService.getState("breaker2"));
    }

    @Test
    @DisplayName("断路器打开异常")
    void testCircuitBreakerOpenException() {
        // 让断路器打开
        for (int i = 0; i < 5; i++) {
            circuitBreakerService.executeWithFallback("test",
                    () -> { throw new RuntimeException("fail"); },
                    () -> "fallback"
            );
        }

        // 使用execute方法（无降级）应该抛出异常
        assertThrows(CircuitBreakerService.CircuitBreakerOpenException.class, () -> {
            circuitBreakerService.execute("test", () -> "should not execute");
        });
    }
}
