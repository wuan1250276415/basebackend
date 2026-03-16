package com.basebackend.common.web;

import com.basebackend.common.enums.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResponseResult 单元测试
 */
class ResponseResultTest {

    @Nested
    @DisplayName("失败响应")
    class ErrorResult {

        @Test
        @DisplayName("无参 error() 应返回标准服务端错误码")
        void shouldCreateDefaultErrorWithInternalServerCode() {
            ResponseResult<Void> result = ResponseResult.error();

            assertThat(result.getCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            assertThat(result.isFailed()).isTrue();
        }

        @Test
        @DisplayName("error(message) 应保留标准错误码并替换消息")
        void shouldCreateErrorWithCustomMessage() {
            ResponseResult<Void> result = ResponseResult.error("自定义错误");

            assertThat(result.getCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
            assertThat(result.getMessage()).isEqualTo("自定义错误");
            assertThat(result.isFailed()).isTrue();
        }

        @Test
        @DisplayName("error(ErrorCode) 应保持枚举语义")
        void shouldCreateErrorWithErrorCode() {
            ResponseResult<Void> result = ResponseResult.error(CommonErrorCode.DATA_NOT_FOUND);

            assertThat(result.getCode()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getCode());
            assertThat(result.getMessage()).isEqualTo(CommonErrorCode.DATA_NOT_FOUND.getMessage());
            assertThat(result.isFailed()).isTrue();
        }
    }
}
