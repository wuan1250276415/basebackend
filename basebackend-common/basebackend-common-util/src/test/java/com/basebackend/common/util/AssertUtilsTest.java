package com.basebackend.common.util;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AssertUtils 业务断言工具类单元测试
 */
class AssertUtilsTest {

    // ========== notNull ==========

    @Nested
    @DisplayName("notNull")
    class NotNull {

        @Test
        @DisplayName("非 null 通过")
        void shouldPassForNonNull() {
            assertThatCode(() -> AssertUtils.notNull("hello", "不能为空")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null 抛出 BusinessException")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> AssertUtils.notNull(null, "不能为空"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("ErrorCode 版本")
        void shouldThrowWithErrorCode() {
            assertThatThrownBy(() -> AssertUtils.notNull(null, CommonErrorCode.DATA_NOT_FOUND))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("ErrorCode + 自定义消息")
        void shouldThrowWithErrorCodeAndMessage() {
            assertThatThrownBy(() -> AssertUtils.notNull(null, CommonErrorCode.DATA_NOT_FOUND, "用户不存在"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("Supplier 延迟消息")
        void shouldThrowWithSupplier() {
            assertThatThrownBy(() -> AssertUtils.notNull(null, () -> "延迟消息"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("延迟消息");
        }
    }

    // ========== notEmpty (String) ==========

    @Nested
    @DisplayName("notEmpty (String)")
    class NotEmptyString {

        @Test
        @DisplayName("非空字符串通过")
        void shouldPassForNonEmpty() {
            assertThatCode(() -> AssertUtils.notEmpty("hello", "不能为空")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null 字符串抛出异常")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> AssertUtils.notEmpty((String) null, "不能为空"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("空字符串抛出异常")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> AssertUtils.notEmpty("", "不能为空"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== notBlank ==========

    @Nested
    @DisplayName("notBlank")
    class NotBlank {

        @Test
        @DisplayName("非空白通过")
        void shouldPassForNonBlank() {
            assertThatCode(() -> AssertUtils.notBlank("hello", "不能为空白")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空白字符串抛出异常")
        void shouldThrowForBlank() {
            assertThatThrownBy(() -> AssertUtils.notBlank("   ", "不能为空白"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== notEmpty (Collection) ==========

    @Nested
    @DisplayName("notEmpty (Collection)")
    class NotEmptyCollection {

        @Test
        @DisplayName("非空集合通过")
        void shouldPassForNonEmpty() {
            assertThatCode(() -> AssertUtils.notEmpty(List.of("a"), "集合不能为空")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null 集合抛出异常")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> AssertUtils.notEmpty((java.util.Collection<?>) null, "集合不能为空"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("空集合抛出异常")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> AssertUtils.notEmpty(Collections.emptyList(), "集合不能为空"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== notEmpty (Map) ==========

    @Nested
    @DisplayName("notEmpty (Map)")
    class NotEmptyMap {

        @Test
        @DisplayName("非空 Map 通过")
        void shouldPassForNonEmpty() {
            assertThatCode(() -> AssertUtils.notEmpty(Map.of("k", "v"), "Map不能为空")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空 Map 抛出异常")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> AssertUtils.notEmpty(Collections.emptyMap(), "Map不能为空"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== notEmpty (Array) ==========

    @Nested
    @DisplayName("notEmpty (Array)")
    class NotEmptyArray {

        @Test
        @DisplayName("非空数组通过")
        void shouldPassForNonEmpty() {
            assertThatCode(() -> AssertUtils.notEmpty(new Object[]{"a"}, "数组不能为空")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空数组抛出异常")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> AssertUtils.notEmpty(new Object[0], "数组不能为空"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== isTrue / isFalse ==========

    @Nested
    @DisplayName("isTrue / isFalse")
    class BooleanAssert {

        @Test
        @DisplayName("isTrue true 通过")
        void shouldPassForTrue() {
            assertThatCode(() -> AssertUtils.isTrue(true, "应为 true")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isTrue false 抛出异常")
        void shouldThrowForFalse() {
            assertThatThrownBy(() -> AssertUtils.isTrue(false, "应为 true"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("isFalse false 通过")
        void shouldPassForFalseValue() {
            assertThatCode(() -> AssertUtils.isFalse(false, "应为 false")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isFalse true 抛出异常")
        void shouldThrowForTrueValue() {
            assertThatThrownBy(() -> AssertUtils.isFalse(true, "应为 false"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== state ==========

    @Nested
    @DisplayName("state")
    class StateAssert {

        @Test
        @DisplayName("有效状态通过")
        void shouldPassForValidState() {
            assertThatCode(() -> AssertUtils.state(true, "状态无效")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("无效状态抛出异常")
        void shouldThrowForInvalidState() {
            assertThatThrownBy(() -> AssertUtils.state(false, "状态无效"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("状态无效");
        }

        @Test
        @DisplayName("Supplier 延迟消息")
        void shouldThrowWithSupplier() {
            assertThatThrownBy(() -> AssertUtils.state(false, () -> "延迟状态消息"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("延迟状态消息");
        }
    }

    // ========== 数值断言 ==========

    @Nested
    @DisplayName("数值断言")
    class NumericAssert {

        @Test
        @DisplayName("isGreaterThan 正常通过")
        void shouldPassGreaterThan() {
            assertThatCode(() -> AssertUtils.isGreaterThan(10, 5, "应大于5")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isGreaterThan 等于时抛异常")
        void shouldThrowGreaterThanEqual() {
            assertThatThrownBy(() -> AssertUtils.isGreaterThan(5, 5, "应大于5"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("isBetween 正常范围通过")
        void shouldPassBetween() {
            assertThatCode(() -> AssertUtils.isBetween(5, 1, 10, "应在1-10之间")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isBetween 边界值通过")
        void shouldPassBetweenBoundary() {
            assertThatCode(() -> AssertUtils.isBetween(1, 1, 10, "应在1-10之间")).doesNotThrowAnyException();
            assertThatCode(() -> AssertUtils.isBetween(10, 1, 10, "应在1-10之间")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isBetween 超出范围抛异常")
        void shouldThrowBetweenOutOfRange() {
            assertThatThrownBy(() -> AssertUtils.isBetween(0, 1, 10, "应在1-10之间"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== equals / notEquals ==========

    @Nested
    @DisplayName("equals / notEquals")
    class EqualityAssert {

        @Test
        @DisplayName("equals 相等通过")
        void shouldPassForEqual() {
            assertThatCode(() -> AssertUtils.equals("a", "a", "应相等")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("equals 不相等抛异常")
        void shouldThrowForNotEqual() {
            assertThatThrownBy(() -> AssertUtils.equals("a", "b", "应相等"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("equals null == null 通过")
        void shouldPassForBothNull() {
            assertThatCode(() -> AssertUtils.equals(null, null, "应相等")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("notEquals 不相等通过")
        void shouldPassForNotEqual() {
            assertThatCode(() -> AssertUtils.notEquals("a", "b", "不应相等")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("notEquals 相等抛异常")
        void shouldThrowForEqual() {
            assertThatThrownBy(() -> AssertUtils.notEquals("a", "a", "不应相等"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
