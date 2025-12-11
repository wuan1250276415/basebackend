package com.basebackend.security.exception;

/**
 * mTLS相关异常
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class MTLSException extends SecurityException {

    public MTLSException(String message) {
        super(message, ErrorCode.SSL_INIT_FAILED);
    }

    public MTLSException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public MTLSException(String message, Throwable cause) {
        super(message, ErrorCode.SSL_INIT_FAILED, cause);
    }

    public MTLSException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    // 静态工厂方法
    public static MTLSException sslInitFailed(String reason, Throwable cause) {
        return new MTLSException("SSL上下文初始化失败: " + reason, ErrorCode.SSL_INIT_FAILED, cause);
    }

    public static MTLSException certificateInvalid(String certPath) {
        return new MTLSException("证书无效: " + certPath, ErrorCode.CERTIFICATE_INVALID);
    }

    public static MTLSException certificateExpired(String certPath) {
        return new MTLSException("证书已过期: " + certPath, ErrorCode.CERTIFICATE_EXPIRED);
    }

    public static MTLSException certificateNotFound(String certPath) {
        return new MTLSException("证书未找到: " + certPath, ErrorCode.CERTIFICATE_NOT_FOUND);
    }

    public static MTLSException keystoreError(String keystorePath, Throwable cause) {
        return new MTLSException("密钥库加载失败: " + keystorePath, ErrorCode.KEYSTORE_ERROR, cause);
    }
}
