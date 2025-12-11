package com.basebackend.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SnowflakeIdGenerator 单元测试
 */
@DisplayName("SnowflakeIdGenerator 测试")
class SnowflakeIdGeneratorTest {

    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator(1);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("有效workerId构造成功")
        void shouldConstructWithValidWorkerId() {
            assertDoesNotThrow(() -> new SnowflakeIdGenerator(0));
            assertDoesNotThrow(() -> new SnowflakeIdGenerator(1));
            assertDoesNotThrow(() -> new SnowflakeIdGenerator(512));
            assertDoesNotThrow(() -> new SnowflakeIdGenerator(1023));
        }

        @Test
        @DisplayName("无效workerId抛出异常")
        void shouldThrowForInvalidWorkerId() {
            assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
            assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024));
            assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(Long.MAX_VALUE));
        }

        @Test
        @DisplayName("无参构造函数自动生成workerId")
        void shouldAutoGenerateWorkerIdWithNoArgConstructor() {
            SnowflakeIdGenerator gen = new SnowflakeIdGenerator();
            assertNotNull(gen);
            // 应该能正常生成ID
            assertTrue(gen.generateId() > 0);
        }
    }

    @Nested
    @DisplayName("ID生成测试")
    class IdGenerationTests {

        @Test
        @DisplayName("生成正数ID")
        void shouldGeneratePositiveId() {
            long id = generator.generateId();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("ID有序递增")
        void idsShouldBeOrdered() {
            long[] ids = new long[100];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = generator.generateId();
            }

            for (int i = 1; i < ids.length; i++) {
                assertTrue(ids[i] > ids[i - 1],
                        "ID should be increasing: " + ids[i - 1] + " vs " + ids[i]);
            }
        }

        @Test
        @DisplayName("ID全局唯一")
        void idsShouldBeUnique() {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 10000; i++) {
                assertTrue(ids.add(generator.generateId()), "Duplicate ID found at iteration " + i);
            }
        }
    }

    @Nested
    @DisplayName("静态方法测试")
    class StaticMethodTests {

        @Test
        @DisplayName("nextId方法正常工作")
        void nextIdShouldWork() {
            long id = SnowflakeIdGenerator.nextId();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("nextIdStr返回有效字符串")
        void nextIdStrShouldReturnValidString() {
            String idStr = SnowflakeIdGenerator.nextIdStr();
            assertNotNull(idStr);
            assertDoesNotThrow(() -> Long.parseLong(idStr));
        }

        @Test
        @DisplayName("setWorkerId配置生效")
        void setWorkerIdShouldWork() {
            SnowflakeIdGenerator.setWorkerId(100);
            assertEquals(100, SnowflakeIdGenerator.getWorkerId());
        }

        @Test
        @DisplayName("setWorkerId无效值抛出异常")
        void setWorkerIdShouldThrowForInvalidValue() {
            assertThrows(IllegalArgumentException.class, () -> SnowflakeIdGenerator.setWorkerId(-1));
            assertThrows(IllegalArgumentException.class, () -> SnowflakeIdGenerator.setWorkerId(1024));
        }
    }

    @Nested
    @DisplayName("ID解析测试")
    class IdParsingTests {

        @Test
        @DisplayName("解析时间戳")
        void shouldParseTimestamp() {
            long before = System.currentTimeMillis();
            long id = generator.generateId();
            long after = System.currentTimeMillis();

            long timestamp = SnowflakeIdGenerator.parseTimestamp(id);
            assertTrue(timestamp >= before && timestamp <= after);
        }

        @Test
        @DisplayName("解析workerId")
        void shouldParseWorkerId() {
            SnowflakeIdGenerator gen = new SnowflakeIdGenerator(123);
            long id = gen.generateId();

            assertEquals(123, SnowflakeIdGenerator.parseWorkerId(id));
        }

        @Test
        @DisplayName("解析序列号")
        void shouldParseSequence() {
            long id = generator.generateId();
            long sequence = SnowflakeIdGenerator.parseSequence(id);
            assertTrue(sequence >= 0 && sequence <= 4095);
        }
    }

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("多线程生成唯一ID")
        void shouldBeUniqueUnderConcurrentAccess() throws InterruptedException {
            int threadCount = 10;
            int idsPerThread = 10000;
            Set<Long> allIds = ConcurrentHashMap.newKeySet();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 等待所有线程就绪
                        for (int j = 0; j < idsPerThread; j++) {
                            allIds.add(generator.generateId());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 释放所有线程
            endLatch.await();
            executor.shutdown();

            assertEquals(threadCount * idsPerThread, allIds.size(),
                    "Should have no duplicate IDs");
        }

        @Test
        @DisplayName("不同workerId的生成器应生成不同ID")
        void differentWorkersShouldGenerateDifferentIds() throws InterruptedException {
            int workerCount = 5;
            int idsPerWorker = 1000;
            Set<Long> allIds = ConcurrentHashMap.newKeySet();
            CountDownLatch latch = new CountDownLatch(workerCount);
            ExecutorService executor = Executors.newFixedThreadPool(workerCount);

            for (int i = 0; i < workerCount; i++) {
                final int workerId = i;
                executor.submit(() -> {
                    SnowflakeIdGenerator gen = new SnowflakeIdGenerator(workerId);
                    try {
                        for (int j = 0; j < idsPerWorker; j++) {
                            allIds.add(gen.generateId());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertEquals(workerCount * idsPerWorker, allIds.size());
        }
    }
}
