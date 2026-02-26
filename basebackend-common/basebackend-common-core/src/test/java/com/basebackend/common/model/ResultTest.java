package com.basebackend.common.model;

import com.basebackend.common.enums.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result 统一响应封装单元测试
 */
class ResultTest {

    // ========== 成功响应 ==========

    @Nested
    @DisplayName("成功响应")
    class SuccessResult {

        @Test
        @DisplayName("无参 success()")
        void shouldCreateEmptySuccess() {
            Result<Void> result = Result.success();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailed()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.SUCCESS.getCode());
            assertThat(result.getMessage()).isEqualTo(CommonErrorCode.SUCCESS.getMessage());
            assertThat(result.getData()).isNull();
            assertThat(result.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("带数据 success(data)")
        void shouldCreateSuccessWithData() {
            Result<String> result = Result.success("hello");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo("hello");
        }

        @Test
        @DisplayName("自定义消息 success(message, data)")
        void shouldCreateSuccessWithMessage() {
            Result<Integer> result = Result.success("操作成功", 42);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("操作成功");
            assertThat(result.getData()).isEqualTo(42);
        }
    }

    // ========== 失败响应 ==========

    @Nested
    @DisplayName("失败响应")
    class ErrorResult {

        @Test
        @DisplayName("无参 error()")
        void shouldCreateDefaultError() {
            Result<Void> result = Result.error();
            assertThat(result.isFailed()).isTrue();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        }

        @Test
        @DisplayName("ErrorCode 枚举 error(errorCode)")
        void shouldCreateErrorWithErrorCode() {
            Result<Void> result = Result.error(CommonErrorCode.DATA_NOT_FOUND);
            assertThat(result.isFailed()).isTrue();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
            assertThat(result.getMessage()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("ErrorCode + 自定义消息")
        void shouldCreateErrorWithCustomMessage() {
            Result<Void> result = Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED, "用户名不能为空");
            assertThat(result.isFailed()).isTrue();
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode());
            assertThat(result.getMessage()).isEqualTo("用户名不能为空");
        }

        @Test
        @DisplayName("自定义消息 error(message)")
        void shouldCreateErrorWithMessage() {
            Result<Void> result = Result.error("系统异常");
            assertThat(result.isFailed()).isTrue();
            assertThat(result.getMessage()).isEqualTo("系统异常");
        }

        @Test
        @DisplayName("自定义状态码 error(code, message)")
        @SuppressWarnings("deprecation")
        void shouldCreateErrorWithCodeAndMessage() {
            Result<Void> result = Result.error(403, "禁止访问");
            assertThat(result.isFailed()).isTrue();
            assertThat(result.getCode()).isEqualTo(403);
            assertThat(result.getMessage()).isEqualTo("禁止访问");
        }
    }

    // ========== 便捷方法 ==========

    @Nested
    @DisplayName("便捷方法")
    class ConvenienceMethods {

        @Test
        @DisplayName("isSuccess / isFailed 互斥")
        void shouldBeExclusive() {
            Result<Void> success = Result.success();
            assertThat(success.isSuccess()).isTrue();
            assertThat(success.isFailed()).isFalse();

            Result<Void> error = Result.error();
            assertThat(error.isSuccess()).isFalse();
            assertThat(error.isFailed()).isTrue();
        }

        @Test
        @DisplayName("timestamp 自动设置")
        void shouldSetTimestamp() {
            long before = System.currentTimeMillis();
            Result<Void> result = Result.success();
            long after = System.currentTimeMillis();
            assertThat(result.getTimestamp()).isBetween(before, after);
        }
    }

    // ========== 泛型 ==========

    @Nested
    @DisplayName("泛型支持")
    class GenericSupport {

        @Test
        @DisplayName("支持不同数据类型")
        void shouldSupportVariousTypes() {
            Result<String> strResult = Result.success("text");
            Result<Integer> intResult = Result.success(123);
            Result<java.util.List<String>> listResult = Result.success(java.util.List.of("a", "b"));

            assertThat(strResult.getData()).isEqualTo("text");
            assertThat(intResult.getData()).isEqualTo(123);
            assertThat(listResult.getData()).hasSize(2);
        }
    }
}
