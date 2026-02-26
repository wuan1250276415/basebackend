package com.basebackend.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResult 分页结果单元测试
 */
class PageResultTest {

    // ========== 工厂方法 ==========

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("of(records, total, current, size) Long 版")
        void shouldCreateWithLongParams() {
            var result = PageResult.of(List.of("a", "b", "c"), 100L, 1L, 10L);
            assertThat(result.getCurrent()).isEqualTo(1L);
            assertThat(result.getSize()).isEqualTo(10L);
            assertThat(result.getTotal()).isEqualTo(100L);
            assertThat(result.getPages()).isEqualTo(10L);
            assertThat(result.getRecords()).hasSize(3);
        }

        @Test
        @DisplayName("of(records, total, current, size) int 版")
        void shouldCreateWithIntParams() {
            var result = PageResult.of(List.of(1, 2), 25, 3, 10);
            assertThat(result.getCurrent()).isEqualTo(3L);
            assertThat(result.getSize()).isEqualTo(10L);
            assertThat(result.getTotal()).isEqualTo(25L);
            assertThat(result.getPages()).isEqualTo(3L); // ceil(25/10) = 3
        }

        @Test
        @DisplayName("empty() 创建空分页")
        void shouldCreateEmpty() {
            PageResult<String> result = PageResult.empty();
            assertThat(result.getCurrent()).isEqualTo(1L);
            assertThat(result.getSize()).isEqualTo(10L);
            assertThat(result.getTotal()).isEqualTo(0L);
            assertThat(result.getPages()).isEqualTo(0L);
            assertThat(result.getRecords()).isEmpty();
        }

        @Test
        @DisplayName("empty(current, size) 创建指定参数的空分页")
        void shouldCreateEmptyWithParams() {
            PageResult<String> result = PageResult.empty(2, 20);
            assertThat(result.getCurrent()).isEqualTo(2L);
            assertThat(result.getSize()).isEqualTo(20L);
            assertThat(result.getTotal()).isEqualTo(0L);
        }

        @Test
        @DisplayName("records 为 null 时自动设为空列表")
        void shouldHandleNullRecords() {
            var result = PageResult.of(null, 0L, 1L, 10L);
            assertThat(result.getRecords()).isNotNull().isEmpty();
        }
    }

    // ========== 分页计算 ==========

    @Nested
    @DisplayName("分页计算")
    class PageCalculation {

        @Test
        @DisplayName("总页数正确计算（整除）")
        void shouldCalculatePagesExactDivision() {
            var result = PageResult.of(List.of(), 100L, 1L, 10L);
            assertThat(result.getPages()).isEqualTo(10L);
        }

        @Test
        @DisplayName("总页数正确计算（非整除向上取整）")
        void shouldCalculatePagesCeilDivision() {
            var result = PageResult.of(List.of(), 101L, 1L, 10L);
            assertThat(result.getPages()).isEqualTo(11L);
        }

        @Test
        @DisplayName("total=0 页数为 0")
        void shouldReturnZeroPagesForZeroTotal() {
            var result = PageResult.of(List.of(), 0L, 1L, 10L);
            assertThat(result.getPages()).isEqualTo(0L);
        }

        @Test
        @DisplayName("total=1, size=1 → pages=1")
        void shouldHandleSingleRecord() {
            var result = PageResult.of(List.of("x"), 1L, 1L, 1L);
            assertThat(result.getPages()).isEqualTo(1L);
        }
    }

    // ========== 便捷方法 ==========

    @Nested
    @DisplayName("便捷方法")
    class ConvenienceMethods {

        @Test
        @DisplayName("hasRecords / isEmpty")
        void shouldCheckRecords() {
            var withData = PageResult.of(List.of("a"), 1L, 1L, 10L);
            assertThat(withData.hasRecords()).isTrue();
            assertThat(withData.isEmpty()).isFalse();

            var empty = PageResult.empty();
            assertThat(empty.hasRecords()).isFalse();
            assertThat(empty.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("hasNext / hasPrevious")
        void shouldCheckNavigation() {
            var firstPage = PageResult.of(List.of("a"), 30L, 1L, 10L);
            assertThat(firstPage.hasNext()).isTrue();
            assertThat(firstPage.hasPrevious()).isFalse();

            var middlePage = PageResult.of(List.of("a"), 30L, 2L, 10L);
            assertThat(middlePage.hasNext()).isTrue();
            assertThat(middlePage.hasPrevious()).isTrue();

            var lastPage = PageResult.of(List.of("a"), 30L, 3L, 10L);
            assertThat(lastPage.hasNext()).isFalse();
            assertThat(lastPage.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("isFirst / isLast")
        void shouldCheckBoundary() {
            var first = PageResult.of(List.of("a"), 30L, 1L, 10L);
            assertThat(first.isFirst()).isTrue();
            assertThat(first.isLast()).isFalse();

            var last = PageResult.of(List.of("a"), 30L, 3L, 10L);
            assertThat(last.isFirst()).isFalse();
            assertThat(last.isLast()).isTrue();
        }

        @Test
        @DisplayName("单页时 isFirst && isLast")
        void shouldBeBothFirstAndLastForSinglePage() {
            var single = PageResult.of(List.of("a"), 5L, 1L, 10L);
            assertThat(single.isFirst()).isTrue();
            assertThat(single.isLast()).isTrue();
        }

        @Test
        @DisplayName("getRecordCount")
        void shouldReturnRecordCount() {
            var result = PageResult.of(List.of("a", "b", "c"), 100L, 1L, 10L);
            assertThat(result.getRecordCount()).isEqualTo(3);

            var empty = PageResult.empty();
            assertThat(empty.getRecordCount()).isEqualTo(0);
        }
    }

    // ========== map 转换 ==========

    @Nested
    @DisplayName("map 数据转换")
    class MapTransform {

        @Test
        @DisplayName("类型转换保留分页信息")
        void shouldTransformAndPreservePagination() {
            var original = PageResult.of(List.of(1, 2, 3), 100L, 1L, 10L);
            PageResult<String> mapped = original.map(String::valueOf);

            assertThat(mapped.getRecords()).containsExactly("1", "2", "3");
            assertThat(mapped.getTotal()).isEqualTo(100L);
            assertThat(mapped.getCurrent()).isEqualTo(1L);
            assertThat(mapped.getSize()).isEqualTo(10L);
            assertThat(mapped.getPages()).isEqualTo(10L);
        }

        @Test
        @DisplayName("空列表 map 返回空列表")
        void shouldMapEmptyList() {
            PageResult<Integer> empty = PageResult.empty();
            PageResult<String> mapped = empty.map(String::valueOf);
            assertThat(mapped.getRecords()).isEmpty();
            assertThat(mapped.getTotal()).isEqualTo(0L);
        }
    }
}
