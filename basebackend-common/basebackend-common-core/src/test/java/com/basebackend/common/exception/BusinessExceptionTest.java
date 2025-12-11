package com.basebackend.common.exception;

import com.basebackend.common.enums.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 单元测试
 */
@DisplayName("BusinessException 测试")
class BusinessExceptionTest {

    @Nested
    @DisplayName("ErrorCode构造函数测试")
    class ErrorCodeConstructorTests {

        @Test
        @DisplayName("使用ErrorCode构造异常")
        void shouldConstructWithErrorCode() {
            BusinessException ex = new BusinessException(CommonErrorCode.DATA_NOT_FOUND);

            assertEquals(CommonErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            assertEquals(CommonErrorCode.DATA_NOT_FOUND.getMessage(), ex.getMessage());
            assertEquals(CommonErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("使用ErrorCode和自定义消息构造异常")
        void shouldConstructWithErrorCodeAndMessage() {
            String customMessage = "用户ID=123不存在";
            BusinessException ex = new BusinessException(CommonErrorCode.DATA_NOT_FOUND, customMessage);

            assertEquals(CommonErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            assertEquals(customMessage, ex.getMessage());
            assertEquals(CommonErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("使用ErrorCode和原始异常构造异常")
        void shouldConstructWithErrorCodeAndCause() {
            RuntimeException cause = new RuntimeException("Original error");
            BusinessException ex = new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, cause);

            assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("使用ErrorCode、消息和原始异常构造异常")
        void shouldConstructWithAllParams() {
            String customMessage = "Custom message";
            RuntimeException cause = new RuntimeException("Original error");
            BusinessException ex = new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, customMessage, cause);

            assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
            assertEquals(customMessage, ex.getMessage());
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("paramError创建参数错误异常")
        void shouldCreateParamError() {
            BusinessException ex = BusinessException.paramError("参数不能为空");

            assertEquals(CommonErrorCode.BAD_REQUEST.getCode(), ex.getCode());
            assertEquals("参数不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("notFound创建数据不存在异常")
        void shouldCreateNotFound() {
            BusinessException ex = BusinessException.notFound("用户不存在");

            assertEquals(CommonErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @DisplayName("forbidden创建权限不足异常")
        void shouldCreateForbidden() {
            BusinessException ex = BusinessException.forbidden("无权访问此资源");

            assertEquals(CommonErrorCode.FORBIDDEN.getCode(), ex.getCode());
            assertEquals("无权访问此资源", ex.getMessage());
        }

        @Test
        @DisplayName("unauthorized创建未授权异常")
        void shouldCreateUnauthorized() {
            BusinessException ex = BusinessException.unauthorized();

            assertEquals(CommonErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("conflict创建冲突异常")
        void shouldCreateConflict() {
            BusinessException ex = BusinessException.conflict("资源版本冲突");

            assertEquals(CommonErrorCode.CONFLICT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tooManyRequests创建限流异常")
        void shouldCreateTooManyRequests() {
            BusinessException ex = BusinessException.tooManyRequests("请求过于频繁");

            assertEquals(CommonErrorCode.TOO_MANY_REQUESTS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tokenExpired创建Token过期异常")
        void shouldCreateTokenExpired() {
            BusinessException ex = BusinessException.tokenExpired();

            assertEquals(CommonErrorCode.TOKEN_EXPIRED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tokenInvalid创建Token无效异常")
        void shouldCreateTokenInvalid() {
            BusinessException ex = BusinessException.tokenInvalid();

            assertEquals(CommonErrorCode.TOKEN_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tokenMissing创建Token缺失异常")
        void shouldCreateTokenMissing() {
            BusinessException ex = BusinessException.tokenMissing();

            assertEquals(CommonErrorCode.TOKEN_MISSING.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("向后兼容构造函数测试")
    @SuppressWarnings("deprecation")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("仅消息构造（已弃用）")
        void shouldConstructWithMessageOnly() {
            BusinessException ex = new BusinessException("Something went wrong");

            assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
            assertEquals("Something went wrong", ex.getMessage());
            assertNull(ex.getErrorCode());
        }

        @Test
        @DisplayName("错误码和消息构造（已弃用）")
        void shouldConstructWithCodeAndMessage() {
            BusinessException ex = new BusinessException(400, "Bad request");

            assertEquals(400, ex.getCode());
            assertEquals("Bad request", ex.getMessage());
            assertNull(ex.getErrorCode());
        }

        @Test
        @DisplayName("错误码、消息和原因构造（已弃用）")
        void shouldConstructWithCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("Original");
            BusinessException ex = new BusinessException(500, "Internal error", cause);

            assertEquals(500, ex.getCode());
            assertEquals("Internal error", ex.getMessage());
            assertEquals(cause, ex.getCause());
            assertNull(ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("文件相关异常测试")
    class FileExceptionTests {

        @Test
        @DisplayName("fileNotFound创建文件不存在异常")
        void shouldCreateFileNotFound() {
            BusinessException ex = BusinessException.fileNotFound("文件不存在");
            assertEquals(CommonErrorCode.FILE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("fileTooLarge创建文件过大异常")
        void shouldCreateFileTooLarge() {
            BusinessException ex = BusinessException.fileTooLarge("文件超过最大限制");
            assertEquals(CommonErrorCode.FILE_TOO_LARGE.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("fileTypeNotSupported创建文件类型不支持异常")
        void shouldCreateFileTypeNotSupported() {
            BusinessException ex = BusinessException.fileTypeNotSupported("不支持的文件类型");
            assertEquals(CommonErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("fileUploadFailed创建上传失败异常")
        void shouldCreateFileUploadFailed() {
            BusinessException ex = BusinessException.fileUploadFailed("上传失败");
            assertEquals(CommonErrorCode.FILE_UPLOAD_FAILED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("fileDownloadFailed创建下载失败异常")
        void shouldCreateFileDownloadFailed() {
            BusinessException ex = BusinessException.fileDownloadFailed("下载失败");
            assertEquals(CommonErrorCode.FILE_DOWNLOAD_FAILED.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("租户相关异常测试")
    class TenantExceptionTests {

        @Test
        @DisplayName("tenantNotFound创建租户不存在异常")
        void shouldCreateTenantNotFound() {
            BusinessException ex = BusinessException.tenantNotFound();
            assertEquals(CommonErrorCode.TENANT_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tenantDisabled创建租户已禁用异常")
        void shouldCreateTenantDisabled() {
            BusinessException ex = BusinessException.tenantDisabled();
            assertEquals(CommonErrorCode.TENANT_DISABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tenantExpired创建租户已过期异常")
        void shouldCreateTenantExpired() {
            BusinessException ex = BusinessException.tenantExpired();
            assertEquals(CommonErrorCode.TENANT_EXPIRED.getCode(), ex.getCode());
        }
    }
}
