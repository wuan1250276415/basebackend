package com.basebackend.cache.integration;

import com.basebackend.cache.config.CacheAutoConfiguration;
import com.basebackend.cache.service.RedisService;
import com.basebackend.cache.warming.CacheWarmingExecutor;
import com.basebackend.cache.warming.CacheWarmingManager;
import com.basebackend.cache.warming.CacheWarmingTask;
import com.basebackend.cache.warming.WarmingProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存预热真实集成测试
 * 使用真实的 Redis 测试缓存预热流程
 */
@SpringBootTest(classes = {CacheAutoConfiguration.class})
@Testcontainers
class CacheWarmingRealIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("basebackend.cache.enabled", () -> "true");
        registry.add("basebackend.cache.warming.enabled", () -> "true");
    }

    @Autowired
    private CacheWarmingManager warmingManager;

    @Autowired
    private CacheWarmingExecutor warmingExecutor;

    @Autowired
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        // 清理 Redis 和预热任务
        redisService.deleteByPattern("test:*");
        warmingManager.clearTasks();
    }

    @Test
    void testWarmingWithRealRedis() {
        // 创建预热任务
        Map<String, Object> testData = new HashMap<>();
        testData.put("test:user:1", "User 1");
        testData.put("test:user:2", "User 2");
        testData.put("test:user:3", "User 3");

        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("test-users")
                .priority(1)
                .dataLoader(() -> testData)
                .ttl(Duration.ofHours(1))
                .async(false)
                .build();

        // 执行预热
        boolean success = warmingExecutor.execute(task);

        // 验证预热成功
        assertTrue(success);
        assertEquals(CacheWarmingTask.TaskStatus.SUCCESS, task.getStatus());

        // 验证数据已加载到 Redis
        assertEquals("User 1", redisService.get("test:user:1"));
        assertEquals("User 2", redisService.get("test:user:2"));
        assertEquals("User 3", redisService.get("test:user:3"));
    }

    @Test
    void testWarmingWithPriority() {
        // 创建多个不同优先级的任务
        CacheWarmingTask highPriorityTask = CacheWarmingTask.builder()
                .name("high-priority")
                .priority(1)
                .dataLoader(() -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("test:high:1", "High 1");
                    return data;
                })
                .ttl(Duration.ofHours(1))
                .build();

        CacheWarmingTask lowPriorityTask = CacheWarmingTask.builder()
                .name("low-priority")
                .priority(10)
                .dataLoader(() -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("test:low:1", "Low 1");
                    return data;
                })
                .ttl(Duration.ofHours(1))
                .build();

        // 注册任务
        warmingManager.registerWarmingTask(lowPriorityTask);
        warmingManager.registerWarmingTask(highPriorityTask);

        // 执行所有任务
        warmingManager.executeWarmingTasks();

        // 验证所有数据都已加载
        assertEquals("High 1", redisService.get("test:high:1"));
        assertEquals("Low 1", redisService.get("test:low:1"));

        // 验证进度
        WarmingProgress progress = warmingManager.getProgress();
        assertEquals(2, progress.getTotalTasks());
        assertEquals(2, progress.getCompletedTasks());
        assertTrue(progress.isCompleted());
    }

    @Test
    void testWarmingWithLargeDataset() {
        // 测试大数据集预热
        Map<String, Object> largeData = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeData.put("test:large:" + i, "Value " + i);
        }

        CacheWarmingTask task = CacheWarmingTask.builder()
                .name("large-dataset")
                .priority(1)
                .dataLoader(() -> largeData)
                .ttl(Duration.ofHours(1))
                .build();

        // 执行预热
        boolean success = warmingExecutor.execute(task);

        // 验证
        assertTrue(success);
        assertEquals(100, task.getItemCount());
        assertEquals(100, task.getLoadedCount());

        // 抽样验证数据
        assertEquals("Value 0", redisService.get("test:large:0"));
        assertEquals("Value 50", redisService.get("test:large:50"));
        assertEquals("Value 99", redisService.get("test:large:99"));
    }
}
