package com.basebackend.common.util;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.enums.ErrorCode;
import com.basebackend.common.exception.BusinessException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 业务断言工具类
 * <p>
 * 提供常用的参数校验和业务规则断言方法。
 * 断言失败时抛出 {@link BusinessException}，支持 {@link ErrorCode} 枚举。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基本断言
 * AssertUtils.notNull(user, "用户不存在");
 * AssertUtils.notEmpty(username, "用户名不能为空");
 *
 * // 使用错误码
 * AssertUtils.notNull(user, CommonErrorCode.DATA_NOT_FOUND);
 * AssertUtils.notNull(user, CommonErrorCode.DATA_NOT_FOUND, "用户不存在");
 *
 * // 条件断言
 * AssertUtils.isTrue(user.isEnabled(), "用户已被禁用");
 * AssertUtils.state(order.canCancel(), "订单状态不允许取消");
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class AssertUtils {

    private AssertUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== 非空断言 ==========

    /**
     * 断言对象不为 null
     *
     * @param object  待检查对象
     * @param message 错误消息
     * @throws BusinessException 如果对象为 null
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言对象不为 null（使用错误码）
     *
     * @param object    待检查对象
     * @param errorCode 错误码枚举
     * @throws BusinessException 如果对象为 null
     */
    public static void notNull(Object object, ErrorCode errorCode) {
        if (object == null) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 断言对象不为 null（使用错误码和自定义消息）
     *
     * @param object    待检查对象
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @throws BusinessException 如果对象为 null
     */
    public static void notNull(Object object, ErrorCode errorCode, String message) {
        if (object == null) {
            throw new BusinessException(errorCode.getCode(), message);
        }
    }

    /**
     * 断言对象不为 null（延迟消息计算）
     *
     * @param object          待检查对象
     * @param messageSupplier 错误消息提供者
     * @throws BusinessException 如果对象为 null
     */
    public static void notNull(Object object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw BusinessException.paramError(nullSafeGet(messageSupplier));
        }
    }

    // ========== 字符串断言 ==========

    /**
     * 断言字符串不为空（不为 null 且不为空字符串）
     *
     * @param text    待检查字符串
     * @param message 错误消息
     * @throws BusinessException 如果字符串为空
     */
    public static void notEmpty(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言字符串不为空（使用错误码）
     *
     * @param text      待检查字符串
     * @param errorCode 错误码枚举
     * @throws BusinessException 如果字符串为空
     */
    public static void notEmpty(String text, ErrorCode errorCode) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 断言字符串不为空白（不为 null 且不全为空白字符）
     *
     * @param text    待检查字符串
     * @param message 错误消息
     * @throws BusinessException 如果字符串为空白
     */
    public static void notBlank(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言字符串不为空白（使用错误码）
     *
     * @param text      待检查字符串
     * @param errorCode 错误码枚举
     * @throws BusinessException 如果字符串为空白
     */
    public static void notBlank(String text, ErrorCode errorCode) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    // ========== 集合断言 ==========

    /**
     * 断言集合不为空
     *
     * @param collection 待检查集合
     * @param message    错误消息
     * @throws BusinessException 如果集合为空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言集合不为空（使用错误码）
     *
     * @param collection 待检查集合
     * @param errorCode  错误码枚举
     * @throws BusinessException 如果集合为空
     */
    public static void notEmpty(Collection<?> collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 断言 Map 不为空
     *
     * @param map     待检查 Map
     * @param message 错误消息
     * @throws BusinessException 如果 Map 为空
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言数组不为空
     *
     * @param array   待检查数组
     * @param message 错误消息
     * @throws BusinessException 如果数组为空
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw BusinessException.paramError(message);
        }
    }

    // ========== 条件断言 ==========

    /**
     * 断言条件为 true
     *
     * @param expression 条件表达式
     * @param message    错误消息
     * @throws BusinessException 如果条件为 false
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言条件为 true（使用错误码）
     *
     * @param expression 条件表达式
     * @param errorCode  错误码枚举
     * @throws BusinessException 如果条件为 false
     */
    public static void isTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 断言条件为 true（使用错误码和自定义消息）
     *
     * @param expression 条件表达式
     * @param errorCode  错误码枚举
     * @param message    自定义错误消息
     * @throws BusinessException 如果条件为 false
     */
    public static void isTrue(boolean expression, ErrorCode errorCode, String message) {
        if (!expression) {
            throw new BusinessException(errorCode.getCode(), message);
        }
    }

    /**
     * 断言条件为 false
     *
     * @param expression 条件表达式
     * @param message    错误消息
     * @throws BusinessException 如果条件为 true
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言条件为 false（使用错误码）
     *
     * @param expression 条件表达式
     * @param errorCode  错误码枚举
     * @throws BusinessException 如果条件为 true
     */
    public static void isFalse(boolean expression, ErrorCode errorCode) {
        if (expression) {
            throw new BusinessException(errorCode);
        }
    }

    // ========== 状态断言 ==========

    /**
     * 断言状态有效
     * <p>
     * 用于业务状态校验，断言失败时使用默认的业务异常错误码。
     * </p>
     *
     * @param state   状态表达式
     * @param message 错误消息
     * @throws BusinessException 如果状态无效
     */
    public static void state(boolean state, String message) {
        if (!state) {
            throw new BusinessException(CommonErrorCode.BUSINESS_RULE_VIOLATION.getCode(), message);
        }
    }

    /**
     * 断言状态有效（使用错误码）
     *
     * @param state     状态表达式
     * @param errorCode 错误码枚举
     * @throws BusinessException 如果状态无效
     */
    public static void state(boolean state, ErrorCode errorCode) {
        if (!state) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 断言状态有效（延迟消息计算）
     *
     * @param state           状态表达式
     * @param messageSupplier 错误消息提供者
     * @throws BusinessException 如果状态无效
     */
    public static void state(boolean state, Supplier<String> messageSupplier) {
        if (!state) {
            throw new BusinessException(CommonErrorCode.BUSINESS_RULE_VIOLATION.getCode(), nullSafeGet(messageSupplier));
        }
    }

    // ========== 数值断言 ==========

    /**
     * 断言数值大于指定值
     *
     * @param number  待检查数值
     * @param min     最小值（不包含）
     * @param message 错误消息
     * @throws BusinessException 如果数值不大于指定值
     */
    public static void isGreaterThan(long number, long min, String message) {
        if (number <= min) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言数值大于等于指定值
     *
     * @param number  待检查数值
     * @param min     最小值（包含）
     * @param message 错误消息
     * @throws BusinessException 如果数值小于指定值
     */
    public static void isGreaterThanOrEqual(long number, long min, String message) {
        if (number < min) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言数值小于指定值
     *
     * @param number  待检查数值
     * @param max     最大值（不包含）
     * @param message 错误消息
     * @throws BusinessException 如果数值不小于指定值
     */
    public static void isLessThan(long number, long max, String message) {
        if (number >= max) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言数值小于等于指定值
     *
     * @param number  待检查数值
     * @param max     最大值（包含）
     * @param message 错误消息
     * @throws BusinessException 如果数值大于指定值
     */
    public static void isLessThanOrEqual(long number, long max, String message) {
        if (number > max) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言数值在指定范围内（包含边界）
     *
     * @param number  待检查数值
     * @param min     最小值（包含）
     * @param max     最大值（包含）
     * @param message 错误消息
     * @throws BusinessException 如果数值不在范围内
     */
    public static void isBetween(long number, long min, long max, String message) {
        if (number < min || number > max) {
            throw BusinessException.paramError(message);
        }
    }

    // ========== 相等断言 ==========

    /**
     * 断言两个对象相等
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param message  错误消息
     * @throws BusinessException 如果两个对象不相等
     */
    public static void equals(Object expected, Object actual, String message) {
        if (expected == null) {
            if (actual != null) {
                throw BusinessException.paramError(message);
            }
        } else if (!expected.equals(actual)) {
            throw BusinessException.paramError(message);
        }
    }

    /**
     * 断言两个对象不相等
     *
     * @param unexpected 不期望的值
     * @param actual     实际值
     * @param message    错误消息
     * @throws BusinessException 如果两个对象相等
     */
    public static void notEquals(Object unexpected, Object actual, String message) {
        if (unexpected == null) {
            if (actual == null) {
                throw BusinessException.paramError(message);
            }
        } else if (unexpected.equals(actual)) {
            throw BusinessException.paramError(message);
        }
    }

    // ========== 私有方法 ==========

    private static String nullSafeGet(Supplier<String> messageSupplier) {
        return (messageSupplier != null ? messageSupplier.get() : null);
    }
}
