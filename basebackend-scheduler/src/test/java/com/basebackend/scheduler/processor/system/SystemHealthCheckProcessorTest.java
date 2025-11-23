package com.basebackend.scheduler.processor.system;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 系统健康检查处理器测试。
 */
class SystemHealthCheckProcessorTest {

    private DataSource mockDataSource;
    private RedisTemplate<String, Object> mockRedisTemplate;
    private SystemHealthCheckProcessor processor;

    @BeforeEach
    void setUp() throws SQLException {
        mockDataSource = mock(DataSource.class);
        mockRedisTemplate = mock(RedisTemplate.class);

        // Mock数据库连接
        Connection mockConnection = mock(Connection.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(2)).thenReturn(true);

        // Mock Redis连接
        when(mockRedisTemplate.hasKey("health:check")).thenReturn(true);

        processor = new SystemHealthCheckProcessor(mockDataSource, mockRedisTemplate);
    }

    @Test
    void testProcessorName() {
        assertEquals("system-health-check", processor.name());
    }

    @Test
    void testSuccessfulHealthCheck() {
        Map<String, Object> params = new HashMap<>();
        params.put("cpuUsageOverride", 0.5);      // 模拟50%CPU
        params.put("memoryUsageOverride", 0.5);   // 模拟50%内存
        params.put("diskFreeRatioOverride", 0.8); // 模拟80%磁盘剩余

        TaskContext context = TaskContext.builder("health-check-test")
                .parameters(params)
                .build();
        TaskResult result = processor.process(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());

        // 验证输出包含所有必要字段
        assertNotNull(result.getOutput().get("cpuUsage"));
        assertNotNull(result.getOutput().get("memoryUsage"));
        assertNotNull(result.getOutput().get("diskFreeRatio"));
        assertNotNull(result.getOutput().get("dbHealthy"));
        assertNotNull(result.getOutput().get("redisHealthy"));
        assertEquals(true, result.getOutput().get("dbHealthy"));
        assertEquals(true, result.getOutput().get("redisHealthy"));
    }

    @Test
    void testCustomThresholds() {
        Map<String, Object> params = new HashMap<>();
        params.put("cpuThreshold", 0.5);      // 50% CPU阈值
        params.put("memoryThreshold", 0.6);   // 60% 内存阈值
        params.put("diskThreshold", 0.2);     // 20% 磁盘阈值

        TaskContext context = TaskContext.builder("health-check-test")
                .parameters(params)
                .build();

        TaskResult result = processor.process(context);

        // 应该包含自定义阈值
        assertEquals(0.5, result.getOutput().get("cpuThreshold"));
        assertEquals(0.6, result.getOutput().get("memoryThreshold"));
        assertEquals(0.2, result.getOutput().get("diskThreshold"));
    }

    @Test
    void testDatabaseFailure() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isValid(2)).thenReturn(false);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);

        TaskContext context = TaskContext.builder("health-check-test").build();
        TaskResult result = processor.process(context);

        assertFalse(result.isSuccess());
        assertEquals(false, result.getOutput().get("dbHealthy"));
        assertEquals("System health threshold violated", result.getErrorMessage());
    }

    @Test
    void testRedisFailure() {
        when(mockRedisTemplate.hasKey("health:check")).thenReturn(false);

        TaskContext context = TaskContext.builder("health-check-test").build();
        TaskResult result = processor.process(context);

        assertFalse(result.isSuccess());
        assertEquals(false, result.getOutput().get("redisHealthy"));
        assertEquals("System health threshold violated", result.getErrorMessage());
    }

    @Test
    void testHighCpuUsage() {
        Map<String, Object> params = new HashMap<>();
        params.put("cpuThreshold", 0.01);  // 设置很低的阈值
        params.put("cpuUsageOverride", 0.5);  // 测试用：模拟50%CPU使用率

        TaskContext context = TaskContext.builder("health-check-test")
                .parameters(params)
                .build();

        TaskResult result = processor.process(context);

        // CPU使用率会高于阈值，导致健康检查失败
        assertFalse(result.isSuccess());
    }

    @Test
    void testHighMemoryUsage() {
        Map<String, Object> params = new HashMap<>();
        params.put("memoryThreshold", 0.01);  // 设置很低的阈值

        TaskContext context = TaskContext.builder("health-check-test")
                .parameters(params)
                .build();

        TaskResult result = processor.process(context);

        // 内存使用率会高于阈值，导致健康检查失败
        assertFalse(result.isSuccess());
    }

    @Test
    void testLowDiskSpace() {
        Map<String, Object> params = new HashMap<>();
        params.put("diskThreshold", 0.99);  // 设置很高的阈值

        TaskContext context = TaskContext.builder("health-check-test")
                .parameters(params)
                .build();

        TaskResult result = processor.process(context);

        // 磁盘剩余空间会低于阈值，导致健康检查失败
        assertFalse(result.isSuccess());
    }

    @Test
    void testInvalidParameterValues() {
        Map<String, Object> params = new HashMap<>();
        params.put("cpuThreshold", "invalid");  // 无效值
        params.put("memoryThreshold", -1);      // 负值
        params.put("diskThreshold", 999);       // 超过1的值
        params.put("cpuUsageOverride", 0.5);      // 模拟正常CPU
        params.put("memoryUsageOverride", 0.5);   // 模拟正常内存
        params.put("diskFreeRatioOverride", 0.8); // 模拟正常磁盘

        TaskContext context = TaskContext.builder("health-check-test")
                .parameters(params)
                .build();

        // 应该使用默认值
        TaskResult result = processor.process(context);

        assertTrue(result.isSuccess());
        // 默认值应该是0.85, 0.85, 0.15
        assertEquals(0.85, result.getOutput().get("cpuThreshold"));
        assertEquals(0.85, result.getOutput().get("memoryThreshold"));
        assertEquals(0.15, result.getOutput().get("diskThreshold"));
    }

    @Test
    void testDatabaseException() throws SQLException {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("DB connection error"));

        TaskContext context = TaskContext.builder("health-check-test").build();
        TaskResult result = processor.process(context);

        assertFalse(result.isSuccess());
        assertEquals(false, result.getOutput().get("dbHealthy"));
    }

    @Test
    void testRedisException() {
        when(mockRedisTemplate.hasKey("health:check")).thenThrow(new RuntimeException("Redis error"));

        TaskContext context = TaskContext.builder("health-check-test").build();
        TaskResult result = processor.process(context);

        assertFalse(result.isSuccess());
        assertEquals(false, result.getOutput().get("redisHealthy"));
    }

    @Test
    void testIdempotentKey() {
        Map<String, Object> params = new HashMap<>();
        params.put("cpuUsageOverride", 0.5);      // 模拟50%CPU
        params.put("memoryUsageOverride", 0.5);   // 模拟50%内存
        params.put("diskFreeRatioOverride", 0.8); // 模拟80%磁盘剩余

        String idempotentKey = "health-check-idempotent";
        TaskContext context = TaskContext.builder("health-check-test")
                .idempotentKey(idempotentKey)
                .parameters(params)
                .build();

        TaskResult result = processor.process(context);

        assertTrue(result.isSuccess());
        assertTrue(result.isIdempotentHit());
        assertEquals(idempotentKey, result.getIdempotentKey());
    }
}
