package com.basebackend.jwt;

/**
 * JWT 异常 — 携带结构化错误类型，便于上层按类型分别处理
 */
public class JwtException extends RuntimeException {

    private final ErrorType errorType;

    public JwtException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public JwtException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * JWT 错误类型枚举
     */
    public enum ErrorType {
        /** Token 已过期 */
        EXPIRED,
        /** 签名无效 */
        INVALID_SIGNATURE,
        /** Token 格式错误 */
        MALFORMED,
        /** Token 已被吊销 */
        REVOKED,
        /** 不支持的 Token 类型 */
        UNSUPPORTED,
        /** Token 类型不匹配（如用 refresh token 访问业务接口） */
        TOKEN_TYPE_MISMATCH
    }
}
