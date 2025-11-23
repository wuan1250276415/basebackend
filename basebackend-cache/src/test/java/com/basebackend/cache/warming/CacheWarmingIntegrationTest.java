package com.basebackend.cache.warming;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 缓存预热集成测试
 */
@ExtendWith(MockitoExtension.class)
class CacheWarmingIntegrationTest {

    @Mock
    private RedisService redisService;

    @Mock
    private CacheProperties cacheProperties;

    private CacheWarmingExecutor executor;
    private CacheWarmingManager manager;

    @BeforeEach
    void setUp() {
        // 设置配置
        CacheProperties.Warming warmingConfig = new CacheProperties.Warming();
        warmingConfig.setEnabled(true);
        warmingConfig.setTimeout(Duration.ofMinutes(5));
        warmingConfig.setAsync(false);
        
        lenient().when(cacheProperties.getWarming()).thenReturn(warmingConfig);

        // 创建执行器和管理器
        executor = new CacheWarmingExecutor(redisService, null);
        manager = new CacheWarmingManager(cacheProperties, executor);
    }

    @Test
    void testRegisterWarmingTask() {
        // 创建预热任务
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("test-task")
                .priority(1)
                .dataLoader(() -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("key1", "value1");
                    data.put("key2", "value2");
                    return data;
                })
                .ttl(Duration.ofHours(1))
                .async(false)
                .build();

        // 注册任务
        manager.registerWarmingTask(task);

        // 验证
        assertEquals(1, manager.getTaskCount());
        assertEquals("test-task", manager.getTasks().get(0).getName());
    }

    @Test
    void testExecuteWarmingTask() {
        // 创建预热任务
        Map<String, Object> testData = new HashMap<>();
        testData.put("user:1", "User 1");
        testData.put("user:2", "User 2");
        testData.put("user:3", "User 3");

        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("test-users")
                .priority(1)
                .dataLoader(() -> testData)
                .ttl(Duration.ofHours(1))
                .async(false)
                .build();

        // 执行任务
        boolean success = executor.execute(task);

        // 验证
        assertTrue(success);
        assertEquals(CacheWarmingTask.TaskStatus.SUCCESS, task.getStatus());
        assertEquals(3, task.getItemCount());
        assertEquals(3, task.getLoadedCount());
        assertEquals(0, task.getFailedCount());
        assertTrue(task.getExecutionTime() >= 0);

        // 验证 Redis 调用
        verify(redisService, times(3)).set(anyString(), any(), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testExecuteWarmingTaskWithEmptyData() {
        // 创建返回空数据的任务
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("empty-task")
                .priority(1)
                .dataLoader(HashMap::new)
                .ttl(Duration.ofHours(1))
                .async(false)
                .build();

        // 执行任务
        boolean success = executor.execute(task);

        // 验证
        assertTrue(success);
        assertEquals(CacheWarmingTask.TaskStatus.SUCCESS, task.getStatus());
        assertEquals(0, task.getItemCount());
        assertEquals(0, task.getLoadedCount());

        // 验证没有调用 Redis
        verify(redisService, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void testExecuteWarmingTaskWithException() {
        // 创建会抛出异常的任务
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("error-task")
                .priority(1)
                .dataLoader(() -> {
                    throw new RuntimeException("Test exception");
                })
                .ttl(Duration.ofHours(1))
                .async(false)
                .build();

        // 执行任务
        boolean success = executor.execute(task);

        // 验证
        assertFalse(success);
        assertEquals(CacheWarmingTask.TaskStatus.FAILED, task.getStatus());
        assertNotNull(task.getErrorMessage());
        assertTrue(task.getErrorMessage().contains("Test exception"));
    }

    @Test
    void testExecuteWarmingTaskWithPartialFailure() {
        // Mock Redis 服务，让部分调用失败
        doNothing().when(redisService).set(eq("key1"), any(), anyLong(), any());
        doThrow(new RuntimeException("Redis error")).when(redisService).set(eq("key2"), any(), anyLong(), any());
        doNothing().when(redisService).set(eq("key3"), any(), anyLong(), any());

        // 创建预热任务
        Map<String, Object> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", "value2");
        testData.put("key3", "value3");

        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("partial-fail-task")
                .priority(1)
                .dataLoader(() -> testData)
                .ttl(Duration.ofHours(1))
                .async(false)
                .build();

        // 执行任务
        boolean success = executor.execute(task);

        // 验证
        assertTrue(success); // 部分成功也返回 true
        assertEquals(CacheWarmingTask.TaskStatus.PARTIAL_SUCCESS, task.getStatus());
        assertEquals(3, task.getItemCount());
        assertEquals(2, task.getLoadedCount());
        assertEquals(1, task.getFailedCount());
    }

    @Test
    void testTaskPriorityOrdering() {
        // 创建多个不同优先级的任务
        CacheWarmingTask task1 = CacheWarmingTask.builder()
                .name("low-priority")
                .priority(10)
                .dataLoader(HashMap::new)
                .build();

        CacheWarmingTask task2 = CacheWarmingTask.builder()
                .name("high-priority")
                .priority(1)
                .dataLoader(HashMap::new)
                .build();

        CacheWarmingTask task3 = CacheWarmingTask.builder()
                .name("medium-priority")
                .priority(5)
                .dataLoader(HashMap::new)
                .build();

        // 注册任务
        manager.registerWarmingTask(task1);
        manager.registerWarmingTask(task2);
        manager.registerWarmingTask(task3);

        // 验证任务数量
        assertEquals(3, manager.getTaskCount());
    }

    @Test
    void testDuplicateTaskRegistration() {
        // 创建任务
        CacheWarmingTask task1 = CacheWarmingTask.builder()
                .name("duplicate-task")
                .priority(1)
                .dataLoader(HashMap::new)
                .build();

        CacheWarmingTask task2 = CacheWarmingTask.builder()
                .name("duplicate-task")
                .priority(2)
                .dataLoader(HashMap::new)
                .build();

        // 注册任务
        manager.registerWarmingTask(task1);
        manager.registerWarmingTask(task2); // 应该被忽略

        // 验证只注册了一个任务
        assertEquals(1, manager.getTaskCount());
    }

    @Test
    void testWarmingProgress() {
        // 创建任务
        Map<String, Object> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", "value2");

        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("progress-task")
                .priority(1)
                .dataLoader(() -> testData)
                .ttl(Duration.ofHours(1))
                .build();

        manager.registerWarmingTask(task);

        // 执行任务
        manager.executeWarmingTasks();

        // 获取进度
        WarmingProgress progress = manager.getProgress();

        // 验证进度
        assertNotNull(progress);
        assertEquals(1, progress.getTotalTasks());
        assertEquals(1, progress.getCompletedTasks());
        assertEquals(1, progress.getSuccessTasks());
        assertEquals(0, progress.getFailedTasks());
        assertEquals(2, progress.getTotalItems());
        assertEquals(2, progress.getLoadedItems());
        assertTrue(progress.isCompleted());
        assertTrue(progress.isAllSuccess());
    }

    @Test
    void testClearTasks() {
        // 注册任务
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("test-task")
                .priority(1)
                .dataLoader(HashMap::new)
                .build();

        manager.registerWarmingTask(task);
        assertEquals(1, manager.getTaskCount());

        // 清除任务
        manager.clearTasks();
        assertEquals(0, manager.getTaskCount());
    }

    @Test
    void testTaskSuccessRate() {
        // 创建任务
        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("success-rate-task")
                .priority(1)
                .dataLoader(HashMap::new)
                .build();

        // 设置任务状态
        task.setItemCount(10);
        task.setLoadedCount(8);
        task.setFailedCount(2);

        // 验证成功率
        assertEquals(0.8, task.getSuccessRate(), 0.01);
    }
}
