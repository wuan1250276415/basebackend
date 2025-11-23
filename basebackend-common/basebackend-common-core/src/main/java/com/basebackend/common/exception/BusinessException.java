package com.basebackend.common.exception;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 统一的业务异常类，支持错误码枚举和自定义错误信息。
 * 所有业务逻辑中的异常都应该抛出此异常或其子类。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 方式1: 使用错误码枚举（推荐）
 * throw new BusinessException(CommonErrorCode.DATA_NOT_FOUND);
 *
 * // 方式2: 使用错误码枚举并自定义消息
 * throw new BusinessException(CommonErrorCode.DATA_NOT_FOUND, "用户ID=123的数据不存在");
 *
 * // 方式3: 使用自定义错误码和消息（向后兼容）
 * throw new BusinessException(400, "参数错误");
 *
 * // 方式4: 包装其他异常
 * throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, cause);
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see ErrorCode
 * @see CommonErrorCode
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 错误码枚举对象
     * <p>
     * 可能为 null（使用向后兼容的旧版构造函数时）。
     * 优先使用该对象以获取完整上下文（HTTP 状态、i18n 键、模块等）。
     * </p>
     */
    private final ErrorCode errorCode;

    /**
     * 获取错误码枚举对象
     * <p>
     * 通过此方法可以获取完整的错误码上下文，包括 HTTP 状态码映射、i18n 键等。
     * </p>
     *
     * @return 错误码对象；当通过向后兼容的旧构造函数创建时返回 null
     */
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    // ========== 构造函数：基于 ErrorCode 枚举（推荐）==========

    /**
     * 使用错误码枚举构造异常
     * <p>
     * 自动使用枚举中定义的错误码和消息。
     * </p>
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码枚举和自定义消息构造异常
     * <p>
     * 使用枚举的错误码，但可以自定义更详细的错误消息。
     * </p>
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码枚举和原始异常构造异常
     * <p>
     * 用于包装其他异常，保留异常链。
     * </p>
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码枚举、自定义消息和原始异常构造异常
     * <p>
     * 最完整的构造方式，同时支持自定义消息和异常链。
     * </p>
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.message = message;
        this.errorCode = errorCode;
    }

    // ========== 构造函数：使用原始参数（向后兼容）==========

    /**
     * 使用默认错误码（500）和自定义消息构造异常
     * <p>
     * <b>注意：</b>推荐使用 ErrorCode 枚举方式，此方法仅用于向后兼容。
     * </p>
     *
     * @param message 错误消息
     * @deprecated 请使用 {@link #BusinessException(ErrorCode)} 或 {@link #BusinessException(ErrorCode, String)}
     */
    @Deprecated
    public BusinessException(String message) {
        super(message);
        this.code = CommonErrorCode.INTERNAL_SERVER_ERROR.getCode();
        this.message = message;
        this.errorCode = null;
    }

    /**
     * 使用自定义错误码和消息构造异常
     * <p>
     * <b>注意：</b>推荐使用 ErrorCode 枚举方式，此方法仅用于向后兼容。
     * </p>
     *
     * @param code    错误码
     * @param message 错误消息
     * @deprecated 请使用 {@link #BusinessException(ErrorCode, String)}
     */
    @Deprecated
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.errorCode = null;
    }

    /**
     * 使用自定义错误码、消息和原始异常构造异常
     * <p>
     * <b>注意：</b>推荐使用 ErrorCode 枚举方式，此方法仅用于向后兼容。
     * </p>
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     * @deprecated 请使用 {@link #BusinessException(ErrorCode, String, Throwable)}
     */
    @Deprecated
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.errorCode = null;
    }

    // ========== 便捷静态工厂方法 ==========

    /**
     * 创建参数错误异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 创建数据不存在异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(CommonErrorCode.DATA_NOT_FOUND, message);
    }

    /**
     * 创建权限不足异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(CommonErrorCode.FORBIDDEN, message);
    }

    /**
     * 创建未授权异常
     *
     * @return 业务异常
     */
    public static BusinessException unauthorized() {
        return new BusinessException(CommonErrorCode.UNAUTHORIZED);
    }

    /**
     * 创建冲突异常（如资源状态或版本冲突）
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(CommonErrorCode.CONFLICT, message);
    }

    /**
     * 创建限流异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(CommonErrorCode.TOO_MANY_REQUESTS, message);
    }

    /**
     * 创建 Token 过期异常
     *
     * @return 业务异常
     */
    public static BusinessException tokenExpired() {
        return new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
    }

    /**
     * 创建 Token 无效异常
     *
     * @return 业务异常
     */
    public static BusinessException tokenInvalid() {
        return new BusinessException(CommonErrorCode.TOKEN_INVALID);
    }

    /**
     * 创建 Token 缺失异常
     *
     * @return 业务异常
     */
    public static BusinessException tokenMissing() {
        return new BusinessException(CommonErrorCode.TOKEN_MISSING);
    }

    /**
     * 创建租户不存在异常
     *
     * @return 业务异常
     */
    public static BusinessException tenantNotFound() {
        return new BusinessException(CommonErrorCode.TENANT_NOT_FOUND);
    }

    /**
     * 创建租户已禁用异常
     *
     * @return 业务异常
     */
    public static BusinessException tenantDisabled() {
        return new BusinessException(CommonErrorCode.TENANT_DISABLED);
    }

    /**
     * 创建租户已过期异常
     *
     * @return 业务异常
     */
    public static BusinessException tenantExpired() {
        return new BusinessException(CommonErrorCode.TENANT_EXPIRED);
    }

    /**
     * 创建文件不存在异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException fileNotFound(String message) {
        return new BusinessException(CommonErrorCode.FILE_NOT_FOUND, message);
    }

    /**
     * 创建文件过大异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException fileTooLarge(String message) {
        return new BusinessException(CommonErrorCode.FILE_TOO_LARGE, message);
    }

    /**
     * 创建存储服务异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException storageServiceError(String message) {
        return new BusinessException(CommonErrorCode.STORAGE_SERVICE_ERROR, message);
    }

    /**
     * 创建 Token 在黑名单内异常
     *
     * @return 业务异常
     */
    public static BusinessException tokenBlacklisted() {
        return new BusinessException(CommonErrorCode.TOKEN_BLACKLISTED);
    }

    /**
     * 创建刷新 Token 过期异常
     *
     * @return 业务异常
     */
    public static BusinessException refreshTokenExpired() {
        return new BusinessException(CommonErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    /**
     * 创建刷新 Token 无效异常
     *
     * @return 业务异常
     */
    public static BusinessException refreshTokenInvalid() {
        return new BusinessException(CommonErrorCode.REFRESH_TOKEN_INVALID);
    }

    /**
     * 创建刷新 Token 缺失异常
     *
     * @return 业务异常
     */
    public static BusinessException refreshTokenMissing() {
        return new BusinessException(CommonErrorCode.REFRESH_TOKEN_MISSING);
    }

    /**
     * 创建文件类型不支持异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException fileTypeNotSupported(String message) {
        return new BusinessException(CommonErrorCode.FILE_TYPE_NOT_SUPPORTED, message);
    }

    /**
     * 创建文件上传失败异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException fileUploadFailed(String message) {
        return new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, message);
    }

    /**
     * 创建文件下载失败异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException fileDownloadFailed(String message) {
        return new BusinessException(CommonErrorCode.FILE_DOWNLOAD_FAILED, message);
    }

    /**
     * 创建序列化失败异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException serializationError(String message) {
        return new BusinessException(CommonErrorCode.SERIALIZATION_ERROR, message);
    }

    /**
     * 创建反序列化失败异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException deserializationError(String message) {
        return new BusinessException(CommonErrorCode.DESERIALIZATION_ERROR, message);
    }

    /**
     * 创建 JSON 解析错误异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException jsonParseError(String message) {
        return new BusinessException(CommonErrorCode.JSON_PARSE_ERROR, message);
    }

    /**
     * 创建乐观锁冲突异常
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException optimisticLockConflict(String message) {
        return new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT, message);
    }
}
