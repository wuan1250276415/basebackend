package com.basebackend.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdGenerator 单元测试
 */
@DisplayName("IdGenerator 测试")
class IdGeneratorTest {

    @Nested
    @DisplayName("UUID生成测试")
    class UuidTests {

        @Test
        @DisplayName("生成标准UUID")
        void shouldGenerateStandardUuid() {
            String uuid = IdGenerator.uuid();
            assertNotNull(uuid);
            assertEquals(36, uuid.length());
            assertTrue(uuid.contains("-"));
        }

        @Test
        @DisplayName("生成简化UUID")
        void shouldGenerateSimpleUuid() {
            String uuid = IdGenerator.simpleUuid();
            assertNotNull(uuid);
            assertEquals(32, uuid.length());
            assertFalse(uuid.contains("-"));
        }

        @Test
        @DisplayName("生成快速UUID")
        void shouldGenerateFastUuid() {
            String uuid = IdGenerator.fastUuid();
            assertNotNull(uuid);
            // 快速UUID长度可能不固定（16进制）
            assertTrue(uuid.length() >= 16);
        }

        @RepeatedTest(100)
        @DisplayName("UUID唯一性测试")
        void uuidShouldBeUnique() {
            Set<String> uuids = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                assertTrue(uuids.add(IdGenerator.uuid()));
            }
        }
    }

    @Nested
    @DisplayName("雪花算法测试")
    class SnowflakeTests {

        @Test
        @DisplayName("生成雪花ID")
        void shouldGenerateSnowflakeId() {
            long id = IdGenerator.snowflakeId();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("生成雪花ID字符串")
        void shouldGenerateSnowflakeIdStr() {
            String idStr = IdGenerator.snowflakeIdStr();
            assertNotNull(idStr);
            assertDoesNotThrow(() -> Long.parseLong(idStr));
        }

        @Test
        @DisplayName("雪花ID有序性")
        void snowflakeIdShouldBeOrdered() {
            long id1 = IdGenerator.snowflakeId();
            long id2 = IdGenerator.snowflakeId();
            long id3 = IdGenerator.snowflakeId();

            assertTrue(id1 < id2);
            assertTrue(id2 < id3);
        }

        @RepeatedTest(100)
        @DisplayName("雪花ID唯一性测试")
        void snowflakeIdShouldBeUnique() {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                assertTrue(ids.add(IdGenerator.snowflakeId()));
            }
        }

        @Test
        @DisplayName("高并发唯一性测试")
        void shouldBeUniqueUnderHighConcurrency() throws InterruptedException {
            int threadCount = 10;
            int idsPerThread = 1000;
            Set<Long> allIds = ConcurrentHashMap.newKeySet();
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < idsPerThread; j++) {
                            allIds.add(IdGenerator.snowflakeId());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // 验证没有重复ID
            assertEquals(threadCount * idsPerThread, allIds.size());
        }
    }

    @Nested
    @DisplayName("时间戳ID测试")
    class TimestampIdTests {

        @Test
        @DisplayName("生成时间戳ID")
        void shouldGenerateTimestampId() {
            String id = IdGenerator.timestampId();
            assertNotNull(id);
            assertEquals(21, id.length());
        }

        @Test
        @DisplayName("生成紧凑时间戳ID")
        void shouldGenerateCompactTimestampId() {
            String id = IdGenerator.compactTimestampId();
            assertNotNull(id);
            assertEquals(20, id.length());
        }

        @Test
        @DisplayName("时间戳ID格式验证")
        void timestampIdShouldHaveCorrectFormat() {
            String id = IdGenerator.timestampId();
            // 验证前14位为日期时间格式
            String datePart = id.substring(0, 8);
            assertDoesNotThrow(() -> Integer.parseInt(datePart));
        }
    }

    @Nested
    @DisplayName("随机字符串测试")
    class RandomStringTests {

        @Test
        @DisplayName("生成随机十六进制字符串")
        void shouldGenerateRandomHex() {
            String hex = IdGenerator.randomHex(16);
            assertNotNull(hex);
            assertEquals(16, hex.length());
            assertTrue(hex.matches("[0-9a-f]+"));
        }

        @Test
        @DisplayName("生成随机字母数字字符串")
        void shouldGenerateRandomAlphanumeric() {
            String str = IdGenerator.randomAlphanumeric(20);
            assertNotNull(str);
            assertEquals(20, str.length());
            assertTrue(str.matches("[0-9a-zA-Z]+"));
        }

        @Test
        @DisplayName("生成随机数字字符串")
        void shouldGenerateRandomNumeric() {
            String str = IdGenerator.randomNumeric(10);
            assertNotNull(str);
            assertEquals(10, str.length());
            assertTrue(str.matches("[0-9]+"));
        }

        @Test
        @DisplayName("长度为0返回空字符串")
        void shouldReturnEmptyForZeroLength() {
            assertEquals("", IdGenerator.randomHex(0));
            assertEquals("", IdGenerator.randomAlphanumeric(0));
            assertEquals("", IdGenerator.randomNumeric(0));
        }

        @Test
        @DisplayName("负数长度返回空字符串")
        void shouldReturnEmptyForNegativeLength() {
            assertEquals("", IdGenerator.randomHex(-1));
            assertEquals("", IdGenerator.randomAlphanumeric(-1));
            assertEquals("", IdGenerator.randomNumeric(-1));
        }
    }
}
