package com.basebackend.database.dynamic.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据源上下文持有者测试
 * 测试栈式数据源切换和线程安全性
 *
 * @author BaseBackend
 */
@DisplayName("DataSourceContextHolder 数据源上下文测试")
class DataSourceContextHolderTest {

    @AfterEach
    void tearDown() {
        // 每个测试后清理上下文
        DataSourceContextHolder.clear();
    }

    @Test
    @DisplayName("设置和获取数据源键")
    void shouldSetAndGetDataSourceKey() {
        // When - 设置数据源
        DataSourceContextHolder.setDataSourceKey("master");
        String key = DataSourceContextHolder.getDataSourceKey();

        // Then
        assertThat(key).isEqualTo("master");
    }

    @Test
    @DisplayName("设置null或空数据源键应该被忽略")
    void shouldIgnoreNullOrEmptyDataSourceKey() {
        // When - 设置null
        DataSourceContextHolder.setDataSourceKey(null);
        String key1 = DataSourceContextHolder.getDataSourceKey();

        // Then - 应该为null
        assertThat(key1).isNull();

        // When - 设置空字符串
        DataSourceContextHolder.setDataSourceKey("   ");
        String key2 = DataSourceContextHolder.getDataSourceKey();

        // Then - 应该为null（之前的null值）
        assertThat(key2).isNull();
    }

    @Test
    @DisplayName("嵌套数据源切换应该使用栈结构")
    void shouldSupportNestedDataSourceSwitching() {
        // Given - 设置主数据源
        DataSourceContextHolder.setDataSourceKey("master");
        assertThat(DataSourceContextHolder.getDataSourceKey()).isEqualTo("master");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(1);

        // When - 切换到从数据源
        DataSourceContextHolder.setDataSourceKey("slave1");
        assertThat(DataSourceContextHolder.getDataSourceKey()).isEqualTo("slave1");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(2);

        // When - 再切换到另一个从数据源
        DataSourceContextHolder.setDataSourceKey("slave2");
        assertThat(DataSourceContextHolder.getDataSourceKey()).isEqualTo("slave2");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(3);
    }

    @Test
    @DisplayName("清除数据源应该恢复上一个数据源")
    void shouldRestorePreviousDataSourceWhenClearing() {
        // Given - 多层嵌套
        DataSourceContextHolder.setDataSourceKey("master");
        DataSourceContextHolder.setDataSourceKey("slave1");
        DataSourceContextHolder.setDataSourceKey("slave2");

        // When - 清除栈顶
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(DataSourceContextHolder.getDataSourceKey()).isEqualTo("slave1");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(2);

        // When - 再次清除
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(DataSourceContextHolder.getDataSourceKey()).isEqualTo("master");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(1);

        // When - 清除最后一个
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("空栈时清除不应该出错")
    void shouldHandleClearOnEmptyStack() {
        // Given - 空栈
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(0);

        // When - 尝试清除
        DataSourceContextHolder.clearDataSourceKey();

        // Then - 不应该抛异常，栈深度仍为0
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("完全清空应该清理ThreadLocal")
    void shouldClearThreadLocalCompletely() {
        // Given - 设置多个数据源
        DataSourceContextHolder.setDataSourceKey("master");
        DataSourceContextHolder.setDataSourceKey("slave1");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(2);

        // When - 完全清空
        DataSourceContextHolder.clear();

        // Then
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("多次清空不应该出错")
    void shouldHandleMultipleClears() {
        // Given
        DataSourceContextHolder.setDataSourceKey("master");

        // When - 多次清空
        DataSourceContextHolder.clear();
        DataSourceContextHolder.clear();
        DataSourceContextHolder.clear();

        // Then - 不应该抛异常
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
    }

    @Test
    @DisplayName("获取栈深度应该正确")
    void shouldGetCorrectStackDepth() {
        // Given - 空栈
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(0);

        // When - 添加数据源
        DataSourceContextHolder.setDataSourceKey("master");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(1);

        // When - 添加更多
        DataSourceContextHolder.setDataSourceKey("slave1");
        DataSourceContextHolder.setDataSourceKey("slave2");
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(3);

        // When - 清除
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("线程安全性测试 - 多线程访问")
    void shouldBeThreadSafe() throws Exception {
        // Given - 并发线程数和倒计数闩
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - 并发设置和获取数据源
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    String key = "datasource-" + index;
                    DataSourceContextHolder.setDataSourceKey(key);
                    try {
                        Thread.sleep(10); // 模拟一些工作
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    String currentKey = DataSourceContextHolder.getDataSourceKey();
                    assertThat(currentKey).isEqualTo(key);
                    DataSourceContextHolder.clearDataSourceKey();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        // Then - 等待所有线程完成
        latch.await();
        CompletableFuture.allOf(futures).join();

        // 清理
        executor.shutdown();

        // 验证最终状态
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
        assertThat(DataSourceContextHolder.getStackDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("线程隔离测试 - 不同线程不互相影响")
    void shouldIsolateThreadLocals() throws Exception {
        // Given - 两个线程
        String[] thread1Key = {null};
        String[] thread2Key = {null};

        Thread t1 = new Thread(() -> {
            DataSourceContextHolder.setDataSourceKey("thread1-ds");
            thread1Key[0] = DataSourceContextHolder.getDataSourceKey();
        });

        Thread t2 = new Thread(() -> {
            DataSourceContextHolder.setDataSourceKey("thread2-ds");
            thread2Key[0] = DataSourceContextHolder.getDataSourceKey();
        });

        // When - 同时运行
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Then - 每个线程应该有自己的值
        assertThat(thread1Key[0]).isEqualTo("thread1-ds");
        assertThat(thread2Key[0]).isEqualTo("thread2-ds");

        // 主线程应该是空的
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
    }
}
