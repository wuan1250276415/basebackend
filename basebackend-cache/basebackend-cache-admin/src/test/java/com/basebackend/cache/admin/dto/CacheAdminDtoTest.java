package com.basebackend.cache.admin.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cache Admin DTO 单元测试
 */
class CacheAdminDtoTest {

    // ========== CacheInfoDTO ==========

    @Nested
    @DisplayName("CacheInfoDTO")
    class CacheInfoTest {

        @Test
        @DisplayName("from(name, size, null stats) 统计为零")
        void shouldHandleNullStats() {
            var dto = CacheInfoDTO.from("user-cache", 100, null);
            assertThat(dto.name()).isEqualTo("user-cache");
            assertThat(dto.size()).isEqualTo(100);
            assertThat(dto.hitRate()).isEqualTo(0.0);
            assertThat(dto.hitCount()).isEqualTo(0);
            assertThat(dto.missCount()).isEqualTo(0);
            assertThat(dto.available()).isTrue();
        }

        @Test
        @DisplayName("unavailable 工厂方法")
        void shouldCreateUnavailable() {
            var dto = CacheInfoDTO.unavailable("broken-cache");
            assertThat(dto.name()).isEqualTo("broken-cache");
            assertThat(dto.size()).isEqualTo(-1);
            assertThat(dto.available()).isFalse();
            assertThat(dto.hitRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("record equals / hashCode")
        void shouldSupportEquality() {
            var dto1 = CacheInfoDTO.unavailable("cache-a");
            var dto2 = CacheInfoDTO.unavailable("cache-a");
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }
    }

    // ========== CacheDetailDTO ==========

    @Nested
    @DisplayName("CacheDetailDTO")
    class CacheDetailTest {

        @Test
        @DisplayName("from(name, size, null stats, keys) 统计为零")
        void shouldHandleNullStats() {
            var dto = CacheDetailDTO.from("detail-cache", 50, null, Set.of("key1", "key2"));
            assertThat(dto.name()).isEqualTo("detail-cache");
            assertThat(dto.size()).isEqualTo(50);
            assertThat(dto.hitRate()).isEqualTo(0.0);
            assertThat(dto.hitCount()).isEqualTo(0);
            assertThat(dto.missCount()).isEqualTo(0);
            assertThat(dto.totalCount()).isEqualTo(0);
            assertThat(dto.evictionCount()).isEqualTo(0);
            assertThat(dto.averageLoadTime()).isEqualTo(0);
            assertThat(dto.sampleKeys()).containsExactlyInAnyOrder("key1", "key2");
        }

        @Test
        @DisplayName("sampleKeys 为空集合")
        void shouldHandleEmptyKeys() {
            var dto = CacheDetailDTO.from("empty", 0, null, Set.of());
            assertThat(dto.sampleKeys()).isEmpty();
        }

        @Test
        @DisplayName("record toString 包含名称")
        void shouldIncludeNameInToString() {
            var dto = CacheDetailDTO.from("my-cache", 10, null, Set.of());
            assertThat(dto.toString()).contains("my-cache");
        }
    }
}
