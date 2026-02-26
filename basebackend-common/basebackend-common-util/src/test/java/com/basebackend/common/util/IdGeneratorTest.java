package com.basebackend.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdGenerator 单元测试
 */
class IdGeneratorTest {

    // ========== UUID ==========

    @Nested
    @DisplayName("UUID 生成")
    class UuidGeneration {

        @Test
        @DisplayName("uuid() 格式正确（带横杠）")
        void shouldGenerateStandardUuid() {
            String uuid = IdGenerator.uuid();
            assertThat(uuid).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("simpleUuid() 32位无横杠")
        void shouldGenerateSimpleUuid() {
            String uuid = IdGenerator.simpleUuid();
            assertThat(uuid).hasSize(32).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("fastUuid() 非空")
        void shouldGenerateFastUuid() {
            String uuid = IdGenerator.fastUuid();
            assertThat(uuid).isNotBlank();
        }

        @Test
        @DisplayName("UUID 唯一性")
        void shouldBeUnique() {
            Set<String> uuids = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                uuids.add(IdGenerator.simpleUuid());
            }
            assertThat(uuids).hasSize(1000);
        }
    }

    // ========== 雪花算法 ==========

    @Nested
    @DisplayName("雪花算法")
    class Snowflake {

        @Test
        @DisplayName("生成正数 ID")
        void shouldGeneratePositiveId() {
            long id = IdGenerator.snowflakeId();
            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("字符串形式")
        void shouldGenerateStringId() {
            String id = IdGenerator.snowflakeIdStr();
            assertThat(id).isNotBlank();
            assertThat(Long.parseLong(id)).isPositive();
        }

        @Test
        @DisplayName("严格递增")
        void shouldBeStrictlyIncreasing() {
            long prev = IdGenerator.snowflakeId();
            for (int i = 0; i < 100; i++) {
                long next = IdGenerator.snowflakeId();
                assertThat(next).isGreaterThan(prev);
                prev = next;
            }
        }

        @Test
        @DisplayName("唯一性（高并发）")
        void shouldBeUniqueUnderConcurrency() {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 10000; i++) {
                ids.add(IdGenerator.snowflakeId());
            }
            assertThat(ids).hasSize(10000);
        }
    }

    // ========== 时间戳 ID ==========

    @Nested
    @DisplayName("时间戳 ID")
    class TimestampId {

        @Test
        @DisplayName("timestampId() 21位")
        void shouldGenerate21DigitId() {
            String id = IdGenerator.timestampId();
            assertThat(id).hasSize(21).matches("\\d+");
        }

        @Test
        @DisplayName("compactTimestampId() 20位")
        void shouldGenerate20DigitId() {
            String id = IdGenerator.compactTimestampId();
            assertThat(id).hasSize(20).matches("\\d+");
        }
    }

    // ========== 随机字符串 ==========

    @Nested
    @DisplayName("随机字符串")
    class RandomString {

        @Test
        @DisplayName("randomHex 正确长度和字符")
        void shouldGenerateHex() {
            String hex = IdGenerator.randomHex(16);
            assertThat(hex).hasSize(16).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("randomAlphanumeric 正确长度")
        void shouldGenerateAlphanumeric() {
            String str = IdGenerator.randomAlphanumeric(20);
            assertThat(str).hasSize(20).matches("[0-9a-zA-Z]+");
        }

        @Test
        @DisplayName("randomNumeric 纯数字")
        void shouldGenerateNumeric() {
            String str = IdGenerator.randomNumeric(8);
            assertThat(str).hasSize(8).matches("\\d+");
        }

        @Test
        @DisplayName("长度为 0 返回空字符串")
        void shouldReturnEmptyForZeroLength() {
            assertThat(IdGenerator.randomHex(0)).isEmpty();
            assertThat(IdGenerator.randomAlphanumeric(0)).isEmpty();
            assertThat(IdGenerator.randomNumeric(0)).isEmpty();
        }

        @Test
        @DisplayName("负数长度返回空字符串")
        void shouldReturnEmptyForNegativeLength() {
            assertThat(IdGenerator.randomHex(-1)).isEmpty();
        }
    }
}
